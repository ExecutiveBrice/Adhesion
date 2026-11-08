import { Component, inject, Input, OnChanges, OnDestroy, SimpleChanges, TemplateRef, ViewChild } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { DatePipe, KeyValuePipe, NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleCheck, faCircleExclamation, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import { forkJoin, Observable } from 'rxjs';
import { TeamService, Team, TeamEvent, TeamPerson, ResponsibilityTeam, TeamResponsibilityEdit, teamError } from '../../_services/team.service';
import { ParamService } from '../../_services/param.service';
import { TeamEventModalComponent } from '../team-event-modal/team-event-modal.component';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { ToastService } from '../../_services/toast.service';
import { TacheSeanceConfiguration } from '../../models/responsabiliteSeance';
import { ResponsabilitesSeanceComponent } from '../responsabilites-seance/responsabilites-seance.component';

@Component({ selector: 'app-team-events', imports: [FormsModule, DatePipe, KeyValuePipe, NgTemplateOutlet, FaIconComponent, ResponsabilitesSeanceComponent],
  templateUrl: './team-events.component.html', styleUrls: ['./team-events.component.css'] })
export class TeamEventsComponent implements OnChanges, OnDestroy {
  private refresh = registerApiViewRefresh();
  api = inject(TeamService);
  private toasts = inject(ToastService);
  private params = inject(ParamService);
  private modals = inject(NgbModal);
  private responsibilityModal?: NgbModalRef;
  @ViewChild('responsibilitiesModal', { static: true }) responsibilitiesModal!: TemplateRef<unknown>;
  @Input({ required: true }) team!: Team;
  @Input() teams: Team[] = [];
  @Input() collapsibleParticipants = false;
  events: TeamEvent[] = [];
  error = ''; busy = false; loading = false;
  private eventModal?: NgbModalRef;
  selectionEvent?: TeamEvent;
  candidates: TeamPerson[] = [];
  selected: number[] = [];
  search = '';
  showPast = false;
  faCircleCheck = faCircleCheck;
  faCircleExclamation = faCircleExclamation;
  faTriangleExclamation = faTriangleExclamation;
  expandedParticipants = new Set<number>();
  expandedResponsibilities = new Set<number>();
  responsibilityEvent?: TeamEvent;
  responsibilityRows: TeamResponsibilityEdit[] = [];
  responsibilityTeams: ResponsibilityTeam[] = [];
  responsibilitiesLoading = false;
  tasks: TacheSeanceConfiguration[] = [];
  private reminders = new Map<number, string>();
  ngOnChanges(changes: SimpleChanges) {
    const team = changes['team'];
    // Changes to the available teams must not reload HTTP data or erase a draft.
    if (!team || team.currentValue?.id === team.previousValue?.id) {
      if (team && !this.team.canManage) { this.closeEventModal(); this.selectionEvent = undefined; this.closeResponsibilities(); }
      return;
    }
    this.closeEventModal(); this.selectionEvent = undefined; this.closeResponsibilities(); this.expandedParticipants.clear(); this.expandedResponsibilities.clear(); this.events = []; this.error = ''; this.load();
  }
  ngOnDestroy() { this.closeEventModal(); this.closeResponsibilities(); }
  load() {
    const id = this.team.id; this.loading = true;
    this.api.events(id).subscribe({ next: list => { if (this.team.id === id) { this.events = list; this.loading = false; } },
      error: e => { if (this.team.id === id) { this.error = teamError(e); this.loading = false; } } });
  }
  get visibleEvents() { return this.events.filter(e => this.showPast || new Date(e.fin).getTime() >= Date.now()); }
  upcoming(e: TeamEvent) { return e.etat !== 'ANNULEE' && new Date(e.debut).getTime() > Date.now(); }
  get filteredCandidates() { const q = this.search.toLocaleLowerCase('fr'); return this.candidates.filter(p => `${p.prenom} ${p.nom}`.toLocaleLowerCase('fr').includes(q)); }
  open(e?: TeamEvent) {
    if (!this.team.canManage || this.team.archivee) return;
    this.closeEventModal();
    const modal = this.modals.open(TeamEventModalComponent, { size: 'lg', backdrop: 'static', ariaLabelledBy: 'event-modal-title' });
    this.eventModal = modal;
    modal.componentInstance.team = this.team;
    modal.componentInstance.teams = this.teams;
    modal.componentInstance.event = e;
    modal.result.then((result: any) => {
      if (this.eventModal !== modal) return;
      this.eventModal = undefined;
      this.toasts.success(result.nombre ? `${result.nombre} événement(s) enregistré(s).` : 'Événement enregistré.');
      this.load();
    }, () => { if (this.eventModal === modal) this.eventModal = undefined; });
  }
  private closeEventModal() { this.eventModal?.dismiss(); this.eventModal = undefined; }
  editSelection(e: TeamEvent) {
    this.selectionEvent = e; this.selected = e.convocations.filter(c => c.selectionnee).map(c => c.adhesionId); this.search = '';
    this.api.candidates(e.id).subscribe({ next: p => this.candidates = p, error: err => this.error = teamError(err) });
  }
  togglePerson(id: number, checked: boolean) { this.selected = this.selected.filter(i => i !== id); if (checked) this.selected.push(id); }
  saveSelection() { if (this.selectionEvent) this.run(this.api.select(this.selectionEvent.id, this.selected), 'Sélection enregistrée. Publiez les nouvelles convocations pour prévenir les participants.', () => this.selectionEvent = undefined); }
  publish(e: TeamEvent) { this.run(this.api.publish(e.id), 'Convocations publiées. Les emails sont placés dans la file d’envoi.'); }
  remind(e: TeamEvent) {
    const key = this.reminders.get(e.id) ?? crypto.randomUUID(); this.reminders.set(e.id, key);
    this.run(this.api.remind(e.id, key), 'Relance enregistrée pour les personnes sans réponse.', () => this.reminders.delete(e.id));
  }
  cancel(e: TeamEvent) { if (confirm('Annuler cet événement et prévenir les personnes convoquées ?')) this.run(this.api.cancel(e.id), 'Événement annulé.'); }
  reply(e: TeamEvent, id: number, value: boolean) { this.run(this.api.reply(e.id, id, value), 'Réponse enregistrée.'); }
  point(e: TeamEvent, id: number, value: boolean) { this.run(this.api.attendance(e.id, id, value), 'Présence enregistrée.'); }
  toggleParticipants(eventId: number) {
    if (this.expandedParticipants.has(eventId)) this.expandedParticipants.delete(eventId);
    else this.expandedParticipants.add(eventId);
  }
  toggleResponsibilities(eventId: number) {
    if (this.expandedResponsibilities.has(eventId)) this.expandedResponsibilities.delete(eventId);
    else this.expandedResponsibilities.add(eventId);
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
  presenceCount(e: TeamEvent, value: boolean | null) { return e.convocations.filter(c => c.selectionnee && c.presence === value).length; }
  mailLabel(state: string) { return ({ PENDING: 'en attente', SENDING: 'envoi engagé', SENT: 'accepté par le prestataire', UNKNOWN: 'résultat incertain', REJECTED: 'refusé', CANCELLED: 'devenu inutile', NO_ADDRESS: 'adresse absente' } as Record<string, string>)[state] ?? state; }
  run<T>(request: Observable<T>, message: string, after?: () => void) {
    if (this.busy) return; this.busy = true; this.error = '';
    const id = this.team.id;
    request.subscribe({ next: () => { this.busy = false; if (id === this.team.id) { this.toasts.success(message); after?.(); this.load(); } },
      error: e => { this.busy = false; this.error = teamError(e); } });
  }
}
