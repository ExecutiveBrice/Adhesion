import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { TeamService, Team, TeamState, teamError } from '../../_services/team.service';
import { TeamEventsComponent } from '../team-events/team-events.component';
import { registerApiViewRefresh } from '../../_services/api-render.service';

type TeamEdit = { id?: number; nom: string; description: string; encadrants: number[]; referents: number[] };

@Component({ selector: 'app-team-management', imports: [FormsModule, TeamEventsComponent],
  templateUrl: './team-management.component.html', styleUrls: ['./team-management.component.css'] })
export class TeamManagementComponent implements OnChanges {
  private refresh = registerApiViewRefresh();
  private api = inject(TeamService);
  @Input() activityId?: number;
  @Input() selectedTeamId?: number;
  @Input() creating = false;
  @Output() stateChanged = new EventEmitter<TeamState>();
  @Output() teamCreated = new EventEmitter<Team>();
  @Output() busyChanged = new EventEmitter<boolean>();
  state?: TeamState;
  activeTeams: Team[] = [];
  error = ''; message = ''; busy = false;
  teamEdit?: TeamEdit;
  newTeamDraft?: TeamEdit;
  rosterTeam?: Team;
  selected: number[] = [];
  search = ''; onlyUnassigned = false; applyToFuture = false;
  selectedTeam?: Team;
  get editor() { return this.creating ? this.newTeamDraft : this.teamEdit; }
  ngOnChanges(changes: SimpleChanges) {
    if (changes['selectedTeamId']) {
      this.selectedTeam = this.activeTeams.find(team => team.id === this.selectedTeamId);
      this.teamEdit = undefined; this.rosterTeam = undefined;
    }
    if (changes['activityId']) {
      this.state = undefined; this.activeTeams = []; this.selectedTeam = undefined;
      this.newTeamDraft = undefined;
      if (this.activityId) this.load();
    }
    if (changes['creating'] && this.creating && this.state && !this.newTeamDraft) this.openTeam();
  }
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
  get unassigned() { const assigned = new Set(this.activeTeams.flatMap(t => t.effectif)); return this.state?.candidats.filter(p => !assigned.has(p.adhesionId)) ?? []; }
  get filteredPeople() { const q = this.search.toLocaleLowerCase('fr'); const list = this.onlyUnassigned ? this.unassigned : this.state?.candidats ?? [];
    return list.filter(p => `${p.prenom} ${p.nom}`.toLocaleLowerCase('fr').includes(q)); }
  openTeam(t?: Team) {
    const teams = this.state?.equipes ?? [];
    let number = teams.length + 1;
    while (teams.some(team => team.nom.toLocaleLowerCase('fr') === `groupe ${number}`)) number++;
    const draft = { id: t?.id, nom: t?.nom ?? `Groupe ${number}`, description: t?.description ?? '', encadrants: [...(t?.encadrants ?? [])], referents: [...(t?.referents ?? [])] };
    if (t) this.teamEdit = draft; else this.newTeamDraft = draft;
  }
  closeEditor() { if (this.creating) this.openTeam(); else this.teamEdit = undefined; }
  toggleStaff(personId: number, type: 'encadrants' | 'referents', checked: boolean) {
    const edit = this.editor;
    if (!edit) return; edit[type] = edit[type].filter(id => id !== personId); if (checked) edit[type].push(personId);
  }
  saveTeam() {
    const edit = this.editor;
    if (this.activityId && edit) this.run(this.api.save(this.activityId, edit), 'Équipe enregistrée.', team => {
      if (!edit.id) { this.newTeamDraft = undefined; return team; }
      this.teamEdit = undefined; return undefined;
    });
  }
  archive(t: Team) { if (this.activityId && confirm(`Archiver l’équipe ${t.nom} ? Son historique sera conservé.`)) this.run(this.api.archive(this.activityId, t.id), 'Équipe archivée.'); }
  openRoster(t: Team) { this.rosterTeam = t; this.selected = [...t.effectif]; this.search = ''; this.onlyUnassigned = false; this.applyToFuture = false; }
  togglePerson(id: number, checked: boolean) { this.selected = this.selected.filter(v => v !== id); if (checked) this.selected.push(id); }
  saveRoster() { if (this.activityId && this.rosterTeam) this.run(this.api.roster(this.activityId, this.rosterTeam.id, this.selected, this.applyToFuture), 'Effectif enregistré. Les convocations déjà publiées et les pointages sont conservés.', () => this.rosterTeam = undefined); }
  names(ids: number[]) { return this.state?.intervenants.filter(p => ids.includes(p.id)).map(p => `${p.prenom} ${p.nom}`).join(', ') || 'Non renseigné'; }
  run<T>(request: Observable<T>, message: string, after?: (result: T) => Team | void) {
    if (this.busy) return; this.busy = true; this.busyChanged.emit(true); this.error = ''; this.message = '';
    request.subscribe({ next: result => {
      this.busy = false; this.busyChanged.emit(false); this.message = message;
      const created = after?.(result); this.load(created || undefined);
    }, error: e => { this.busy = false; this.busyChanged.emit(false); this.error = teamError(e); } });
  }
}
