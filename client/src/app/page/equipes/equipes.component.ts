import { Component, inject, OnInit } from '@angular/core';
import { TeamService, Team, teamError } from '../../_services/team.service';
import { TeamEventsComponent } from '../../template/team-events/team-events.component';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({ selector: 'app-equipes', imports: [TeamEventsComponent], styleUrls: ['./equipes.component.css'], template: `
  <main class="equipes-page">
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4"><div><h1 class="h3 mb-1">Mes équipes</h1><p class="text-muted mb-0">Les équipes dont vous êtes référent ou encadrant, leurs événements, convocations et présences.</p></div><button type="button" class="btn btn-outline-secondary" (click)="load()">Actualiser</button></div>
    @if (error) { <div class="alert alert-danger" role="alert">{{error}}</div> }
    @if (loading) { <p role="status">Chargement…</p> }
    @if (teams.length) {
      <div class="team-buttons mb-4" role="group" aria-label="Choisir une équipe">
        @for (t of teams; track t.id) {
          <button type="button" class="btn team-button" [class.btn-primary]="selected?.id === t.id"
            [class.btn-outline-primary]="selected?.id !== t.id" [attr.aria-pressed]="selected?.id === t.id"
            aria-controls="selected-team-events" (click)="select(t.id)">
            <span class="team-activity">{{t.activiteNom}}</span>
            <strong>{{t.nom}}</strong>
            @if (t.archivee) { <span class="small">Archivée</span> }
          </button>
        }
      </div>
    }
    <section id="selected-team-events" aria-label="Événements de l’équipe sélectionnée">
      @if (selected) {
        <h2 class="h4 mb-3">{{selected.activiteNom}} · {{selected.nom}}</h2>
        <app-team-events [team]="selected" [teams]="teams" [summaryCards]="true" [collapsibleParticipants]="true" [allowEventEditing]="false" />
      } @else if (!loading && teams.length) {
        <p class="text-muted">Choisissez une équipe pour afficher ses événements.</p>
      }
    </section>
    @if (!loading && !teams.length && !error) { <div class="alert alert-info">Vous n’êtes référent ou encadrant d’aucune équipe pour le moment. Les équipes se configurent dans la fiche de chaque activité.</div> }
  </main>` })
export class EquipesComponent implements OnInit {
  private refresh = registerApiViewRefresh();
  private api = inject(TeamService);
  teams: Team[] = []; selected?: Team; loading = false; error = '';
  ngOnInit() { this.load(); }
  load() { this.loading = true; this.error = ''; this.api.mine().subscribe({ next: teams => { this.teams = teams; this.selected = teams.find(t => t.id === this.selected?.id); this.loading = false; }, error: e => { this.error = teamError(e); this.loading = false; } }); }
  select(id: number) { this.selected = this.teams.find(t => t.id === id); }
}
