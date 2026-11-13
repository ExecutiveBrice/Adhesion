import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';
import { shopAuthGuard } from './shop-auth.guard';
import { Component } from '@angular/core';
import { routes } from '../app.routes';

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
      {} as ActivatedRouteSnapshot, { url: '/boutique/checkout' } as RouterStateSnapshot));

    expect(router.serializeUrl(result as ReturnType<Router['createUrlTree']>))
      .toBe('/login?returnUrl=%2Fboutique%2Fcheckout');
  });
});

@Component({ template: '' })
class ShopRouteTestPage {}

describe('Navigation boutique sans connexion', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes.map(route => ({ ...route, loadComponent: undefined, component: ShopRouteTestPage }))),
        { provide: TokenStorageService, useValue: { getToken: () => null, isTokenExpired: () => true } }
      ]
    });
  });

  it('ouvre le catalogue, les produits et le panier puis demande la connexion au checkout', async () => {
    const router = TestBed.inject(Router);
    for (const url of ['/boutique', '/boutique/produits/12', '/boutique/panier']) {
      await router.navigateByUrl(url);
      expect(router.url).toBe(url);
    }
    await router.navigateByUrl('/boutique/checkout');
    expect(router.url).toBe('/login?returnUrl=%2Fboutique%2Fcheckout');
  });
});
