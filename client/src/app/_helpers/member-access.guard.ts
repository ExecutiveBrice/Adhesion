import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';

export const memberAccessGuard: CanActivateFn = (route, state) => {
  const storage = inject(TokenStorageService);
  const router = inject(Router);
  if (!storage.getToken() || storage.isTokenExpired()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  const required: string[] = route.data['roles'] ?? [];
  const roles: string[] = storage.getUser().roles ?? [];
  return !required.length || required.some(role => roles.includes(role))
    ? true : router.createUrlTree(['/profil']);
};
