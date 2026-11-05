import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { SectionManagementService } from '../_services/section-management.service';
import { TokenStorageService } from '../_services/token-storage.service';
import { activitesAccessGuard } from './activites-access.guard';
import { sectionListingAccessGuard } from './section-listing-access.guard';

describe('Accès des encadrants à la liste des activités', () => {
  let roles: string[];
  let expired: boolean;
  let sections: { id: number; nom: string; couleur: string }[];
  const check = (guard = activitesAccessGuard) => TestBed.runInInjectionContext(() => guard(
    {} as ActivatedRouteSnapshot, { url: '/activites' } as RouterStateSnapshot
  ));

  beforeEach(() => {
    roles = ['ROLE_ENCADRANT']; expired = false; sections = [];
    TestBed.configureTestingModule({ providers: [provideRouter([]), {
      provide: TokenStorageService,
      useValue: { getToken: () => 'jwt', isTokenExpired: () => expired, getUser: () => ({ roles }) }
    }, {
      provide: SectionManagementService, useValue: { sections: () => of(sections) }
    }] });
  });

  it('autorise un encadrant sans section confiée', () => expect(check()).toBeTrue());

  it('refuse un adhérent sans section confiée', async () => {
    roles = ['ROLE_USER'];
    const result = await firstValueFrom(check() as any);
    expect(TestBed.inject(Router).serializeUrl(result as any)).toBe('/profil');
  });

  it('conserve les accès des responsables de section et du secrétariat', async () => {
    roles = ['ROLE_USER']; sections = [{ id: 3, nom: 'Culture', couleur: '#000000' }];
    expect(await firstValueFrom(check() as any)).toBeTrue();
    roles = ['ROLE_SECRETAIRE']; expect(check()).toBeTrue();
  });

  it('renvoie un encadrant dont le jeton a expiré vers la connexion', () => {
    expired = true;
    expect(TestBed.inject(Router).serializeUrl(check() as any)).toBe('/login?returnUrl=%2Factivites');
  });

  it('ne donne pas aux encadrants les droits de gestion des sections', async () => {
    const result = await firstValueFrom(check(sectionListingAccessGuard) as any);
    expect(TestBed.inject(Router).serializeUrl(result as any)).toBe('/profil');
  });
});
