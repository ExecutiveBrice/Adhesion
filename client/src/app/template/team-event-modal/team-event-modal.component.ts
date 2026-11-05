import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { forkJoin, Observable } from 'rxjs';
import { Team, TeamEvent, TeamEventEdit, TeamService, teamError } from '../../_services/team.service';
import { ParamService } from '../../_services/param.service';
import { SalleConfiguration } from '../../models/salle';
import { registerApiViewRefresh } from '../../_services/api-render.service';

@Component({ selector: 'app-team-event-modal', imports: [FormsModule], templateUrl: './team-event-modal.component.html' })
export class TeamEventModalComponent implements OnInit {
  private refresh = registerApiViewRefresh();
  private api = inject(TeamService);
  private params = inject(ParamService);
  modal = inject(NgbActiveModal);
  team?: Team;
  teams: Team[] = [];
  event?: TeamEvent;
  rooms: SalleConfiguration[] = [];
  sections: { id: number; nom: string }[] = [];
  selectedSections: number[] = [];
  repeat = false;
  weeks = 2;
  loading = true;
  busy = false;
  error = '';
  private automaticMeeting = true;
  form: TeamEventEdit = { equipes: [], type: 'ACTIVITE', debut: '', fin: '', salleId: null, lieu: '', rendezVous: '', adversaire: '', commentaire: '' };
  ngOnInit() {
    const date = new Date(); date.setDate(date.getDate() + 1); date.setHours(18, 0, 0, 0);
    const start = this.local(date);
    this.form = { equipes: this.team ? [this.team.id] : [], type: this.event?.type ?? 'ACTIVITE',
      debut: this.event?.debut.slice(0, 16) ?? start, fin: this.event?.fin.slice(0, 16) ?? this.shift(start, 60),
      salleId: this.event?.salleId ?? null, lieu: this.event?.salleId ? '' : this.event?.lieu ?? '',
      rendezVous: this.event?.rendezVous?.slice(0, 16) || this.shift(this.event?.debut.slice(0, 16) ?? start, -60),
      adversaire: this.event?.adversaire ?? '', commentaire: this.event?.commentaire ?? '' };
    this.automaticMeeting = !this.event;
    this.loadOptions();
  }
  loadOptions() {
    this.loading = true; this.error = '';
    const rooms = this.params.getSalles();
    if (this.team) {
      this.teams = [...this.teams.filter(t => t.id !== this.team!.id), this.team].filter(t => t.activiteId === this.team!.activiteId && t.canManage && !t.archivee);
      rooms.subscribe({ next: r => { this.rooms = r; this.loading = false; }, error: e => { this.error = teamError(e); this.loading = false; } });
    } else {
      forkJoin({ rooms, options: this.api.eventOptions() }).subscribe({ next: ({ rooms, options }) => {
        this.rooms = rooms; this.teams = options.equipes; this.sections = options.sections; this.loading = false;
      }, error: e => { this.error = teamError(e); this.loading = false; } });
    }
  }
  private local(date: Date) { return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16); }
  private shift(value: string, minutes: number) {
    const date = new Date(value + 'Z');
    return Number.isFinite(date.getTime()) ? new Date(date.getTime() + minutes * 60000).toISOString().slice(0, 16) : '';
  }
  startChanged(value: string) {
    this.form.debut = value;
    if (this.automaticMeeting) this.form.rendezVous = this.shift(value, -60);
  }
  meetingChanged(value: string) { this.form.rendezVous = value; this.automaticMeeting = false; }
  toggleTeam(id: number, checked: boolean) { this.form.equipes = this.form.equipes.filter(i => i !== id); if (checked) this.form.equipes.push(id); }
  toggleSection(id: number, checked: boolean) { this.selectedSections = this.selectedSections.filter(i => i !== id); if (checked) this.selectedSections.push(id); }
  get validationError(): string {
    if (!this.form.equipes.length && !this.selectedSections.length) return 'Sélectionnez au moins une équipe ou une section.';
    const start = Date.parse(this.form.debut + 'Z'), end = Date.parse(this.form.fin + 'Z');
    if (!Number.isFinite(start)) return 'Renseignez une date et une heure de début valides.';
    if (!Number.isFinite(end)) return 'Renseignez une date et une heure de fin valides.';
    if (end <= start) return 'La fin de l’événement doit être postérieure à son début.';
    if (end - start > 72 * 60 * 60 * 1000) return 'La durée de l’événement ne peut pas dépasser 72 heures.';
    if (this.form.rendezVous) {
      const meeting = Date.parse(this.form.rendezVous + 'Z');
      if (!Number.isFinite(meeting)) return 'Renseignez une date et une heure de rendez-vous valides.';
      if (meeting > start) return 'Le rendez-vous ne peut pas être postérieur au début de l’événement.';
    }
    if (this.repeat && (!Number.isInteger(this.weeks) || this.weeks < 2 || this.weeks > 104)) return 'Choisissez un nombre entier de semaines entre 2 et 104.';
    return '';
  }
  save() {
    if (this.busy || this.loading || this.validationError) return;
    this.busy = true; this.error = '';
    const form = { ...this.form, rendezVous: this.form.rendezVous || null, lieu: this.form.salleId ? null : this.form.lieu || null };
    const request: Observable<unknown> = this.event ? this.api.edit(this.event.id, form)
      : this.team && !this.repeat ? this.api.create(this.team.id, form)
      : this.api.createEvents(form, this.selectedSections, this.repeat ? this.weeks : 1);
    request.subscribe({ next: result => this.modal.close(result), error: e => { this.error = teamError(e); this.busy = false; } });
  }
}
