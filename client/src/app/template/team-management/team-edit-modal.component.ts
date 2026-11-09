import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Team, TeamPerson, TeamService, teamError } from '../../_services/team.service';
import { registerApiViewRefresh } from '../../_services/api-render.service';
import { UtilisateurSelectionnable } from '../../models/utilisateurSelectionnable';
import { UserCheckboxDropdownComponent } from '../user-checkbox-dropdown/user-checkbox-dropdown.component';

export type TeamEditResult = { deleted: true } | { team: Team };

@Component({ selector: 'app-team-edit-modal', imports: [FormsModule, UserCheckboxDropdownComponent], templateUrl: './team-edit-modal.component.html' })
export class TeamEditModalComponent implements OnInit {
  private refresh = registerApiViewRefresh();
  private api = inject(TeamService);
  private destroyRef = inject(DestroyRef);
  readonly modal = inject(NgbActiveModal);
  activityId!: number;
  team!: Team;
  people: TeamPerson[] = [];
  form = { id: 0, nom: '', description: '', encadrants: [] as number[], referents: [] as number[] };
  busy = false;
  error = '';

  ngOnInit() {
    this.form = { id: this.team.id, nom: this.team.nom, description: this.team.description ?? '',
      encadrants: [...this.team.encadrants], referents: [...this.team.referents] };
  }
  selectedStaff(type: 'encadrants' | 'referents') {
    return this.people.filter(person => this.form[type].includes(person.id));
  }
  selectStaff(type: 'encadrants' | 'referents', selection: UtilisateurSelectionnable[]) {
    this.form[type] = selection.map(person => person.id);
  }
  save() {
    if (this.busy || !this.team.canManage || !this.form.nom.trim()) return;
    this.busy = true; this.error = '';
    this.api.save(this.activityId, this.form).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: team => this.modal.close({ team } satisfies TeamEditResult),
      error: error => { this.error = teamError(error); this.busy = false; }
    });
  }
  deleteTeam() {
    if (this.busy || !this.team.canManage || this.team.principale
      || !confirm(`Supprimer l’équipe ${this.team.nom} ? Son historique sera conservé.`)) return;
    this.busy = true; this.error = '';
    this.api.archive(this.activityId, this.team.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.modal.close({ deleted: true } satisfies TeamEditResult),
      error: error => { this.error = teamError(error); this.busy = false; }
    });
  }
}
