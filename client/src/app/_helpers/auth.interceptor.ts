import { HTTP_INTERCEPTORS, HttpErrorResponse, HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { HttpInterceptor, HttpHandler, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';

import { TokenStorageService } from '../_services/token-storage.service';
import { catchError, finalize, Observable, switchMap, throwError } from 'rxjs';
import { ApiRenderService } from '../_services/api-render.service';
import { AuthService } from '../_services/auth.service';
import { environment } from '../../environments/environment';

const TOKEN_HEADER_KEY = 'Authorization';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private token = inject(TokenStorageService);
  private router = inject(Router);
  private auth = inject(AuthService);


  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const apiBase = environment.server.replace(/\/$/, '') + '/';
    if (!req.url.startsWith(apiBase)) return next.handle(req);

    const token = this.token.getToken();
    const send = (accessToken: string | null) => next.handle(accessToken
      ? req.clone({ headers: req.headers.set(TOKEN_HEADER_KEY, 'Bearer ' + accessToken) })
      : req);
    const authAction = req.url.substring(apiBase.length).split('?')[0];
    if (['auth/signin', 'auth/signup', 'auth/reinitPassword', 'auth/changePassword',
      'auth/refresh', 'auth/signout'].includes(authAction)) {
      return next.handle(req);
    }

    const renewAndSend = () => this.auth.refreshSession().pipe(
      switchMap(() => send(this.token.getToken())),
      catchError(error => {
        if (error instanceof HttpErrorResponse
          && (error.status === 401 || (error.status === 400 && !this.token.getToken()))) {
          this.expireSession();
        }
        return throwError(() => error);
      })
    );

    if (this.auth.canRefreshSession() && this.token.isTokenExpired()) return renewAndSend();

    return send(token).pipe(catchError(error => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !token) {
        return throwError(() => error);
      }
      // Un autre appel peut avoir déjà renouvelé le JWT pendant cette requête.
      const currentToken = this.token.getToken();
      if (currentToken && currentToken !== token) return send(currentToken);
      if (this.auth.canRefreshSession()) return renewAndSend();
      this.expireSession();
      return throwError(() => error);
    }));
  }

  private expireSession(): void {
    this.token.signOut();
    void this.router.navigate(['/login'], { queryParams: { sessionExpiree: '1' } });
  }
}

// Pont de compatibilite pour les composants existants qui utilisent encore
// des proprietes classiques dans leurs callbacks subscribe().
export const apiChangeDetectionInterceptor: HttpInterceptorFn = (request, next) => {
  const apiRenderService = inject(ApiRenderService);
  return next(request).pipe(
    finalize(() => setTimeout(() => apiRenderService.notify()))
  );
};

export const authInterceptorProviders = [
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
];
