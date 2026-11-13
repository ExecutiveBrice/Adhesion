import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { routes } from '../app.routes';
import { TokenStorageService } from '../_services/token-storage.service';
import { ShopOrderDto } from './models/shop.models';
import { ShopApiService } from './services/shop-api.service';

@Component({ template: '' })
class OrdersRouteTestPage {}

describe('Accès à Mes commandes', () => {
  let api: jasmine.SpyObj<ShopApiService>;
  let storage: jasmine.SpyObj<TokenStorageService>;
  let router: Router;

  beforeEach(() => {
    api = jasmine.createSpyObj('ShopApiService', ['orders']);
    api.orders.and.returnValue(of([]));
    storage = jasmine.createSpyObj('TokenStorageService', ['getToken', 'isTokenExpired']);
    storage.getToken.and.returnValue('token');
    storage.isTokenExpired.and.returnValue(false);
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes.map(route => ({ ...route, loadComponent: undefined, component: OrdersRouteTestPage }))),
        { provide: ShopApiService, useValue: api },
        { provide: TokenStorageService, useValue: storage }
      ]
    });
    router = TestBed.inject(Router);
  });

  it('redirige un adhérent sans commande vers le catalogue même par URL directe', async () => {
    await router.navigateByUrl('/boutique/commandes');
    expect(router.url).toBe('/boutique');
  });

  for (const status of ['PENDING_PAYMENT', 'PAID', 'COMPLETED', 'CANCELLED', 'EXPIRED'] as ShopOrderDto['status'][]) {
    it(`autorise un adhérent ayant une commande ${status}`, async () => {
      api.orders.and.returnValue(of([{ status } as ShopOrderDto]));
      await router.navigateByUrl('/boutique/commandes');
      expect(router.url).toBe('/boutique/commandes');
    });
  }

  it('redirige vers le catalogue si l’historique ne peut pas être vérifié', async () => {
    api.orders.and.returnValue(throwError(() => new Error('unavailable')));
    await router.navigateByUrl('/boutique/commandes');
    expect(router.url).toBe('/boutique');
  });

  for (const expired of [false, true]) {
    it(`demande la connexion pour une session ${expired ? 'expirée' : 'absente'}`, async () => {
      storage.getToken.and.returnValue(expired ? 'token' : null);
      storage.isTokenExpired.and.returnValue(expired);
      await router.navigateByUrl('/boutique/commandes');
      expect(router.url).toBe('/login?returnUrl=%2Fboutique%2Fcommandes');
      expect(api.orders).not.toHaveBeenCalled();
    });
  }
});
