import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';
import { SectionManagementService } from '../_services/section-management.service';
import { TokenStorageService } from '../_services/token-storage.service';
import { sectionListingAccessGuard } from './section-listing-access.guard';

describe('Accès aux listes des référents de section', () => {
  let roles: string[];
  let managedSections: { id: number; nom: string; couleur: string }[];
  const check = () => TestBed.runInInjectionContext(() => sectionListingAccessGuard(
    {} as ActivatedRouteSnapshot, { url: '/adherents' } as RouterStateSnapshot
  ));

  beforeEach(() => {
    roles = ['ROLE_USER'];
    managedSections = [];
    TestBed.configureTestingModule({ providers: [provideRouter([]), {
      provide: TokenStorageService,
      useValue: { getToken: () => 'jwt', isTokenExpired: () => false, getUser: () => ({ roles }) }
    }, {
      provide: SectionManagementService,
      useValue: { sections: () => of(managedSections) }
    }] });
  });

  it('ouvre les listes aux comptes affectés à une section', async () => {
    managedSections = [{ id: 4, nom: 'Basket', couleur: '#FFFFFF' }];
    expect(await firstValueFrom(check() as any)).toBeTrue();
  });

  it('refuse les comptes sans section affectée', async () => {
    const result = await firstValueFrom(check() as any);
    expect(TestBed.inject(Router).serializeUrl(result as any)).toBe('/profil');
  });

  it('préserve l’accès global de l’administration', () => {
    roles = ['ROLE_ADMIN'];
    expect(check()).toBeTrue();
  });
});
