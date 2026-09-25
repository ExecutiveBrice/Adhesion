import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ShopAdminOrderDto } from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { ShopAdminOrderCardComponent } from './shop-admin-order-card.component';

describe('ShopAdminOrderCardComponent', () => {
  const order: ShopAdminOrderDto = {
    orderNumber: 'CMD-2026-000001', status: 'PAID',
    total: { amountInCents: 3000, currency: 'EUR' }, createdAt: '2026-09-25T10:00:00Z',
    customerEmail: 'client@example.test', customerTribeId: 17,
    items: [{ id: 7, productName: 'Tee-shirt', variantName: 'M', sku: 'TS-M',
      unitPrice: { amountInCents: 1500, currency: 'EUR' }, quantity: 2,
      lineTotal: { amountInCents: 3000, currency: 'EUR' }, status: 'PENDING' }]
  };

  beforeAll(() => registerLocaleData(localeFr));

  it('déplie les articles et enregistre leur statut', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderItemStatus']);
    api.updateOrderItemStatus.and.returnValue(of({ ...order.items[0], status: 'PROCESSING' }));
    TestBed.configureTestingModule({
      imports: [ShopAdminOrderCardComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    fixture.componentInstance.order = structuredClone(order);
    fixture.componentInstance.statusLabel = 'Payée';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.customer-email')?.textContent).toContain('client@example.test');
    expect(fixture.nativeElement.querySelector('.customer-tribe')?.textContent).toContain('Tribu 17');
    expect(fixture.nativeElement.querySelector('.order-customer button')?.textContent).toContain('Copier');
    const toggle = fixture.nativeElement.querySelector('.order-toggle') as HTMLButtonElement;
    expect(fixture.nativeElement.querySelector('.order-item')).toBeNull();
    toggle.click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.querySelector('.item-description')?.textContent).toContain('Tee-shirt');

    const select = fixture.nativeElement.querySelector('.item-status-control select') as HTMLSelectElement;
    select.value = 'PROCESSING';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(api.updateOrderItemStatus).toHaveBeenCalledWith('CMD-2026-000001', 7, 'PROCESSING');
    expect(fixture.componentInstance.order.items[0].status).toBe('PROCESSING');
    expect(fixture.nativeElement.textContent).toContain('Statut enregistré');
  });

  it('rétablit le statut affiché si l’enregistrement échoue', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderItemStatus']);
    api.updateOrderItemStatus.and.returnValue(throwError(() => new Error('échec')));
    TestBed.configureTestingModule({
      imports: [ShopAdminOrderCardComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    fixture.componentInstance.order = structuredClone(order);
    fixture.componentInstance.statusLabel = 'Payée';
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.order-toggle').click();
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('.item-status-control select') as HTMLSelectElement;
    select.value = 'COMPLETED';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(select.value).toBe('PENDING');
    expect(fixture.nativeElement.textContent).toContain('Le statut n’a pas pu être enregistré');
  });

  it('enregistre le statut global sans changer celui des articles', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderStatus']);
    api.updateOrderStatus.and.returnValue(of({ ...order, status: 'PROCESSING' }));
    TestBed.configureTestingModule({
      imports: [ShopAdminOrderCardComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    fixture.componentInstance.order = structuredClone(order);
    fixture.componentInstance.statusLabel = 'Payée';
    const changed = jasmine.createSpy('changed');
    fixture.componentInstance.statusChanged.subscribe(changed);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.order-toggle').click();
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('.order-status-control select') as HTMLSelectElement;
    select.value = 'PROCESSING';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(api.updateOrderStatus).toHaveBeenCalledWith('CMD-2026-000001', 'PROCESSING');
    expect(fixture.componentInstance.order.status).toBe('PROCESSING');
    expect(fixture.componentInstance.order.items[0].status).toBe('PENDING');
    expect(changed).toHaveBeenCalledTimes(1);
  });

  it('annule une commande en attente de paiement sans changer le statut de ses articles', () => {
    const pendingOrder: ShopAdminOrderDto = { ...structuredClone(order), status: 'PENDING_PAYMENT' };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderStatus']);
    api.updateOrderStatus.and.returnValue(of({ ...pendingOrder, status: 'CANCELLED' }));
    TestBed.configureTestingModule({
      imports: [ShopAdminOrderCardComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    fixture.componentInstance.order = pendingOrder;
    fixture.componentInstance.statusLabel = 'En attente de paiement';
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.order-toggle').click();
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('.order-status-control select') as HTMLSelectElement;
    expect(Array.from(select.options).map(option => option.value)).toEqual(['PENDING_PAYMENT', 'CANCELLED']);
    select.value = 'CANCELLED';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(api.updateOrderStatus).toHaveBeenCalledWith('CMD-2026-000001', 'CANCELLED');
    expect(fixture.componentInstance.order.status).toBe('CANCELLED');
    expect(fixture.componentInstance.order.items[0].status).toBe('PENDING');
  });

  it('rétablit le statut global si son enregistrement échoue', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderStatus']);
    api.updateOrderStatus.and.returnValue(throwError(() => new Error('échec')));
    TestBed.configureTestingModule({
      imports: [ShopAdminOrderCardComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    fixture.componentInstance.order = structuredClone(order);
    fixture.componentInstance.statusLabel = 'Payée';
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.order-toggle').click();
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('.order-status-control select') as HTMLSelectElement;
    select.value = 'PROCESSING';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(select.value).toBe('PAID');
    expect(fixture.componentInstance.order.items[0].status).toBe('PENDING');
    expect(fixture.nativeElement.textContent).toContain('Le statut de la commande n’a pas pu être enregistré');
  });
});
