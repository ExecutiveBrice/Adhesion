import { HTTP_INTERCEPTORS, HttpErrorResponse, HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { HttpInterceptor, HttpHandler, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';

import { TokenStorageService } from '../_services/token-storage.service';
import { catchError, finalize, Observable, throwError } from 'rxjs';
import { ApiRenderService } from '../_services/api-render.service';

// const TOKEN_HEADER_KEY = 'Authorization';       // for Spring Boot back-end
const TOKEN_HEADER_KEY = 'Authorization';   // for Node.js Express back-end

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private token = inject(TokenStorageService);
  private router = inject(Router);


  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let authReq = req;
    const token = this.token.getToken();
    if (token != null) {
      // for Spring Boot back-end
      // authReq = req.clone({ headers: req.headers.set(TOKEN_HEADER_KEY, 'Bearer ' + token) });

      // for Node.js Express back-end
      authReq = req.clone({ headers: req.headers.set(TOKEN_HEADER_KEY,  'Bearer ' + token) });
    }
    return next.handle(authReq).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse
          && error.status === 401
          && this.token.getToken() != null) {
          this.token.signOut();
          void this.router.navigate(['/login'], {
            queryParams: { sessionExpiree: '1' }
          });
        }
        return throwError(() => error);
      })
    );
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
