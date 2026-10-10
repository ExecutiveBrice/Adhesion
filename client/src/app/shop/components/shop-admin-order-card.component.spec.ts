import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ShopAdminOrderDto } from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { ShopAdminOrderCardComponent } from './shop-admin-order-card.component';

describe('ShopAdminOrderCardComponent', () => {
  const order: ShopAdminOrderDto = {
    orderNumber: 'CMD-2026-000001', status: 'PAID',
    total: { amountInCents: 3000, currency: 'EUR' }, createdAt: '2026-09-25T10:00:00Z',
    customerEmail: 'client@example.test', customerTribeId: 17, refundRequested: false,
    items: [{ id: 7, productVariantId: 42, productName: 'Tee-shirt', variantName: 'M', sku: 'TS-M',
      unitPrice: { amountInCents: 1500, currency: 'EUR' }, quantity: 2,
      lineTotal: { amountInCents: 3000, currency: 'EUR' }, status: 'PENDING' }]
  };

  beforeAll(() => registerLocaleData(localeFr));

  it('affiche la disponibilité par article et globalement, et exige le paiement avant la livraison', () => {
    TestBed.configureTestingModule({ imports: [ShopAdminOrderCardComponent],
      providers: [{ provide: ShopAdminApiService, useValue: {} }] });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    const component = fixture.componentInstance;
    component.order = structuredClone(order);
    component.statusLabel = 'Payée';
    component.expanded = true;
    component.availability = new Map([[7, { state: 'missing', missingQuantity: 1, label: 'Manque 1 sur 2' }]]);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.order-toggle .delivery-indicator').textContent).toContain('1 article manquant');
    expect(fixture.nativeElement.querySelector('.item-availability').textContent).toContain('Manque 1 sur 2');
    fixture.componentRef.setInput('availability', new Map([[7, { state: 'ready', missingQuantity: 0, label: 'Disponible : 2 / 2' }]]));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.order-toggle .delivery-indicator').textContent).toContain('Peut être livrée');
    component.order.status = 'PENDING_PAYMENT';
    expect(component.deliveryAvailability.label).toBe('En attente de paiement');
    component.order.items[0].status = 'COMPLETED';
    component.order.status = 'COMPLETED';
    expect(component.deliveryAvailability.label).toBe('Commande livrée');
    expect(component.itemAvailability(component.order.items[0]).label).toBe('Article livré');
  });

  it('vérifie le paiement, évite les doubles clics et actualise la commande', () => {
    const result = new Subject<ShopAdminOrderDto>();
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['verifyPayment']);
    api.verifyPayment.and.returnValue(result);
    TestBed.configureTestingModule({
      imports: [ShopAdminOrderCardComponent], providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    fixture.componentInstance.order = { ...structuredClone(order), status: 'PENDING_PAYMENT' };
    fixture.componentInstance.statusLabel = 'En attente de paiement';
    fixture.componentInstance.expanded = true;
    const changed = jasmine.createSpy('changed');
    fixture.componentInstance.statusChanged.subscribe(changed);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.verify-payment').click();
    fixture.componentInstance.verifyPayment();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.verify-payment').disabled).toBeTrue();
    expect(fixture.nativeElement.querySelector('.order-status-control select').disabled).toBeTrue();
    expect(api.verifyPayment).toHaveBeenCalledOnceWith(order.orderNumber);

    result.next(structuredClone(order));
    result.complete();
    fixture.detectChanges();
    expect(fixture.componentInstance.order.status).toBe('PAID');
    expect(fixture.nativeElement.querySelector('.verify-payment')).toBeNull();
    expect(changed).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('le statut de la commande est à jour');
  });

  it('distingue un paiement encore en attente d’une erreur de vérification', () => {
    const pending = { ...structuredClone(order), status: 'PENDING_PAYMENT' as const };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['verifyPayment']);
    api.verifyPayment.and.returnValue(of(pending));
    TestBed.configureTestingModule({
      imports: [ShopAdminOrderCardComponent], providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    fixture.componentInstance.order = pending;
    fixture.componentInstance.statusLabel = 'En attente de paiement';
    fixture.componentInstance.expanded = true;
    fixture.componentInstance.verifyPayment();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('le paiement n’est pas encore confirmé');

    api.verifyPayment.and.returnValue(throwError(() => ({ error: { detail: 'HelloAsso indisponible' } })));
    fixture.componentInstance.verifyPayment();
    fixture.detectChanges();
    expect(fixture.componentInstance.order.status).toBe('PENDING_PAYMENT');
    expect(fixture.nativeElement.textContent).toContain('HelloAsso indisponible');
    expect(fixture.nativeElement.querySelector('.verify-payment').disabled).toBeFalse();
    expect(fixture.componentInstance.paymentVerificationMessage).toBe('');
  });

  it('déplie les articles et enregistre leur statut', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderItemStatus']);
    api.updateOrderItemStatus.and.returnValue(of({ ...order, status: 'PROCESSING', items: [{ ...order.items[0], status: 'PROCESSING' }] }));
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
    expect(fixture.componentInstance.order.status).toBe('PROCESSING');
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

  it('ne propose pas l’annulation d’un article payé sans remboursement', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderItemStatus']);
    TestBed.configureTestingModule({
      imports: [ShopAdminOrderCardComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopAdminOrderCardComponent);
    fixture.componentInstance.order = structuredClone(order);
    fixture.componentInstance.statusLabel = 'Payée';
    fixture.componentInstance.expanded = true;
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('.item-status-control select') as HTMLSelectElement;
    expect(Array.from(select.options).map(option => option.value)).toEqual(['PENDING', 'PROCESSING', 'COMPLETED']);
    expect(fixture.nativeElement.textContent).toContain('traitement du remboursement');
  });

  it('enregistre le statut global et actualise celui des articles', () => {
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderStatus']);
    api.updateOrderStatus.and.returnValue(of({ ...order, status: 'PROCESSING', items: [{ ...order.items[0], status: 'PROCESSING' }] }));
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
    expect(fixture.componentInstance.order.items[0].status).toBe('PROCESSING');
    expect(changed).toHaveBeenCalledTimes(1);
  });

  it('annule une commande en attente de paiement et ses articles', () => {
    const pendingOrder: ShopAdminOrderDto = { ...structuredClone(order), status: 'PENDING_PAYMENT' };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['updateOrderStatus']);
    api.updateOrderStatus.and.returnValue(of({ ...pendingOrder, status: 'CANCELLED', items: [{ ...pendingOrder.items[0], status: 'CANCELLED' }] }));
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
    expect(fixture.componentInstance.order.items[0].status).toBe('CANCELLED');
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
