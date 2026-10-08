import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { SectionManagementService } from '../_services/section-management.service';
import { TokenStorageService } from '../_services/token-storage.service';

export const sectionListingAccessGuard: CanActivateFn = (_route, state) => {
  const storage = inject(TokenStorageService);
  const router = inject(Router);
  if (!storage.getToken() || storage.isTokenExpired()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  const roles: string[] = storage.getUser().roles ?? [];
  if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_SECRETAIRE')) return true;
  return inject(SectionManagementService).sections().pipe(
    map(sections => sections.length > 0 ? true : router.createUrlTree(['/profil'])),
    catchError(() => of(router.createUrlTree(['/profil'])))
  );
};
