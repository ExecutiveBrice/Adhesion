import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';
import { shopAuthGuard } from './shop-auth.guard';

describe('shopAuthGuard', () => {
  let storage: jasmine.SpyObj<TokenStorageService>;
  let router: Router;

  beforeEach(() => {
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'isTokenExpired']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: TokenStorageService, useValue: storage }]
    });
    router = TestBed.inject(Router);
  });

  it('laisse entrer un utilisateur connecté', () => {
    storage.getToken.and.returnValue('token');
    storage.isTokenExpired.and.returnValue(false);

    expect(TestBed.runInInjectionContext(() => shopAuthGuard(
      {} as ActivatedRouteSnapshot, { url: '/boutique/panier' } as RouterStateSnapshot))).toBeTrue();
  });

  it('renvoie un visiteur vers la connexion avec sa destination', () => {
    storage.getToken.and.returnValue(null);

    const result = TestBed.runInInjectionContext(() => shopAuthGuard(
      {} as ActivatedRouteSnapshot, { url: '/boutique/produits/12' } as RouterStateSnapshot));

    expect(router.serializeUrl(result as ReturnType<Router['createUrlTree']>))
      .toBe('/login?returnUrl=%2Fboutique%2Fproduits%2F12');
  });
});
