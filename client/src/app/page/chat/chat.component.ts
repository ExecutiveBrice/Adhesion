import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, EMPTY, exhaustMap, filter, finalize, Subscription, timer } from 'rxjs';
import { ChatMessage, ChatRoom, ChatService } from '../../_services/chat.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ManagedSection, SectionManagementService } from '../../_services/section-management.service';
import { ERole } from '../../models';
import { ChatAdministrationComponent } from './chat-administration.component';

interface RoomState {
  initialized: boolean;
  messages: ChatMessage[];
  cursor?: number;
  draft: string;
  loading: boolean;
  loadingOlder: boolean;
  sending: boolean;
  hasOlder: boolean;
  error: string;
}

const emptyState = (): RoomState => ({
  initialized: false, messages: [], draft: '', loading: false,
  loadingOlder: false, sending: false, hasOlder: false, error: ''
});

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, ChatAdministrationComponent],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent implements OnInit, OnDestroy {
  private readonly chat = inject(ChatService);
  private readonly sectionManagement = inject(SectionManagementService);
  readonly unreadCounts = this.chat.unreadCounts;
  private readonly readThrough = new Map<number, number>();
  private readonly reading = new Set<number>();
  private readonly user = inject(TokenStorageService).getUser();
  readonly userId = this.user.id;
  readonly isAdmin = Array.isArray(this.user.roles) && this.user.roles.includes(ERole.ROLE_ADMIN);
  readonly managedSections = signal<ManagedSection[]>([]);
  readonly canAdminister = computed(() => this.isAdmin || this.managedSections().length > 0);
  readonly administrationOpen = signal(false);
  readonly rooms = signal<ChatRoom[]>([]);
  readonly filter = signal('');
  readonly filteredRooms = computed(() => {
    const filter = this.normalized(this.filter());
    const rooms = !filter ? this.rooms() : this.rooms().filter(room => [room.nom, room.groupeFiltre, ...room.categories.flatMap(category => [category.descriptif, category.jour, category.horaire])]
      .some(value => this.normalized(value).includes(filter)));
    const unreadCounts = this.unreadCounts();
    return [...rooms].sort((a, b) => Number((unreadCounts.get(b.id) ?? 0) > 0) - Number((unreadCounts.get(a.id) ?? 0) > 0)
      || a.nom.localeCompare(b.nom, 'fr'));
  });
  readonly activeRoomId = signal<number | undefined>(undefined);
  readonly activeRoom = computed(() => {
    const roomId = this.activeRoomId();
    return roomId === undefined ? undefined : this.rooms().find(room => room.id === roomId);
  });
  readonly states = signal(new Map<number, RoomState>());
  readonly loadingRooms = signal(true);
  readonly roomError = signal('');
  private readonly requests = new Subscription();
  private readonly roomRequests = new Map<number, Subscription>();

  ngOnInit(): void {
    this.loadRooms();
    if (!this.isAdmin) {
      this.requests.add(this.sectionManagement.sections().subscribe({
        next: sections => this.managedSections.set(sections),
        error: () => this.managedSections.set([])
      }));
    }
  }

  loadRooms(): void {
    this.loadingRooms.set(true);
    this.roomError.set('');
    this.requests.add(this.chat.rooms().subscribe({
      next: rooms => {
        this.rooms.set([...rooms].sort((a, b) => a.nom.localeCompare(b.nom, 'fr')));
        this.loadingRooms.set(false);
      },
      error: () => {
        this.loadingRooms.set(false);
        this.roomError.set('Impossible de charger les canaux. Réessayez.');
      }
    }));
  }

  state(roomId: number): RoomState { return this.states().get(roomId) ?? emptyState(); }

  private normalized(value?: string): string {
    return (value ?? '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('fr');
  }

  open(room: ChatRoom): void {
    const previous = this.activeRoom();
    if (previous && previous.id !== room.id) {
      this.roomRequests.get(previous.id)?.unsubscribe();
      this.roomRequests.delete(previous.id);
    }
    const state = this.state(room.id);
    this.activeRoomId.set(room.id);
    this.update(room.id, { loading: !state.initialized, error: '' });
    this.startPolling(room);
  }

  close(): void {
    const room = this.activeRoom();
    if (room) {
      this.roomRequests.get(room.id)?.unsubscribe();
      this.roomRequests.delete(room.id);
    }
    this.activeRoomId.set(undefined);
  }

  openAdministration(): void {
    if (!this.canAdminister()) return;
    this.close();
    this.administrationOpen.set(true);
  }

  closeAdministration(): void {
    this.administrationOpen.set(false);
    queueMicrotask(() => document.getElementById('chat-administration-button')?.focus());
  }

  updateDraft(roomId: number, draft: string): void { this.update(roomId, { draft }); }

  older(room: ChatRoom): void {
    const state = this.state(room.id);
    const oldest = state.messages[0];
    if (!oldest || state.loadingOlder) return;
    this.update(room.id, { loadingOlder: true });
    this.add(room.id, this.chat.messages(room.id, { beforeId: oldest.id }).subscribe({
      next: messages => {
        this.merge(room.id, messages);
        this.update(room.id, { hasOlder: messages.length === 50, loadingOlder: false, error: '' });
      },
      error: () => this.update(room.id, { loadingOlder: false, error: 'Impossible de charger les messages précédents. Réessayez.' })
    }));
  }

  send(room: ChatRoom): void {
    const state = this.state(room.id);
    const content = state.draft.trim();
    if (room.canWrite === false || !content || state.draft.length > 2000 || state.sending || state.loading) return;
    this.update(room.id, { sending: true, error: '' });
    this.add(room.id, this.chat.send(room.id, content).subscribe({
      next: message => {
        this.merge(room.id, [message]);
        this.update(room.id, { draft: '', sending: false });
      },
      error: err => this.update(room.id, {
        sending: false,
        error: err.status === 403 ? 'Vous n’êtes plus autorisé à publier dans ce chat.'
          : 'Le message n’a pas pu être envoyé. Votre texte est conservé, vous pouvez réessayer.'
      })
    }));
  }

  private startPolling(room: ChatRoom): void {
    this.roomRequests.get(room.id)?.unsubscribe();
    const subscription = timer(0, 5000).pipe(
      filter(() => document.visibilityState !== 'hidden'),
      exhaustMap(() => {
        const state = this.state(room.id);
        return this.chat.messages(room.id, state.cursor ? { afterId: state.cursor } : undefined).pipe(
          catchError(err => {
            this.update(room.id, {
              loading: false,
              error: err.status === 403 ? 'Vous n’êtes plus autorisé à accéder à ce canal.'
                : 'Actualisation impossible. Une nouvelle tentative aura lieu automatiquement.'
            });
            return EMPTY;
          })
        );
      })
    ).subscribe(messages => {
      const state = this.state(room.id);
      const initial = !state.initialized;
      this.merge(room.id, messages);
      this.update(room.id, {
        initialized: true, loading: false, hasOlder: initial ? messages.length === 50 : state.hasOlder, error: '',
        // Advance only through fetched messages, never through a newly sent message.
        cursor: messages.length ? messages[messages.length - 1].id : state.cursor
      });
      const lastMessageId = this.state(room.id).cursor;
      if (lastMessageId && document.visibilityState !== 'hidden' && !this.reading.has(room.id)
          && this.readThrough.get(room.id) !== lastMessageId) {
        this.reading.add(room.id);
        this.add(room.id, this.chat.markRead(room.id, lastMessageId).pipe(
          finalize(() => this.reading.delete(room.id))
        ).subscribe({
          next: () => this.readThrough.set(room.id, lastMessageId),
          error: () => this.update(room.id, { error: 'Impossible de mettre à jour les messages lus. Une nouvelle tentative aura lieu automatiquement.' })
        }));
      }
    });
    this.roomRequests.set(room.id, subscription);
  }

  private merge(roomId: number, messages: ChatMessage[]): void {
    if (!messages.length) return;
    const merged = new Map(this.state(roomId).messages.map(message => [message.id, message]));
    messages.forEach(message => merged.set(message.id, message));
    const allMessages = [...merged.values()].sort((a, b) => a.id - b.id);
    this.update(roomId, { messages: allMessages });
  }

  private update(roomId: number, changes: Partial<RoomState>): void {
    const states = new Map(this.states());
    states.set(roomId, { ...this.state(roomId), ...changes });
    this.states.set(states);
  }

  private add(roomId: number, subscription: Subscription): void {
    const current = this.roomRequests.get(roomId);
    if (current) current.add(subscription);
    else this.requests.add(subscription);
  }

  ngOnDestroy(): void {
    this.roomRequests.forEach(subscription => subscription.unsubscribe());
    this.requests.unsubscribe();
  }
}
