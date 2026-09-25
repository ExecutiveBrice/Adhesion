import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';

export const shopAuthGuard: CanActivateFn = (_route, state) => {
  const storage = inject(TokenStorageService);
  if (storage.getToken() && !storage.isTokenExpired()) return true;
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
