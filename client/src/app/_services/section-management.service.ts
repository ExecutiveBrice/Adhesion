import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { ChatConfiguration } from '../models';

export interface ManagedSection { id: number; nom: string; couleur: string; }
export interface SectionActivity {
  id: number; nom: string; horaire: string; tarif: number; nbPlaces: number;
  complete: boolean; reinscription: boolean; lien: string;
  adhesionsValidees: number; adhesionsEnCours: number; adhesionsEnAttente: number;
}
export interface SectionActivityCreation {
  nom: string; tarif: number; nbPlaces: number; ageMin: number; ageMax: number;
  genre: string; jour: string; horaireDebut: string; duree: number;
}
export interface SectionMembership {
  id: number; adherent: string; activiteId: number; activite: string; statut: string;
  dateChangementStatut?: string; paiementValide?: boolean; documentsValides?: boolean;
}
export interface SectionMembershipPage {
  content: SectionMembership[]; totalElements: number; totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class SectionManagementService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.server + '/section-management/sections';

  sections(): Observable<ManagedSection[]> {
    return this.http.get<ManagedSection[]>(this.url);
  }

  activities(sectionId: number): Observable<SectionActivity[]> {
    return this.http.get<SectionActivity[]>(`${this.url}/${sectionId}/activities`);
  }

  createActivity(sectionId: number, activity: SectionActivityCreation): Observable<SectionActivity> {
    return this.http.post<SectionActivity>(`${this.url}/${sectionId}/activities`, activity);
  }

  updateActivity(sectionId: number, activity: SectionActivity): Observable<SectionActivity> {
    const { nom, tarif, nbPlaces, complete, reinscription, lien } = activity;
    return this.http.put<SectionActivity>(`${this.url}/${sectionId}/activities/${activity.id}`,
      { nom, tarif, nbPlaces, complete, reinscription, lien });
  }

  memberships(sectionId: number, page: number, search = '', status = '', size = 20): Observable<SectionMembershipPage> {
    const params = new HttpParams().set('page', page).set('size', size).set('search', search).set('status', status);
    return this.http.get<SectionMembershipPage>(`${this.url}/${sectionId}/memberships`, { params });
  }

  updateStatus(sectionId: number, membershipId: number, statut: string): Observable<SectionMembership> {
    return this.http.put<SectionMembership>(
      `${this.url}/${sectionId}/memberships/${membershipId}/status`, { statut });
  }

  chats(sectionId: number): Observable<ChatConfiguration[]> {
    return this.http.get<ChatConfiguration[]>(`${this.url}/${sectionId}/chats`);
  }

  createChat(sectionId: number, chat: ChatConfiguration): Observable<ChatConfiguration> {
    return this.http.post<ChatConfiguration>(`${this.url}/${sectionId}/chats`, chat);
  }

  updateChat(sectionId: number, chat: ChatConfiguration): Observable<ChatConfiguration> {
    return this.http.put<ChatConfiguration>(`${this.url}/${sectionId}/chats/${chat.id}`, chat);
  }

  deleteChat(sectionId: number, chatId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${sectionId}/chats/${chatId}`);
  }

}
