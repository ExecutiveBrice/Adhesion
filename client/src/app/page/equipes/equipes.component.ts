import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TeamService, Team, teamError } from '../../_services/team.service';
import { TeamEventsComponent } from '../../template/team-events/team-events.component';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({ selector: 'app-equipes', imports: [FormsModule, TeamEventsComponent], template: `
  <main class="container py-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4"><div><h1 class="h3 mb-1">Mes équipes</h1><p class="text-muted mb-0">Événements, convocations et présences pour vous et les personnes que vous représentez.</p></div><button type="button" class="btn btn-outline-secondary" (click)="load()">Actualiser</button></div>
    @if (error) { <div class="alert alert-danger" role="alert">{{error}}</div> }
    @if (loading) { <p role="status">Chargement…</p> }
    <label class="form-label" for="selected-team">Équipe</label><select id="selected-team" class="form-select mb-4" [ngModel]="selected?.id" (ngModelChange)="select($event)"><option [ngValue]="undefined">Choisir une équipe</option>@for (t of teams; track t.id) { <option [ngValue]="t.id">{{t.activiteNom}} · {{t.nom}}{{t.archivee ? ' (archivée)' : ''}}</option> }</select>
    @if (selected) { <app-team-events [team]="selected" [teams]="teams" /> }
    @if (!loading && !teams.length && !error) { <div class="alert alert-info">Vous n’êtes affecté à aucune équipe pour le moment. Les équipes se configurent dans la fiche de chaque activité.</div> }
  </main>` })
export class EquipesComponent implements OnInit {
  private refresh = registerApiViewRefresh();
  private api = inject(TeamService);
  teams: Team[] = []; selected?: Team; loading = false; error = '';
  ngOnInit() { this.load(); }
  load() { this.loading = true; this.error = ''; this.api.mine().subscribe({ next: teams => { this.teams = teams; this.selected = teams.find(t => t.id === this.selected?.id) ?? teams.find(t => !t.archivee); this.loading = false; }, error: e => { this.error = teamError(e); this.loading = false; } }); }
  select(id: number) { this.selected = this.teams.find(t => t.id === id); }
}
