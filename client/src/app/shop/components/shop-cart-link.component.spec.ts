import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ShopOrderDto } from '../models/shop.models';
import { CartStore } from '../services/cart.store';
import { ShopApiService } from '../services/shop-api.service';
import { ShopCartLinkComponent } from './shop-cart-link.component';
import { TokenStorageService } from '../../_services/token-storage.service';

describe('ShopCartLinkComponent', () => {
  let fixture: ComponentFixture<ShopCartLinkComponent>;
  const itemCount = signal(0);
  const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['orders']);
  const storage = jasmine.createSpyObj<TokenStorageService>('TokenStorageService', ['getToken', 'isTokenExpired']);

  function create(link: 'cart' | 'shop' | 'orders'): void {
    TestBed.configureTestingModule({
      imports: [ShopCartLinkComponent],
      providers: [
        provideRouter([]),
        { provide: CartStore, useValue: { itemCount } },
        { provide: ShopApiService, useValue: api },
        { provide: TokenStorageService, useValue: storage }
      ]
    });
    fixture = TestBed.createComponent(ShopCartLinkComponent);
    fixture.componentRef.setInput('link', link);
    fixture.detectChanges();
  }

  beforeEach(() => {
    itemCount.set(0);
    api.orders.calls.reset();
    storage.getToken.and.returnValue('token');
    storage.isTokenExpired.and.returnValue(false);
  });

  it('permet d’accéder à la boutique sans demander les commandes d’un visiteur', () => {
    storage.getToken.and.returnValue(null);
    create('shop');

    expect(api.orders).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/boutique');
  });

  it('compte uniquement les commandes en attente de paiement', () => {
    api.orders.and.returnValue(of([
      { status: 'PENDING_PAYMENT' } as ShopOrderDto,
      { status: 'PAID' } as ShopOrderDto,
      { status: 'PENDING_PAYMENT' } as ShopOrderDto
    ]));
    create('orders');

    expect(fixture.nativeElement.querySelector('.shop-cart-badge').textContent.trim()).toBe('2');
    expect(fixture.nativeElement.querySelector('a').getAttribute('aria-label')).toBe('Mes commandes : 2 commandes en attente');
    expect(fixture.nativeElement.querySelector('.shop-action-alert')).toBeNull();
  });

  it('affiche le compteur sans alerte lorsqu’un panier contient des articles', () => {
    api.orders.and.returnValue(of([]));
    itemCount.set(2);
    create('cart');

    expect(fixture.nativeElement.querySelector('.shop-cart-badge').textContent.trim()).toBe('2');
    expect(fixture.nativeElement.querySelector('.shop-action-alert')).toBeNull();
  });

  it('affiche le compteur des commandes à régler sur le bouton Boutique du menu', () => {
    api.orders.and.returnValue(of([{ status: 'PENDING_PAYMENT' } as ShopOrderDto]));
    itemCount.set(3);
    create('shop');
    fixture.componentRef.setInput('navigation', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.shop-action-alert')).toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.shop-cart-badge').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.shop-cart-badge').textContent.trim()).toBe('1');
    expect(fixture.nativeElement.querySelector('a').getAttribute('aria-label')).toBe('Boutique : 1 commande en attente');
  });

  it('n’affiche pas d’alerte en l’absence d’action à effectuer', () => {
    api.orders.and.returnValue(of([]));
    create('orders');

    expect(fixture.nativeElement.querySelector('.shop-action-alert')).toBeNull();
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
  });

  it('masque Mes commandes tant que l’historique n’est pas chargé', () => {
    const orders = new Subject<ShopOrderDto[]>();
    api.orders.and.returnValue(orders);
    create('orders');
    expect(fixture.nativeElement.querySelector('a')).toBeNull();

    orders.next([{ status: 'CANCELLED' } as ShopOrderDto]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/boutique/commandes');
    expect(fixture.nativeElement.querySelector('.shop-cart-badge')).toBeNull();
    expect(fixture.nativeElement.querySelector('.shop-action-alert')).toBeNull();
  });

  it('masque Mes commandes pour un visiteur ou une session expirée', () => {
    storage.isTokenExpired.and.returnValue(true);
    create('orders');
    expect(api.orders).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
  });

  it('masque Mes commandes si l’historique est indisponible', () => {
    api.orders.and.returnValue(throwError(() => new Error('unavailable')));
    create('orders');
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
  });
});
