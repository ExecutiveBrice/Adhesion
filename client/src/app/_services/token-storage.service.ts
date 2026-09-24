import { Injectable } from '@angular/core';
import { debounceTime, filter, fromEvent } from 'rxjs';

const TOKEN_KEY = 'auth-token';
const USER_KEY = 'auth-user';
const REFRESH_TOKEN_KEY = 'auth-refresh-token';

@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  // L'événement storage est émis uniquement dans les autres onglets.
  // Regrouper les événements évite plusieurs rechargements lors de la
  // sauvegarde successive du token et de l'utilisateur.
  readonly sessionChanges$ = fromEvent<StorageEvent>(window, 'storage').pipe(
    filter(event => event.storageArea === window.localStorage
      && (event.key === TOKEN_KEY || event.key === USER_KEY || event.key === null)),
    debounceTime(0)
  );

  constructor() { }

  signOut(): void {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(USER_KEY);
    window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  public saveToken(token: string): void {
    window.localStorage.setItem(TOKEN_KEY, token);
  }

  public getToken(): string | null {
    return window.localStorage.getItem(TOKEN_KEY);
  }

  public saveUser(user: any): void {
    const { refreshToken, ...profile } = user;
    window.localStorage.setItem(USER_KEY, JSON.stringify(profile));
  }

  public saveSession(session: any): void {
    this.saveToken(session.token);
    this.saveUser(session);
    if (session.refreshToken) {
      window.localStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken);
    } else {
      window.localStorage.removeItem(REFRESH_TOKEN_KEY);
    }
  }

  public getRefreshToken(): string | null {
    return window.localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  public isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;
    try {
      const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const { exp } = JSON.parse(window.atob(payload));
      return typeof exp !== 'number' || exp * 1000 <= Date.now() + 30000;
    } catch {
      return true;
    }
  }

  public getUser(): any {
    const user = window.localStorage.getItem(USER_KEY);
    if (user) {
      return JSON.parse(user);
    }

    return {};
  }
}
