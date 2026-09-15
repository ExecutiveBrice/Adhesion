import { Injectable, inject } from '@angular/core';
import { HttpBackend, HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { catchError, finalize, firstValueFrom, Observable, of, shareReplay, tap, throwError, timeout } from 'rxjs';
import { environment } from 'src/environments/environment';
import { Adherent, User } from '../models';
import { TokenStorageService } from './token-storage.service';
import { PwaService } from './pwa.service';

const AUTH_API = environment.server+'/auth/';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  // Le renouvellement contourne l'intercepteur pour éviter toute boucle sur un 401.
  private sessionHttp = new HttpClient(inject(HttpBackend));
  private storage = inject(TokenStorageService);
  private pwa = inject(PwaService);
  private refreshInFlight?: Observable<any>;



  impersonate(username: string): Observable<any> {
    return this.http.post(AUTH_API+'impersonate/'+username, {},{ responseType: 'json' }).pipe(
      // Une session d'impersonnalisation ne doit pas renouveler la session de l'administrateur.
      tap(() => this.clearRememberedSession())
    );
  }


  login(username: string, password: string): Observable<any> {
    return this.http.post(AUTH_API + 'signin', {
      username,
      password,
      rememberSession: this.pwa.isMobileStandalone()
    },{ responseType: 'json' }).pipe(tap(session => this.storage.saveSession(session)));
  }

  canRefreshSession(): boolean {
    return this.pwa.isMobileStandalone() && !!this.storage.getRefreshToken();
  }

  async restoreSession(): Promise<void> {
    if (!this.canRefreshSession() || !this.storage.isTokenExpired()) return;
    await firstValueFrom(this.refreshSession().pipe(catchError(() => of(null))));
  }

  refreshSession(): Observable<any> {
    if (this.refreshInFlight) return this.refreshInFlight;
    const refreshToken = this.storage.getRefreshToken();
    if (!this.canRefreshSession() || !refreshToken) {
      return throwError(() => new HttpErrorResponse({ status: 401 }));
    }
    this.refreshInFlight = this.sessionHttp.post<any>(AUTH_API + 'refresh', { refreshToken }).pipe(
      timeout(10000),
      tap(session => {
        // Une déconnexion pendant la requête ne doit pas rouvrir la session.
        if (this.storage.getRefreshToken() !== refreshToken) {
          this.revokeRememberedSession(session.refreshToken).subscribe();
          throw new HttpErrorResponse({ status: 401 });
        }
        this.storage.saveSession(session);
      }),
      catchError(error => {
        if (error instanceof HttpErrorResponse && (error.status === 400 || error.status === 401)
          && this.storage.getRefreshToken() === refreshToken) {
          this.storage.signOut();
        }
        // Une coupure réseau conserve le jeton pour la prochaine tentative.
        return throwError(() => error);
      }),
      finalize(() => this.refreshInFlight = undefined),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    return this.refreshInFlight;
  }

  logout(): Observable<unknown> {
    const refreshToken = this.storage.getRefreshToken();
    this.storage.signOut();
    return this.revokeRememberedSession(refreshToken);
  }

  private clearRememberedSession(): void {
    const refreshToken = this.storage.getRefreshToken();
    if (!refreshToken) return;
    // Effacer le jeton tout en conservant la session JWT courante.
    const session = { ...this.storage.getUser(), token: this.storage.getToken() };
    this.storage.saveSession(session);
    this.revokeRememberedSession(refreshToken).subscribe();
  }

  private revokeRememberedSession(refreshToken: string | null): Observable<unknown> {
    return refreshToken
      ? this.sessionHttp.post(AUTH_API + 'signout', { refreshToken }).pipe(
          timeout(5000), catchError(() => of(null)))
      : of(null);
  }

  changePassword(token: string, password: string): Observable<any> {
    return this.http.post(AUTH_API + 'changePassword', {token, password},{responseType: 'json'});
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
