import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ParamService } from '../../_services/param.service';
import { SectionManagementService } from '../../_services/section-management.service';
import { catchError, map, of, switchMap } from 'rxjs';
import { ERole } from '../../models';

export const chatAuthGuard: CanActivateFn = (_route, state) => {
  const storage = inject(TokenStorageService);
  const router = inject(Router);
  if (!storage.getToken() || storage.isTokenExpired()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  if (storage.getUser().roles?.includes(ERole.ROLE_ADMIN)) return true;
  const sectionManagement = inject(SectionManagementService);
  const denied = router.createUrlTree(['/profil']);
  return inject(ParamService).getAllBoolean().pipe(
    catchError(() => of([])),
    switchMap(params => params.some(param => param.paramName === 'Show_Chat' && param.paramValue)
      ? of(true)
      : sectionManagement.sections().pipe(
        map(sections => sections.length > 0 ? true : denied),
        catchError(() => of(denied))
      ))
  );
};
