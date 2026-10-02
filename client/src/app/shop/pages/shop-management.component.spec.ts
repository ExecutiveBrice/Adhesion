import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ShopAdminOrderDto, ShopAdminProductDto } from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { ShopManagementComponent } from './shop-management.component';

describe('ShopManagementComponent', () => {
  beforeAll(() => registerLocaleData(localeFr));

  it('ouvre le tableau de stock et charge le catalogue à la demande', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'categories']);
    api.products.and.returnValue(of([]));
    api.orders.and.returnValue(of([]));
    api.categories.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [ShopManagementComponent],
      providers: [provideRouter([]), { provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopManagementComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#stocks-tab')?.getAttribute('aria-selected')).toBe('true');
    expect(fixture.nativeElement.querySelector('#stock-overview-title')?.textContent).toContain('État des stocks');
    expect(api.products).toHaveBeenCalledTimes(1);
    expect(api.categories).not.toHaveBeenCalled();

    (fixture.nativeElement.querySelector('#catalogue-tab') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#catalogue-tab')?.getAttribute('aria-selected')).toBe('true');
    expect(api.categories).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('crée une variante sans choix de quantité ni de suivi du stock', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 41, sku: 'TS-S', label: 'S', priceAmountInCents: 1500, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: 0, stockReserved: 0, version: 0
      }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService',
      ['products', 'orders', 'categories', 'createVariant']);
    api.products.and.returnValue(of([product]));
    api.orders.and.returnValue(of([]));
    api.categories.and.returnValue(of([]));
    api.createVariant.and.returnValue(of({
      id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1500, currency: 'EUR',
      active: true, displayOrder: 0, stockTracked: true, stockOnHand: 0, stockReserved: 0, version: 0
    }));
    TestBed.configureTestingModule({
      imports: [ShopManagementComponent],
      providers: [provideRouter([]), { provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopManagementComponent);
    fixture.detectChanges();
    fixture.componentInstance.selectTab('catalogue');
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('.new-variant') as HTMLFormElement;
    expect(form.querySelector('input[name^="newVariantStock"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('input[name^="variantStock"]')).toBeNull();
    expect(form.textContent).toContain('Stock initial : 0');
    Object.assign(fixture.componentInstance.draftFor(product.id), {
      sku: ' TS-M ', label: ' M ', priceAmountInCents: 1500
    });
    fixture.componentInstance.createVariant(product);

    expect(api.createVariant).toHaveBeenCalledWith(1, {
      sku: 'TS-M', label: 'M', priceAmountInCents: 1500,
      currency: 'EUR', active: true, displayOrder: 0
    });
    fixture.destroy();
  });

  it('sépare les commandes actives des commandes clôturées', () => {
    const statuses: ShopAdminOrderDto['status'][] = [
      'DRAFT', 'PENDING_PAYMENT', 'PAID', 'PROCESSING',
      'COMPLETED', 'CANCELLED', 'EXPIRED', 'REFUNDED'
    ];
    const orders = statuses.map((status, index) => ({
      orderNumber: `CMD-${index}`, status,
      total: { amountInCents: 1000, currency: 'EUR' }, createdAt: '2026-09-25T10:00:00Z',
      customerEmail: 'client@example.test', customerTribeId: 17, refundRequested: false, items: []
    }));
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['orders', 'products']);
    api.orders.and.returnValue(of(orders));
    api.products.and.returnValue(of([]));

    TestBed.configureTestingModule({
      imports: [ShopManagementComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    TestBed.overrideComponent(ShopManagementComponent, { set: { template: '' } });

    const fixture = TestBed.createComponent(ShopManagementComponent);
    fixture.componentInstance.selectTab('orders');

    expect(fixture.componentInstance.ordersInProgress.map(order => order.status))
      .toEqual(statuses.slice(0, 4));
    expect(fixture.componentInstance.completedOrders.map(order => order.status))
      .toEqual(statuses.slice(4));
    fixture.componentInstance.ordersInProgress[3].status = 'COMPLETED';
    fixture.componentInstance.reclassifyOrders();
    expect(fixture.componentInstance.ordersInProgress.length).toBe(3);
    expect(fixture.componentInstance.completedOrders.length).toBe(5);
    expect(api.orders).toHaveBeenCalledTimes(1);
    fixture.componentInstance.selectTab('stocks');
    fixture.componentInstance.selectTab('orders');
    expect(api.orders).toHaveBeenCalledTimes(2);

    fixture.destroy();
  });
});
