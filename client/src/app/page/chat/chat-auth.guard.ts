import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../../_services/token-storage.service';
import { ParamService } from '../../_services/param.service';
import { catchError, map, of } from 'rxjs';

export const chatAuthGuard: CanActivateFn = (_route, state) => {
  const storage = inject(TokenStorageService);
  const router = inject(Router);
  if (!storage.getToken() || storage.isTokenExpired()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  return inject(ParamService).getAllBoolean().pipe(
    map(params => params.some(param => param.paramName === 'Show_Chat' && param.paramValue)
      ? true : router.createUrlTree(['/inscription'])),
    catchError(() => of(router.createUrlTree(['/inscription'])))
  );
};
