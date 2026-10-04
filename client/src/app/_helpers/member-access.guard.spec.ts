import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';
import { memberAccessGuard } from './member-access.guard';

describe('Accès aux pages du membre actif', () => {
  let roles: string[];
  let token: string | null;
  const check = (required: string[]) => TestBed.runInInjectionContext(() => memberAccessGuard(
    { data: { roles: required } } as unknown as ActivatedRouteSnapshot,
    { url: '/admin' } as RouterStateSnapshot
  ));

  beforeEach(() => {
    roles = ['ROLE_ADMIN'];
    token = 'jwt';
    TestBed.configureTestingModule({ providers: [provideRouter([]), {
      provide: TokenStorageService,
      useValue: { getToken: () => token, isTokenExpired: () => false, getUser: () => ({ roles }) }
    }] });
  });

  it('réévalue les droits après le changement de membre', () => {
    expect(check(['ROLE_ADMIN'])).toBeTrue();
    roles = ['ROLE_USER'];
    expect(TestBed.inject(Router).serializeUrl(check(['ROLE_ADMIN']) as any)).toBe('/profil');
    expect(check([])).toBeTrue();
  });

  it('redirige vers la connexion en l’absence de session', () => {
    token = null;
    expect(TestBed.inject(Router).serializeUrl(check([]) as any)).toBe('/login?returnUrl=%2Fadmin');
  });
});
