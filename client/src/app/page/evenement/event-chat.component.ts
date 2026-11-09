import { afterEveryRender, Component, ElementRef, Input, OnChanges, OnDestroy, ViewChild, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, EMPTY, exhaustMap, filter, Subscription, timer } from 'rxjs';
import { EventChatMessage, TeamService } from '../../_services/team.service';
import { TokenStorageService } from '../../_services/token-storage.service';

@Component({
  selector: 'app-event-chat', imports: [DatePipe, FormsModule],
  templateUrl: './event-chat.component.html', styleUrl: './event-chat.component.css'
})
export class EventChatComponent implements OnChanges, OnDestroy {
  private readonly api = inject(TeamService);
  readonly userId = inject(TokenStorageService).getUser().id;
  @Input({ required: true }) eventId!: number;
  @Input() active = true;
  @ViewChild('messageList') private messageList?: ElementRef<HTMLOListElement>;
  readonly messages = signal<EventChatMessage[]>([]);
  readonly draft = signal('');
  readonly loading = signal(true);
  readonly loadingOlder = signal(false);
  readonly sending = signal(false);
  readonly hasOlder = signal(false);
  readonly error = signal('');
  readonly sendError = signal('');
  readonly denied = signal(false);
  private currentId?: number;
  private cursor?: number;
  private initialized = false;
  private polling?: Subscription;
  private requests = new Subscription();
  private scroll?: 'bottom' | { top: number; height: number };

  constructor() {
    afterEveryRender(() => {
      const list = this.messageList?.nativeElement;
      if (!list || !this.active || !this.scroll) return;
      list.scrollTop = this.scroll === 'bottom' ? list.scrollHeight
        : this.scroll.top + list.scrollHeight - this.scroll.height;
      this.scroll = undefined;
    });
  }

  ngOnChanges() {
    if (this.currentId !== this.eventId) {
      this.polling?.unsubscribe(); this.requests.unsubscribe(); this.requests = new Subscription();
      this.currentId = this.eventId; this.cursor = undefined; this.initialized = false;
      this.scroll = undefined;
      this.messages.set([]); this.draft.set(''); this.loading.set(true); this.loadingOlder.set(false);
      this.sending.set(false); this.hasOlder.set(false); this.error.set(''); this.sendError.set(''); this.denied.set(false);
    }
    this.polling?.unsubscribe();
    if (!this.active) return;
    this.polling = timer(0, 5000).pipe(
      filter(() => document.visibilityState !== 'hidden' && !this.denied()),
      exhaustMap(() => this.api.eventMessages(this.eventId, this.cursor ? { afterId: this.cursor } : undefined).pipe(
        catchError(error => {
          this.loading.set(false);
          this.denied.set(error.status === 403 || error.status === 404);
          this.error.set(this.denied() ? 'Vous n’avez plus accès au chat de cet événement.'
            : 'Actualisation impossible. Une nouvelle tentative aura lieu automatiquement.');
          return EMPTY;
        })
      ))
    ).subscribe(messages => {
      const list = this.messageList?.nativeElement;
      if (!this.initialized || (messages.length && list && list.scrollHeight - list.scrollTop - list.clientHeight < 80)) this.scroll = 'bottom';
      if (!this.initialized) this.hasOlder.set(messages.length === 50);
      this.initialized = true; this.merge(messages); this.loading.set(false); this.error.set('');
      // Only advance through fetched messages: a send may overtake unseen messages.
      if (messages.length) this.cursor = messages[messages.length - 1].id;
    });
  }
  older() {
    const first = this.messages()[0];
    if (!first || this.loadingOlder() || this.denied()) return;
    this.loadingOlder.set(true);
    this.requests.add(this.api.eventMessages(this.eventId, { beforeId: first.id }).subscribe({
      next: messages => {
        const list = this.messageList?.nativeElement;
        if (list) this.scroll = { top: list.scrollTop, height: list.scrollHeight };
        this.merge(messages); this.hasOlder.set(messages.length === 50); this.loadingOlder.set(false); this.error.set('');
      },
      error: () => { this.loadingOlder.set(false); this.error.set('Impossible de charger les messages précédents. Réessayez.'); }
    }));
  }
  send() {
    const draft = this.draft(), content = draft.trim();
    if (!content || draft.length > 2000 || this.sending() || this.loading() || this.denied()) return;
    this.sending.set(true); this.sendError.set('');
    this.requests.add(this.api.sendEventMessage(this.eventId, content).subscribe({
      next: message => {
        this.scroll = 'bottom';
        this.merge([message]);
        if (this.draft() === draft) this.draft.set('');
        this.sending.set(false);
      },
      error: error => {
        this.sending.set(false);
        if (error.status === 403 || error.status === 404) this.denied.set(true);
        this.sendError.set(this.denied() ? 'Vous n’êtes plus autorisé à écrire dans ce chat.'
          : 'Le message n’a pas pu être envoyé. Votre texte est conservé, vous pouvez réessayer.');
      }
    }));
  }
  private merge(messages: EventChatMessage[]) {
    const merged = new Map(this.messages().map(message => [message.id, message]));
    messages.forEach(message => merged.set(message.id, message));
    this.messages.set([...merged.values()].sort((a, b) => a.id - b.id));
  }
  ngOnDestroy() { this.polling?.unsubscribe(); this.requests.unsubscribe(); }
}
