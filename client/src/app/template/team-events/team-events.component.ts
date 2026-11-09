import { Component, EventEmitter, inject, Input, OnChanges, OnDestroy, Output, SimpleChanges, TemplateRef, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { DatePipe, KeyValuePipe, NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowUpRightFromSquare, faArrowsRotate, faChevronDown, faChevronUp, faCircleCheck, faCircleExclamation, faCircleQuestion, faCircleXmark, faClock, faClockRotateLeft, faEnvelope, faHandHoldingMedical, faPen, faPlus, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import { forkJoin, Observable } from 'rxjs';
import { TeamService, Team, TeamEvent, TeamInvitation, ResponsibilityTeam, TeamResponsibilityEdit, teamError } from '../../_services/team.service';
import { ParamService } from '../../_services/param.service';
import { TeamEventModalComponent } from '../team-event-modal/team-event-modal.component';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { ToastService } from '../../_services/toast.service';
import { TacheSeanceConfiguration } from '../../models/responsabiliteSeance';
import { ResponsabilitesSeanceComponent } from '../responsabilites-seance/responsabilites-seance.component';
import { filterByName } from '../../_helpers/name-search';

@Component({ selector: 'app-team-events', imports: [FormsModule, DatePipe, KeyValuePipe, NgTemplateOutlet, FaIconComponent, ResponsabilitesSeanceComponent, RouterLink],
  templateUrl: './team-events.component.html', styleUrls: ['./team-events.component.css'] })
export class TeamEventsComponent implements OnChanges, OnDestroy {
  private refresh = registerApiViewRefresh();
  api = inject(TeamService);
  private toasts = inject(ToastService);
  private params = inject(ParamService);
  private modals = inject(NgbModal);
  private responsibilityModal?: NgbModalRef;
  @ViewChild('responsibilitiesModal', { static: true }) responsibilitiesModal!: TemplateRef<unknown>;
  @Input() team!: Team;
  @Input() detailEvent?: TeamEvent;
  @Output() eventChanged = new EventEmitter<TeamEvent>();
  get contextId() { return this.detailEvent?.id ?? this.team.id; }
  @Input() teams: Team[] = [];
  @Input() collapsibleParticipants = false;
  @Input() collapsibleEvents = false;
  @Input() summaryCards = false;
  @Input() allowEventEditing = true;
  eventsExpanded = false;
  events: TeamEvent[] = [];
  error = ''; busy = false; loading = false;
  private eventModal?: NgbModalRef;
  showPast = false;
  faCircleCheck = faCircleCheck;
  faCircleXmark = faCircleXmark;
  faCircleQuestion = faCircleQuestion;
  faClock = faClock;
  faCircleExclamation = faCircleExclamation;
  faTriangleExclamation = faTriangleExclamation;
  faChevronDown = faChevronDown;
  faChevronUp = faChevronUp;
  faEnvelope = faEnvelope;
  faPen = faPen;
  faPlus = faPlus;
  faArrowsRotate = faArrowsRotate;
  faClockRotateLeft = faClockRotateLeft;
  faHandHoldingMedical = faHandHoldingMedical;
  faArrowUpRightFromSquare = faArrowUpRightFromSquare;
  expandedParticipants = new Set<number>();
  participantSearch: Record<number, string> = {};
  responsibilityEvent?: TeamEvent;
  responsibilityRows: TeamResponsibilityEdit[] = [];
  responsibilityTeams: ResponsibilityTeam[] = [];
  responsibilitiesLoading = false;
  tasks: TacheSeanceConfiguration[] = [];
  private reminders = new Map<string, string>();
  ngOnChanges(changes: SimpleChanges) {
    if (this.detailEvent) {
      if (changes['detailEvent']?.previousValue?.id !== this.detailEvent.id) {
        this.closeResponsibilities(); this.error = ''; this.participantSearch = {};
      }
      this.events = [this.detailEvent]; this.showPast = true;
      return;
    }
    const team = changes['team'];
    // Changes to the available teams must not reload HTTP data or erase a draft.
    if (!team || team.currentValue?.id === team.previousValue?.id) {
      if (team && !this.team.canManage) { this.closeEventModal(); this.closeResponsibilities(); }
      if (team) {
        const previous = team.previousValue.effectif as number[];
        const current = this.team.effectif;
        if (previous.length !== current.length || current.some(id => !previous.includes(id))) this.load();
      }
      return;
    }
    this.closeEventModal(); this.closeResponsibilities(); this.expandedParticipants.clear(); this.participantSearch = {}; this.events = []; this.error = ''; this.load();
  }
  ngOnDestroy() { this.closeEventModal(); this.closeResponsibilities(); }
  load() {
    if (this.detailEvent) {
      const id = this.detailEvent.id; this.loading = true;
      this.api.event(id).subscribe({ next: detail => {
        if (this.detailEvent?.id === id) { this.events = [detail.evenement]; this.loading = false; this.eventChanged.emit(detail.evenement); }
      }, error: e => { if (this.detailEvent?.id === id) { this.error = teamError(e); this.loading = false; } } });
      return;
    }
    const id = this.team.id; this.loading = true;
    this.api.events(id).subscribe({ next: list => { if (this.team.id === id) { this.events = list; this.loading = false; } },
      error: e => { if (this.team.id === id) { this.error = teamError(e); this.loading = false; } } });
  }
  get visibleEvents() { return this.events.filter(e => this.showPast || new Date(e.fin).getTime() >= Date.now()); }
  get upcomingEventsCount() { return this.events.filter(e => this.upcoming(e)).length; }
  upcoming(e: TeamEvent) { return e.etat !== 'ANNULEE' && new Date(e.debut).getTime() > Date.now(); }
  open(e?: TeamEvent) {
    if (!this.allowEventEditing || !this.team.canManage || this.team.archivee) return;
    this.closeEventModal();
    const modal = this.modals.open(TeamEventModalComponent, { size: 'lg', backdrop: 'static', ariaLabelledBy: 'event-modal-title' });
    this.eventModal = modal;
    modal.componentInstance.team = this.team;
    modal.componentInstance.teams = this.teams;
    modal.componentInstance.event = e;
    modal.result.then((result: any) => {
      if (this.eventModal !== modal) return;
      this.eventModal = undefined;
      this.toasts.success(result.etat === 'ANNULEE' ? 'Événement annulé.' : result.nombre ? `${result.nombre} événement(s) enregistré(s).` : 'Événement enregistré.');
      this.load();
    }, () => { if (this.eventModal === modal) this.eventModal = undefined; });
  }
  private closeEventModal() { this.eventModal?.dismiss(); this.eventModal = undefined; }
  publish(e: TeamEvent) { this.run(this.api.publish(e.id), 'Convocations publiées. Les emails sont placés dans la file d’envoi.'); }
  remind(e: TeamEvent) {
    const scope = `${e.id}:all`;
    const key = this.reminders.get(scope) ?? crypto.randomUUID(); this.reminders.set(scope, key);
    this.run(this.api.remind(e.id, key), 'Relance enregistrée pour les personnes sans réponse.', () => this.reminders.delete(scope));
  }
  publishInvitation(e: TeamEvent, id: number) {
    this.run(this.api.publishInvitation(e.id, id), 'Convocation individuelle publiée. L’email est placé dans la file d’envoi.');
  }
  remindInvitation(e: TeamEvent, id: number) {
    const scope = `${e.id}:${id}`;
    const key = this.reminders.get(scope) ?? crypto.randomUUID(); this.reminders.set(scope, key);
    this.run(this.api.remindInvitation(e.id, id, key), 'Relance individuelle enregistrée.', () => this.reminders.delete(scope));
  }
  reply(e: TeamEvent, id: number, value: boolean | null) { this.run(this.api.reply(e.id, id, value), 'Réponse enregistrée.'); }
  point(e: TeamEvent, id: number, value: boolean) { this.run(this.api.attendance(e.id, id, value), 'Présence enregistrée.'); }
  toggleParticipants(eventId: number) {
    if (this.expandedParticipants.has(eventId)) this.expandedParticipants.delete(eventId);
    else this.expandedParticipants.add(eventId);
  }
  editResponsibilities(e: TeamEvent) {
    if (this.busy) return;
    this.closeResponsibilities();
    this.error = ''; this.responsibilityEvent = e; this.responsibilitiesLoading = true;
    this.responsibilityTeams = []; this.tasks = [];
    this.responsibilityRows = e.responsabilites?.length
      ? e.responsabilites.map(r => ({ tacheId: r.tache.id, equipeId: r.equipeId ?? undefined })) : [{}];
    const modal = this.modals.open(this.responsibilitiesModal, { size: 'lg', centered: true,
      ariaLabelledBy: 'responsibilities-title', beforeDismiss: () => !this.busy });
    this.responsibilityModal = modal;
    modal.result.then(() => this.clearResponsibilityModal(modal), () => this.clearResponsibilityModal(modal));
    forkJoin({ teams: this.api.responsibilityTeams(e.id), tasks: this.params.getTachesSeance() }).subscribe({
      next: ({ teams, tasks }) => {
        if (this.responsibilityModal !== modal) return;
        this.responsibilityTeams = teams; this.tasks = tasks; this.responsibilitiesLoading = false;
      }, error: error => {
        if (this.responsibilityModal === modal) { this.error = teamError(error); this.responsibilitiesLoading = false; }
      }
    });
  }
  private clearResponsibilityModal(modal: NgbModalRef) {
    if (this.responsibilityModal === modal) { this.responsibilityModal = undefined; this.responsibilityEvent = undefined; }
  }
  closeResponsibilities() {
    this.responsibilityModal?.close(); this.responsibilityModal = undefined; this.responsibilityEvent = undefined;
  }
  responsibilityAssignee(row: TeamResponsibilityEdit) {
    return this.responsibilityTeams.find(t => t.id === row.equipeId)?.attributaire;
  }
  get responsibilityValidationError() {
    if (this.responsibilityRows.some(r => !r.tacheId || !r.equipeId)) return 'Choisissez une responsabilité et une équipe pour chaque ligne.';
    const keys = this.responsibilityRows.map(r => `${r.tacheId}:${r.equipeId}`);
    if (new Set(keys).size !== keys.length) return 'Cette responsabilité est déjà attribuée à cette équipe.';
    if (this.responsibilityRows.some(r => !this.responsibilityAssignee(r))) return 'Une équipe sélectionnée n’a aucun référent ni adhérent. Choisissez une autre équipe.';
    return '';
  }
  saveResponsibilities() {
    if (!this.responsibilityEvent || this.responsibilitiesLoading || this.busy) return;
    if (this.responsibilityValidationError) { this.error = this.responsibilityValidationError; return; }
    this.run(this.api.responsibilities(this.responsibilityEvent.id, this.responsibilityRows), 'Responsabilités enregistrées.', () => this.closeResponsibilities());
  }
  selectedInvites(e: TeamEvent) { return e.convocations.filter(c => c.selectionnee); }
  responsibilityInvites(e: TeamEvent): TeamInvitation[] {
    return (e.responsabilites ?? []).flatMap(r => r.invitation ? [r.invitation] : []);
  }
  responsibilityResponseCount(e: TeamEvent, response: boolean | null) {
    return this.responsibilityInvites(e).filter(c => c.reponse === response).length;
  }
  invitationLabel(invitation: TeamInvitation): string {
    return invitation.reponse === true ? 'Présent' : invitation.reponse === false ? 'Absent'
      : invitation.dateReponse ? 'Ne sais pas encore' : 'Sans réponse';
  }
  visibleInvites(e: TeamEvent) {
    return filterByName(this.selectedInvites(e), this.participantSearch[e.id] ?? '', person => person.nom);
  }
  responseCount(e: TeamEvent, value: boolean | null) { return e.convocations.filter(c => c.selectionnee && c.reponse === value).length; }
  mailLabel(state: string) { return ({ PENDING: 'en attente', SENDING: 'envoi engagé', SENT: 'accepté par le prestataire', UNKNOWN: 'résultat incertain', REJECTED: 'refusé', CANCELLED: 'devenu inutile', NO_ADDRESS: 'adresse absente' } as Record<string, string>)[state] ?? state; }
  run<T>(request: Observable<T>, message: string, after?: () => void) {
    if (this.busy) return; this.busy = true; this.error = '';
    const id = this.contextId;
    request.subscribe({ next: () => { this.busy = false; if (id === this.contextId) { this.toasts.success(message); after?.(); this.load(); } },
      error: e => { this.busy = false; this.error = teamError(e); } });
  }
}
