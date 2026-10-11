import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';

export const materielGuard: CanActivateFn = () => {
  const storage = inject(TokenStorageService);
  const router = inject(Router);
  if (!storage.getToken()) return router.createUrlTree(['/login']);
  const roles: string[] = storage.getUser().roles ?? [];
  return roles.some(role => ['ROLE_ADMIN', 'ROLE_MEMBRECA', 'ROLE_LOGISTIQUE'].includes(role))
    || router.createUrlTree(['/inscription']);
};
