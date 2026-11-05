import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';
import { sectionListingAccessGuard } from './section-listing-access.guard';

export const activitesAccessGuard: CanActivateFn = (route, state) => {
  const storage = inject(TokenStorageService);
  if (storage.getToken() && !storage.isTokenExpired()
      && storage.getUser().roles?.includes('ROLE_ENCADRANT')) return true;
  return sectionListingAccessGuard(route, state);
};
