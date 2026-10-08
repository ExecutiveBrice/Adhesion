import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';
import { memberAccessGuard } from './member-access.guard';
import { routes } from '../app.routes';

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

  it('réserve la route publicités à la communication globale, même pour un administrateur', () => {
    const route = routes.find(candidate => candidate.path === 'publicites')!;
    expect(route.canActivate).toContain(memberAccessGuard);
    const required = route.data!['roles'];
    for (const role of ['ROLE_ADMIN', 'ROLE_USER', 'ROLE_COMPTABLE']) {
      roles = [role];
      expect(TestBed.inject(Router).serializeUrl(check(required) as any)).toBe('/profil');
    }
    roles = ['ROLE_COMMUNICATION_GLOBAL'];
    expect(check(required)).toBeTrue();
  });
});
