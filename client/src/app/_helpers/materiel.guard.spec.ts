import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { materielGuard } from './materiel.guard';
import { TokenStorageService } from '../_services/token-storage.service';

describe('Accès à la page matériel', () => {
  let storage: jasmine.SpyObj<TokenStorageService>;
  beforeEach(() => {
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'getUser']);
    storage.getToken.and.returnValue('token');
    storage.getUser.and.returnValue({ roles: [] });
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: TokenStorageService, useValue: storage }] });
  });
  const verifier = () => TestBed.runInInjectionContext(() => materielGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

  it('redirige vers la connexion en l’absence de session', () => {
    storage.getToken.and.returnValue(null);
    expect(TestBed.inject(Router).serializeUrl(verifier() as UrlTree)).toBe('/login');
  });
  it('autorise le CA, l’administrateur et la Logistique et refuse les autres rôles', () => {
    for (const role of ['ROLE_MEMBRECA', 'ROLE_ADMIN', 'ROLE_LOGISTIQUE']) {
      storage.getUser.and.returnValue({ roles: [role] });
      expect(verifier()).toBeTrue();
    }
    for (const role of ['ROLE_USER', 'ROLE_BUREAU', 'ROLE_SECRETAIRE']) {
      storage.getUser.and.returnValue({ roles: [role] });
      expect(TestBed.inject(Router).serializeUrl(verifier() as UrlTree)).toBe('/inscription');
    }
  });
});
