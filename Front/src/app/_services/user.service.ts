import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { User, UserLite } from '../models';

const API_URL = environment.server+'/user/';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private http: HttpClient) { }

  getConnectedUser(): Observable<User> {
    return this.http.get<User>(API_URL + 'connecteduser', { responseType: 'json' });
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

