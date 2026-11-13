import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';

export const shopManagerGuard: CanActivateFn = () => {
  const storage = inject(TokenStorageService);
  const router = inject(Router);
  if (storage.getToken() && storage.getUser().roles?.includes('ROLE_RESPONSABLE_BOUTIQUE')) return true;
  return router.createUrlTree(['/boutique']);
};
