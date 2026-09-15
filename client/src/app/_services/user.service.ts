import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { User, UserLite } from '../models';
import { PresenceSeance, SeanceDuJour } from '../models/seance';

const API_URL = environment.server+'/user/';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);


  getConnectedUser(): Observable<User> {
    return this.http.get<User>(API_URL + 'connecteduser', { responseType: 'json' });
  }

  getSeancesDuJour(): Observable<SeanceDuJour[]> {
    return this.http.get<SeanceDuJour[]>(API_URL + 'seancesDuJour', { responseType: 'json' });
  }

  getSeancesDuJourPourLeSecretariat(date: string): Observable<SeanceDuJour[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<SeanceDuJour[]>(API_URL + 'secretariat/seances', { params, responseType: 'json' });
  }

  getPresencesPourLeSecretariat(seanceId: number): Observable<PresenceSeance[]> {
    return this.http.get<PresenceSeance[]>(API_URL + 'secretariat/seances/' + seanceId + '/presences', { responseType: 'json' });
  }

  updatePresencePourLeSecretariat(seanceId: number, presenceId: number, presence: boolean): Observable<PresenceSeance> {
    return this.http.patch<PresenceSeance>(API_URL + 'secretariat/seances/' + seanceId + '/presences/' + presenceId,
      { presence }, { responseType: 'json' });
  }

  updateCommentaireSeancePourLeSecretariat(seanceId: number, commentaire: string | null): Observable<SeanceDuJour> {
    return this.http.patch<SeanceDuJour>(API_URL + 'secretariat/seances/' + seanceId + '/commentaire',
      { commentaire }, { responseType: 'json' });
  }

  getPresences(seanceId: number): Observable<PresenceSeance[]> {
    return this.http.get<PresenceSeance[]>(API_URL + 'seances/' + seanceId + '/presences', { responseType: 'json' });
  }

  updatePresence(seanceId: number, presenceId: number, presence: boolean): Observable<PresenceSeance> {
    return this.http.patch<PresenceSeance>(API_URL + 'seances/' + seanceId + '/presences/' + presenceId,
      { presence }, { responseType: 'json' });
  }

  updateCommentaireSeance(seanceId: number, commentaire: string | null): Observable<SeanceDuJour> {
    return this.http.patch<SeanceDuJour>(API_URL + 'seances/' + seanceId + '/commentaire',
      { commentaire }, { responseType: 'json' });
  }

  ajouterNouvelAdherentSeance(seanceId: number, email: string): Observable<PresenceSeance> {
    return this.http.post<PresenceSeance>(API_URL + 'seances/' + seanceId + '/adherents',
      { email }, { responseType: 'json' });
  }
  getUserByMail(userEmail: String): Observable<User> {
    let params = new HttpParams().set('userEmail', '' + userEmail + '');
    return this.http.get<User>(API_URL + 'getUserByMail', {params, responseType: 'json' });
  }
  updateUser(user: User): Observable<any> {
    let params = new HttpParams().set('eventId', '' + user.adherent.tribu + '');
    return this.http.put(API_URL + 'user', user, {params, responseType: 'json' });
  }

  grantUser(role: String, userEmail: String): Observable<User> {
    let params = new HttpParams().set('userEmail', '' + userEmail + '');
    return this.http.post<User>(API_URL + 'grantUser', role, {params, responseType: 'json' });
  }

  unGrantUser(role: String, userEmail: String): Observable<User> {
    let params = new HttpParams().set('userEmail', '' + userEmail + '');
    return this.http.post<User>(API_URL + 'unGrantUser', role, {params, responseType: 'json' });
  }

  getAllLite(): Observable<UserLite[]> {
    return this.http.get<UserLite[]>(API_URL + 'allLite', { responseType: 'json' });
  }

  getUserByRole(role: String): Observable<UserLite[]> {
    let params = new HttpParams().set('role', '' + role + '');
    return this.http.get<UserLite[]>(API_URL + 'getUserByRole', {params, responseType: 'json' });
  }



}

