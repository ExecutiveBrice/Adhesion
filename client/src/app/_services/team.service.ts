import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AdherentResponsabilite, ResponsabiliteSeance } from '../models/responsabiliteSeance';

export interface ResponsibilityTeam { id: number; nom: string; activiteNom: string; attributaire: AdherentResponsabilite | null; }
export interface TeamResponsibilityEdit { tacheId?: number; equipeId?: number; }

export interface TeamPerson { id: number; prenom: string; nom: string; adhesionId: number; statut: string; }
export interface Team { id: number; activiteId: number; activiteNom: string; nom: string; description: string; archivee: boolean;
  canManage: boolean; encadrants: number[]; referents: number[]; effectif: number[]; membres: number; principale?: boolean; }
export interface TeamState { equipes: Team[]; candidats: TeamPerson[]; intervenants: TeamPerson[]; }
export interface TeamInvitation { id: number; adherentId: number; adhesionId: number; nom: string; selectionnee: boolean;
  publication: string | null; reponse: boolean | null; dateReponse: string | null; presence: boolean | null; canReply: boolean; }
export interface TeamEvent { id: number; type: string; debut: string; fin: string; etat: string; lieu: string; salleId: number | null;
  rendezVous: string; adversaire: string; commentaire: string; equipes: string[]; canManage: boolean;
  convocations: TeamInvitation[]; emails: Record<string, number>; responsabilites?: ResponsabiliteSeance[]; }
export interface TeamEventEdit { equipes: number[]; type: string; debut: string; fin: string; salleId: number | null;
  lieu: string | null; rendezVous: string | null; adversaire: string; commentaire: string; }
export interface EventCreationOptions { equipes: Team[]; sections: { id: number; nom: string }[]; }

@Injectable({ providedIn: 'root' })
export class TeamService {
  private http = inject(HttpClient);
  private url = environment.server + '/teams';
  mine() { return this.http.get<Team[]>(this.url + '/mine'); }
  eventOptions() { return this.http.get<EventCreationOptions>(this.url + '/event-options'); }
  createEvents(evenement: TeamEventEdit, sections: number[], semaines: number) {
    return this.http.post<{ nombre: number; evenements: TeamEvent[] }>(this.url + '/events', { evenement, sections, semaines });
  }
  state(id: number) { return this.http.get<TeamState>(`${this.url}/activities/${id}`); }
  save(id: number, team: { id?: number; nom: string; description: string; encadrants: number[]; referents: number[] }) {
    const url = `${this.url}/activities/${id}/equipes`;
    return team.id ? this.http.put<Team>(`${url}/${team.id}`, team) : this.http.post<Team>(url, team);
  }
  archive(id: number, teamId: number) { return this.http.delete<void>(`${this.url}/activities/${id}/equipes/${teamId}`); }
  roster(id: number, teamId: number, adhesions: number[], applyToFuture: boolean) {
    return this.http.put<Team>(`${this.url}/activities/${id}/equipes/${teamId}/effectif`, { adhesions, applyToFuture });
  }
  events(id: number) { return this.http.get<TeamEvent[]>(`${this.url}/equipes/${id}/events`); }
  create(id: number, event: TeamEventEdit) { return this.http.post<TeamEvent>(`${this.url}/equipes/${id}/events`, event); }
  edit(id: number, event: TeamEventEdit) { return this.http.put<TeamEvent>(`${this.url}/events/${id}`, event); }
  candidates(id: number) { return this.http.get<TeamPerson[]>(`${this.url}/events/${id}/candidates`); }
  responsibilityCandidates(id: number) { return this.http.get<AdherentResponsabilite[]>(`${this.url}/events/${id}/responsibility-candidates`); }
  responsibilityTeams(id: number) { return this.http.get<ResponsibilityTeam[]>(`${this.url}/events/${id}/responsibility-teams`); }
  responsibilities(id: number, responsibilities: TeamResponsibilityEdit[]) { return this.http.put<TeamEvent>(`${this.url}/events/${id}/responsibilities`, responsibilities); }
  select(id: number, adhesions: number[]) { return this.http.put<TeamEvent>(`${this.url}/events/${id}/selection`, { adhesions }); }
  publish(id: number) { return this.http.post<TeamEvent>(`${this.url}/events/${id}/publish`, {}); }
  remind(id: number, requestId: string) { return this.http.post<TeamEvent>(`${this.url}/events/${id}/remind`, { requestId }); }
  cancel(id: number) { return this.http.post<TeamEvent>(`${this.url}/events/${id}/cancel`, {}); }
  reply(id: number, invitationId: number, value: boolean) {
    return this.http.patch<TeamEvent>(`${this.url}/events/${id}/invitations/${invitationId}/reply`, { value });
  }
  attendance(id: number, invitationId: number, value: boolean) {
    return this.http.patch<TeamEvent>(`${this.url}/events/${id}/invitations/${invitationId}/attendance`, { value });
  }
}

export function teamError(error: any): string {
  return error?.error?.detail || error?.error?.message || 'L’opération n’a pas pu être enregistrée. Réessayez après actualisation.';
}
