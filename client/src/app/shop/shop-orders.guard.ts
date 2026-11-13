import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { TokenStorageService } from '../_services/token-storage.service';
import { ShopApiService } from './services/shop-api.service';

export const shopOrdersGuard: CanActivateFn = (_route, state) => {
  const storage = inject(TokenStorageService);
  const router = inject(Router);
  if (!storage.getToken() || storage.isTokenExpired()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  const catalogue = router.createUrlTree(['/boutique']);
  return inject(ShopApiService).orders().pipe(
    map(orders => orders.length > 0 ? true : catalogue),
    catchError(() => of(catalogue))
  );
};
