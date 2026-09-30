import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ShopOrderDto } from '../models/shop.models';
import { CartStore } from '../services/cart.store';
import { ShopApiService } from '../services/shop-api.service';
import { ShopCartLinkComponent } from './shop-cart-link.component';

describe('ShopCartLinkComponent', () => {
  let fixture: ComponentFixture<ShopCartLinkComponent>;
  const itemCount = signal(0);
  const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['orders']);

  function create(link: 'cart' | 'shop' | 'orders'): void {
    TestBed.configureTestingModule({
      imports: [ShopCartLinkComponent],
      providers: [
        provideRouter([]),
        { provide: CartStore, useValue: { itemCount } },
        { provide: ShopApiService, useValue: api }
      ]
    });
    fixture = TestBed.createComponent(ShopCartLinkComponent);
    fixture.componentRef.setInput('link', link);
    fixture.detectChanges();
  }

  beforeEach(() => {
    itemCount.set(0);
    api.orders.calls.reset();
  });

  it('affiche l’alerte lorsqu’une commande attend son paiement', () => {
    api.orders.and.returnValue(of([{ status: 'PENDING_PAYMENT' } as ShopOrderDto]));
    create('orders');

    expect(fixture.nativeElement.querySelector('.shop-action-alert')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a').getAttribute('aria-label')).toContain('action est en attente');
  });

  it('affiche l’alerte lorsqu’un panier contient des articles', () => {
    api.orders.and.returnValue(of([]));
    itemCount.set(2);
    create('cart');

    expect(fixture.nativeElement.querySelector('.shop-action-alert')).not.toBeNull();
  });

  it('conserve l’alerte globale de la boutique pour une commande à régler', () => {
    api.orders.and.returnValue(of([{ status: 'PENDING_PAYMENT' } as ShopOrderDto]));
    create('shop');

    expect(fixture.nativeElement.querySelector('.shop-action-alert')).not.toBeNull();
  });

  it('n’affiche pas d’alerte en l’absence d’action à effectuer', () => {
    api.orders.and.returnValue(of([]));
    create('orders');

    expect(fixture.nativeElement.querySelector('.shop-action-alert')).toBeNull();
  });
});
