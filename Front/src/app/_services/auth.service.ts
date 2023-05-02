import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

const AUTH_API = environment.server+'/api_adhesion/auth/';

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

  reinitPassword(username: string): Observable<any> {
    return this.http.post(AUTH_API + 'reinitPassword', {
      username
    },{ responseType: 'json' });
  
  }

  register(username: string, password: string): Observable<any> {
    return this.http.post(AUTH_API + 'signup', {
      username,
      password
    },{ responseType: 'json' });
  }
}
