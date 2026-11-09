import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { AdherentResponsabilite, ResponsabiliteSeance } from '../models/responsabiliteSeance';

export interface ResponsibilityTeam { id: number; nom: string; activiteNom: string; attributaire: AdherentResponsabilite | null; }
export interface TeamResponsibilityEdit { tacheId?: number; equipeId?: number; }

export interface TeamPerson { id: number; prenom: string; nom: string; adhesionId: number; statut: string;
  paiementValide?: boolean; inscriptionValidee?: boolean; flag?: boolean; }
export interface TeamMemberDetails { personne: TeamPerson; naissance: string | null; email: string | null; telephone: string | null;
  adresse: string | null; codePostal: string | null; ville: string | null;
  representant: { prenom: string; nom: string; email: string | null; telephone: string | null } | null; }
export interface Team { id: number; activiteId: number; activiteNom: string; nom: string; description: string; archivee: boolean;
  canManage: boolean; encadrants: number[]; referents: number[]; effectif: number[]; membres: number; principale?: boolean; }
export interface TeamState { equipes: Team[]; candidats: TeamPerson[]; intervenants: TeamPerson[]; }
export interface TeamInvitation { id: number; adherentId: number; adhesionId: number | null; nom: string; selectionnee: boolean;
  prenom?: string | null; nomFamille?: string | null; equipes?: string[];
  // A null response with a date is an explicit “not sure yet” reply.
  publication: string | null; reponse: boolean | null; dateReponse: string | null; presence: boolean | null; canReply: boolean; }
export interface TeamEvent { id: number; type: string; debut: string; fin: string; etat: string; lieu: string; salleId: number | null;
  rendezVous: string; adversaire: string; commentaire: string; equipes: string[]; canManage: boolean;
  convocations: TeamInvitation[]; emails: Record<string, number>; responsabilites?: ResponsabiliteSeance[]; }
export interface TeamEventEdit { equipes: number[]; type: string; debut: string; fin: string; salleId: number | null;
  lieu: string | null; rendezVous: string | null; adversaire: string; commentaire: string; }
export interface TeamEventDetails { evenement: TeamEvent; activiteId: number; activiteNom: string;
  sectionNom: string | null; adresse: string | null; causeAnnulation: string | null; lien: string | null; canChat: boolean; }
export interface EventChatMessage { id: number; senderUserId: number; senderName: string; content: string; createdAt: string; }
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
  memberDetails(id: number, adhesionId: number) { return this.http.get<TeamMemberDetails>(`${this.url}/activities/${id}/adhesions/${adhesionId}`); }
  save(id: number, team: { id?: number; nom: string; description: string; encadrants: number[]; referents: number[] }) {
    const url = `${this.url}/activities/${id}/equipes`;
    return team.id ? this.http.put<Team>(`${url}/${team.id}`, team) : this.http.post<Team>(url, team);
  }
  archive(id: number, teamId: number) { return this.http.delete<void>(`${this.url}/activities/${id}/equipes/${teamId}`); }
  roster(id: number, teamId: number, adhesions: number[]) {
    return this.http.put<Team>(`${this.url}/activities/${id}/equipes/${teamId}/effectif`, { adhesions });
  }
  events(id: number) { return this.http.get<TeamEvent[]>(`${this.url}/equipes/${id}/events`); }
  event(id: number) { return this.http.get<TeamEventDetails>(`${this.url}/events/${id}`); }
  eventMessages(id: number, cursor?: { beforeId?: number; afterId?: number }) {
    let params = new HttpParams();
    if (cursor?.beforeId) params = params.set('beforeId', cursor.beforeId);
    if (cursor?.afterId) params = params.set('afterId', cursor.afterId);
    return this.http.get<EventChatMessage[]>(`${this.url}/events/${id}/messages`, { params });
  }
  sendEventMessage(id: number, content: string) {
    return this.http.post<EventChatMessage>(`${this.url}/events/${id}/messages`, { content });
  }
  create(id: number, event: TeamEventEdit) { return this.http.post<TeamEvent>(`${this.url}/equipes/${id}/events`, event); }
  edit(id: number, event: TeamEventEdit) { return this.http.put<TeamEvent>(`${this.url}/events/${id}`, event); }
  responsibilityCandidates(id: number) { return this.http.get<AdherentResponsabilite[]>(`${this.url}/events/${id}/responsibility-candidates`); }
  responsibilityTeams(id: number) { return this.http.get<ResponsibilityTeam[]>(`${this.url}/events/${id}/responsibility-teams`); }
  responsibilities(id: number, responsibilities: TeamResponsibilityEdit[]) { return this.http.put<TeamEvent>(`${this.url}/events/${id}/responsibilities`, responsibilities); }
  publish(id: number) { return this.http.post<TeamEvent>(`${this.url}/events/${id}/publish`, {}); }
  remind(id: number, requestId: string) { return this.http.post<TeamEvent>(`${this.url}/events/${id}/remind`, { requestId }); }
  publishInvitation(id: number, invitationId: number) {
    return this.http.post<TeamEvent>(`${this.url}/events/${id}/invitations/${invitationId}/publish`, {});
  }
  remindInvitation(id: number, invitationId: number, requestId: string) {
    return this.http.post<TeamEvent>(`${this.url}/events/${id}/invitations/${invitationId}/remind`, { requestId });
  }
  cancel(id: number) { return this.http.post<TeamEvent>(`${this.url}/events/${id}/cancel`, {}); }
  reply(id: number, invitationId: number, value: boolean | null) {
    return this.http.patch<TeamEvent>(`${this.url}/events/${id}/invitations/${invitationId}/reply`, { value });
  }
  attendance(id: number, invitationId: number, value: boolean) {
    return this.http.patch<TeamEvent>(`${this.url}/events/${id}/invitations/${invitationId}/attendance`, { value });
  }
}

export function teamError(error: any): string {
  return error?.error?.detail || error?.error?.message || 'L’opération n’a pas pu être enregistrée. Réessayez après actualisation.';
}
