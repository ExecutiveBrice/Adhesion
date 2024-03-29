import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Adherent, User } from '../models';

const AUTH_API = environment.server+'/auth/';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private http: HttpClient) { }

  login(username: string, password: string): Observable<any> {
    return this.http.post(AUTH_API + 'signin', {
      username,
      password
    },{ responseType: 'json' });
  }

  changePassword(token: string, password: string): Observable<any> {
    let params = new HttpParams().set('token', '' + token + '');
    return this.http.post(AUTH_API + 'changePassword', {password},{params,  responseType: 'text' });
  }

  
  userExist(username: string): Observable<any> {
    return this.http.post(AUTH_API + 'userExist', {
      username
    },{ responseType: 'text' });
  
  }

  reinitPassword(username: string): Observable<any> {
    return this.http.post(AUTH_API + 'reinitPassword', {
      username
    },{ responseType: 'json' });
  
  }
  registerAnonymous(email: string): Observable<Adherent> {
    let params = new HttpParams().set('email', '' + email + '');
    return this.http.post<Adherent>(AUTH_API + 'signupAnonymous', {},{params, responseType: 'json' });
  }


  register(username: string, password: string): Observable<any> {
    return this.http.post(AUTH_API + 'signup', {
      username,
      password
    },{ responseType: 'json' });
  }
}
