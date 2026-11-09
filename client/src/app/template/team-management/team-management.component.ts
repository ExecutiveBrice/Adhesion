import { Component, EventEmitter, inject, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCircleCheck, faCircleXmark, faFlag, faPiggyBank } from '@fortawesome/free-solid-svg-icons';
import { Observable } from 'rxjs';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { TeamService, Team, TeamPerson, TeamState, teamError } from '../../_services/team.service';
import { TeamEventsComponent } from '../team-events/team-events.component';
import { TeamMemberModalComponent } from './team-member-modal.component';
import { TeamEditModalComponent, TeamEditResult } from './team-edit-modal.component';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { ToastService } from '../../_services/toast.service';
import { UtilisateurSelectionnable } from '../../models/utilisateurSelectionnable';
import { UserCheckboxDropdownComponent } from '../user-checkbox-dropdown/user-checkbox-dropdown.component';
import { filterByName } from '../../_helpers/name-search';

type TeamEdit = { id?: number; nom: string; description: string; encadrants: number[]; referents: number[] };

@Component({ selector: 'app-team-management', imports: [FormsModule, FaIconComponent, TeamEventsComponent, UserCheckboxDropdownComponent],
  templateUrl: './team-management.component.html', styleUrls: ['./team-management.component.css'] })
export class TeamManagementComponent implements OnChanges, OnDestroy {
  private refresh = registerApiViewRefresh();
  private api = inject(TeamService);
  private toasts = inject(ToastService);
  private modals = inject(NgbModal);
  private memberModal?: NgbModalRef;
  private teamModal?: NgbModalRef;
  readonly faPiggyBank = faPiggyBank;
  readonly faCircleCheck = faCircleCheck;
  readonly faCircleXmark = faCircleXmark;
  readonly faFlag = faFlag;
  @Input() activityId?: number;
  @Input() selectedTeamId?: number;
  @Input() creating = false;
  @Output() stateChanged = new EventEmitter<TeamState>();
  @Output() teamCreated = new EventEmitter<Team>();
  @Output() busyChanged = new EventEmitter<boolean>();
  state?: TeamState;
  activeTeams: Team[] = [];
  error = ''; busy = false;
  newTeamDraft?: TeamEdit;
  selectedTeam?: Team;
  rosterExpanded = false;
  rosterSearch = '';
  get visibleCandidates() {
    return filterByName(this.state?.candidats ?? [], this.rosterSearch, person => `${person.prenom} ${person.nom}`);
  }
  get editor() { return this.creating ? this.newTeamDraft : undefined; }
  ngOnChanges(changes: SimpleChanges) {
    if (changes['selectedTeamId']) {
      this.rosterSearch = '';
      this.selectedTeam = this.activeTeams.find(team => team.id === this.selectedTeamId);
      this.closeTeamEditor();
    }
    if (changes['activityId']) {
      this.rosterSearch = '';
      this.rosterExpanded = false;
      this.closeMember();
      this.closeTeamEditor();
      this.state = undefined; this.activeTeams = []; this.selectedTeam = undefined;
      this.newTeamDraft = undefined;
      if (this.activityId) this.load();
    }
    if (changes['creating'] && this.creating && this.state && !this.newTeamDraft) this.openTeam();
  }
  ngOnDestroy() { this.closeMember(); this.closeTeamEditor(); }
  editSelectedTeam() {
    const team = this.selectedTeam;
    if (!this.activityId || !team?.canManage || this.creating || this.busy || this.teamModal) return;
    const modal = this.modals.open(TeamEditModalComponent, { size: 'lg', scrollable: true, backdrop: 'static', keyboard: false, ariaLabelledBy: 'team-edit-title' });
    this.teamModal = modal;
    modal.componentInstance.activityId = this.activityId;
    modal.componentInstance.team = team;
    modal.componentInstance.people = this.state?.intervenants ?? [];
    modal.result.then((result: TeamEditResult) => {
      if (this.teamModal !== modal) return;
      this.teamModal = undefined;
      this.toasts.success('deleted' in result ? 'Équipe supprimée.' : 'Équipe enregistrée.');
      this.error = ''; this.load();
    }, () => { if (this.teamModal === modal) this.teamModal = undefined; });
  }
  private closeTeamEditor() { this.teamModal?.dismiss(); this.teamModal = undefined; }
  openMember(person: TeamPerson) {
    if (!this.activityId || this.creating || !this.state?.candidats.some(p => p.adhesionId === person.adhesionId)) return;
    this.closeMember();
    const modal = this.modals.open(TeamMemberModalComponent, { scrollable: true, ariaLabelledBy: 'team-member-title' });
    this.memberModal = modal;
    modal.componentInstance.activityId = this.activityId;
    modal.componentInstance.person = person;
    const closed = () => { if (this.memberModal === modal) this.memberModal = undefined; };
    modal.result.then(closed, closed);
  }
  private closeMember() { this.memberModal?.dismiss(); this.memberModal = undefined; }
  load(created?: Team) {
    if (!this.activityId) return;
    const activityId = this.activityId;
    this.api.state(this.activityId).subscribe({ next: state => {
      if (activityId !== this.activityId) return;
      this.state = state;
      this.activeTeams = state.equipes.filter(t => !t.archivee);
      this.selectedTeam = this.activeTeams.find(team => team.id === this.selectedTeamId);
      if (this.creating && !this.newTeamDraft) this.openTeam();
      this.stateChanged.emit(state);
      if (created) this.teamCreated.emit(created);
    }, error: e => this.error = teamError(e) });
  }
  openTeam() {
    const teams = this.state?.equipes ?? [];
    let number = teams.length + 1;
    while (teams.some(team => team.nom.toLocaleLowerCase('fr') === `groupe ${number}`)) number++;
    this.newTeamDraft = { nom: `Groupe ${number}`, description: '', encadrants: [], referents: [] };
  }
  closeEditor() { this.openTeam(); }
  selectedStaff(type: 'encadrants' | 'referents') {
    return this.state?.intervenants.filter(person => this.editor?.[type].includes(person.id)) ?? [];
  }
  selectStaff(type: 'encadrants' | 'referents', selection: UtilisateurSelectionnable[]) {
    const edit = this.editor;
    if (edit) edit[type] = selection.map(person => person.id);
  }
  saveTeam() {
    const edit = this.editor;
    if (this.activityId && edit) this.run(this.api.save(this.activityId, edit), 'Équipe enregistrée.', team => {
      this.newTeamDraft = undefined; return team;
    });
  }
  toggleRosterMember(adhesionId: number, event?: Event) {
    const activityId = this.activityId, team = this.selectedTeam;
    // The checked state follows the saved roster, including when the request fails.
    if (event) (event.target as HTMLInputElement).checked = !!team?.effectif.includes(adhesionId);
    if (!activityId || !team?.canManage || this.creating || this.busy
      || !this.state?.candidats.some(person => person.adhesionId === adhesionId)) return;
    const effectif = team.effectif.includes(adhesionId)
      ? team.effectif.filter(id => id !== adhesionId) : [...team.effectif, adhesionId];
    this.busy = true; this.busyChanged.emit(true); this.error = '';
    this.api.roster(activityId, team.id, effectif).subscribe({ next: updated => {
      if (activityId === this.activityId && this.state) {
        this.state = { ...this.state, equipes: this.state.equipes.map(t => t.id === updated.id ? updated : t) };
        this.activeTeams = this.state.equipes.filter(t => !t.archivee);
        this.selectedTeam = this.activeTeams.find(t => t.id === this.selectedTeamId);
        this.toasts.success('Effectif enregistré.');
        this.stateChanged.emit(this.state);
      }
      this.busy = false; this.busyChanged.emit(false);
    }, error: e => { this.busy = false; this.busyChanged.emit(false); this.error = teamError(e); } });
  }
  run<T>(request: Observable<T>, message: string, after?: (result: T) => Team | void) {
    if (this.busy) return; this.busy = true; this.busyChanged.emit(true); this.error = '';
    request.subscribe({ next: result => {
      this.busy = false; this.busyChanged.emit(false); this.toasts.success(message);
      const created = after?.(result); this.load(created || undefined);
    }, error: e => { this.busy = false; this.busyChanged.emit(false); this.error = teamError(e); } });
  }
}
