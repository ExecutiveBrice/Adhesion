import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleInfo, faComments, faUsers } from '@fortawesome/free-solid-svg-icons';
import { catchError, finalize, of, switchMap } from 'rxjs';
import { TeamEvent, TeamEventDetails, TeamInvitation, TeamService, teamError } from '../../_services/team.service';
import { ToastService } from '../../_services/toast.service';
import { TokenStorageService } from '../../_services/token-storage.service';
import { TribuService } from '../../_services/tribu.service';
import { TeamEventsComponent } from '../../template/team-events/team-events.component';
import { EventChatComponent } from './event-chat.component';

export type EventTab = 'informations' | 'participants' | 'chat';

@Component({
  selector: 'app-evenement',
  imports: [DatePipe, FaIconComponent, NgbNavModule, TeamEventsComponent, EventChatComponent],
  templateUrl: './evenement.component.html',
  styleUrl: './evenement.component.css'
})
export class EvenementComponent implements OnInit {
  private readonly api = inject(TeamService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toasts = inject(ToastService);
  private readonly storage = inject(TokenStorageService);
  private readonly tribus = inject(TribuService);
  readonly adherentId = signal<number | null>(null);
  readonly detail = signal<TeamEventDetails | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly tab = signal<EventTab>('informations');
  readonly replying = signal(false);
  readonly replyError = signal('');
  readonly replyInvitations = computed(() => {
    const event = this.detail()?.evenement;
    const adherentId = this.adherentId();
    if (!event || event.etat === 'ANNULEE' || adherentId === null) return [];
    return [
      ...event.convocations.map(invitation => ({ invitation, responsibility: '' })),
      ...(event.responsabilites ?? []).flatMap(r => r.invitation
        ? [{ invitation: r.invitation, responsibility: r.tache.nom }] : [])
    ].filter(({ invitation }) => invitation.adherentId === adherentId && invitation.selectionnee && invitation.canReply);
  });
  readonly faCircleInfo = faCircleInfo;
  readonly faUsers = faUsers;
  readonly faComments = faComments;

  ngOnInit() {
    const userId = this.storage.getUser().id;
    this.tribus.getConnected().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: tribu => this.adherentId.set(userId == null ? null : tribu.adherents.find(a => a.user?.id === userId)?.id ?? null),
      error: () => this.replyError.set('Impossible de charger vos invitations. Réessayez après actualisation.')
    });
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const tab = params.get('onglet');
      this.tab.set(tab === 'participants' || (tab === 'chat' && this.detail()?.canChat !== false) ? tab : 'informations');
    });
    this.route.paramMap.pipe(switchMap(params => {
      this.detail.set(null); this.loading.set(true); this.error.set(''); this.replyError.set('');
      const id = Number(params.get('evenementId'));
      if (!Number.isSafeInteger(id) || id <= 0) {
        this.error.set('Événement introuvable.');
        return of(null);
      }
      return this.api.event(id).pipe(catchError(error => {
        this.error.set(error.status === 403 ? 'Vous n’avez pas accès à cet événement.'
          : error.status === 404 ? 'Événement introuvable.' : teamError(error));
        return of(null);
      }));
    }), takeUntilDestroyed(this.destroyRef)).subscribe(detail => {
      this.detail.set(detail); this.loading.set(false);
      if (detail && !detail.canChat && this.tab() === 'chat') this.selectTab('informations');
    });
  }

  selectTab(tab: EventTab) {
    if (tab === 'chat' && !this.detail()?.canChat) tab = 'informations';
    this.tab.set(tab);
    void this.router.navigate([], { relativeTo: this.route, queryParams: { onglet: tab === 'informations' ? null : tab },
      queryParamsHandling: 'merge', replaceUrl: true });
  }
  eventChanged(evenement: TeamEvent) {
    this.detail.update(detail => detail && detail.evenement.id === evenement.id ? { ...detail, evenement } : detail);
  }
  reply(invitation: TeamInvitation, value: boolean | null) {
    const event = this.detail()?.evenement;
    if (!event || this.replying() || !this.replyInvitations().some(row => row.invitation.id === invitation.id)) return;
    this.replying.set(true); this.replyError.set('');
    this.api.reply(event.id, invitation.id, value).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.replying.set(false))
    ).subscribe({
      next: updated => {
        if (this.detail()?.evenement.id !== event.id) return;
        this.eventChanged(updated);
        this.toasts.success('Réponse enregistrée.');
      },
      error: error => {
        if (this.detail()?.evenement.id === event.id) this.replyError.set(teamError(error));
      }
    });
  }
  typeLabel(type: string) {
    return ({ ACTIVITE: 'Séance', ENTRAINEMENT: 'Entraînement', RENCONTRE: 'Rencontre', TOURNOI: 'Tournoi', ANIMATION: 'Animation', AUTRE: 'Autre' } as Record<string, string>)[type] ?? type;
  }
  stateLabel(state: string) {
    return ({ PROGRAMMEE: 'Programmé', MODIFIEE: 'Modifié', REALISEE: 'Réalisé', ANNULEE: 'Annulé' } as Record<string, string>)[state] ?? state;
  }
  get mapsLink() {
    const detail = this.detail();
    const destination = detail?.adresse || detail?.evenement.lieu;
    return destination ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(destination)}` : null;
  }
  get activityLink() {
    const link = this.detail()?.lien?.trim();
    if (!link) return null;
    return /^https?:\/\//i.test(link) ? link : /^[\w.-]+\.[a-z]{2,}(?:[/:?#]|$)/i.test(link) ? `https://${link}` : null;
  }
}
