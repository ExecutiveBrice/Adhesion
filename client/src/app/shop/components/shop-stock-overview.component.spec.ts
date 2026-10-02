import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ShopAdminOrderDto, ShopAdminProductDto, ShopSupplierOrderDto } from '../models/shop.models';
import { ShopAdminApiService } from '../services/shop-admin-api.service';
import { buildStockRows, ShopStockOverviewComponent } from './shop-stock-overview.component';

describe('buildStockRows', () => {
  const product: ShopAdminProductDto = {
    id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
    active: true, displayOrder: 0, categories: [], variants: [{
      id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1200, currency: 'EUR',
      active: true, displayOrder: 0, stockTracked: true, stockOnHand: 7, stockReserved: 2, version: 0
    }]
  };
  const item = (id: number, quantity: number, status: 'PENDING' | 'COMPLETED' = 'PENDING') => ({
    id, productVariantId: 42, productName: 'T-shirt', variantName: 'M', sku: 'TS-M',
    unitPrice: { amountInCents: 1200, currency: 'EUR' }, quantity,
    lineTotal: { amountInCents: 1200 * quantity, currency: 'EUR' }, status
  });
  const order = (status: ShopAdminOrderDto['status'], createdAt: string,
                 items: ShopAdminOrderDto['items']): ShopAdminOrderDto => ({
    orderNumber: `${status}-${createdAt}`, status, createdAt, items,
    total: { amountInCents: 1200, currency: 'EUR' }, customerEmail: null,
    customerTribeId: null, refundRequested: false
  });

  it('ne retire pas une seconde fois les commandes réglées du disponible', () => {
    const now = Date.parse('2026-09-30T12:00:00Z');
    const rows = buildStockRows([product], [
      order('PENDING_PAYMENT', '2026-09-29T12:00:00Z', [item(1, 2)]),
      order('PAID', '2026-09-28T12:00:00Z', [item(2, 3)]),
      order('PROCESSING', '2026-09-27T12:00:00Z', [item(3, 1, 'COMPLETED')]),
      order('REFUNDED', '2026-09-26T12:00:00Z', [item(4, 1)]),
      order('CANCELLED', '2026-09-25T12:00:00Z', [item(5, 5)]),
      order('COMPLETED', '2026-08-01T12:00:00Z', [item(6, 4)])
    ], now);

    expect(rows.length).toBe(1);
    expect(rows[0].available).toBe(5); // 7 en stock - 2 réservés ; les 3 réglés sont déjà sortis.
    expect(rows[0].toPrepare).toBe(3); // Ligne payée non terminée uniquement.
    expect(rows[0].customerOrders).toBe(5); // Payées et en attente de paiement.
    expect(rows[0].outflow30Days).toBe(5); // Réglées récentes, remboursement inclus.
    expect(rows[0].readyToHandOver).toBe(3);
    expect(rows[0].toOrder).toBe(0);
    expect(rows[0].status).toBe('ready');
  });

  it('ne confond pas un solde nul avec un besoin à commander', () => {
    const rows = buildStockRows([{
      ...product,
      variants: [
        { ...product.variants[0], stockOnHand: 2, stockReserved: 2 },
        { ...product.variants[0], id: 43, sku: 'TS-L', stockTracked: false, stockOnHand: null, stockReserved: 0 }
      ]
    }], []);

    expect(rows[0].status).toBe('settled');
    expect(rows[0].available).toBe(0);
    const negative = buildStockRows([{ ...product, variants: [{ ...product.variants[0], stockOnHand: -1, stockReserved: 0 }] }], []);
    expect(negative[0].status).toBe('settled');
    expect(rows[1].status).toBe('untracked');
    expect(rows[1].available).toBeNull();
  });

  it('inclut les commandes en attente de paiement dans la quantité à commander', () => {
    const rows = buildStockRows([{
      ...product,
      variants: [{ ...product.variants[0], stockOnHand: 0, stockReserved: 3 }]
    }], [order('PENDING_PAYMENT', '2026-09-30T10:00:00Z', [item(1, 3)])]);

    expect(rows[0].toPrepare).toBe(0);
    expect(rows[0].toOrder).toBe(3);
    expect(rows[0].status).toBe('to-order');
  });

  it('réunit les commandes client et fournisseur par date avec leurs états', () => {
    const supplierOrder: ShopSupplierOrderDto = {
      id: 9, supplierName: 'Fournisseur A', reference: 'BC-1', status: 'ORDERED',
      createdAt: '2026-09-29T12:00:00Z', receivedAt: null,
      lines: [{ id: 1, variantId: 42, productName: 'T-shirt', variantName: 'M', sku: 'TS-M', quantity: 5 }]
    };
    const rows = buildStockRows([product], [
      order('PENDING_PAYMENT', '2026-09-30T09:00:00Z', [item(1, 2)]),
      order('PAID', '2026-09-28T12:00:00Z', [item(2, 3)]),
      order('COMPLETED', '2026-09-27T12:00:00Z', [item(3, 1)])
    ], Date.parse('2026-09-30T12:00:00Z'), [supplierOrder]);

    expect(rows[0].activities.map(activity => activity.type === 'customer'
      ? [activity.orderStatus, activity.quantity, activity.stockEffect]
      : [activity.status, activity.quantity])).toEqual([
      ['PENDING_PAYMENT', 2, 'reserved'], ['ORDERED', 5],
      ['PAID', 3, 'deducted'], ['COMPLETED', 1, 'deducted']
    ]);
    expect(rows[0].available).toBe(5);
    expect(rows[0].awaitingSupplier).toBe(5);
  });

  it('distingue le besoin restant, les achats attendus, le prêt et le surplus', () => {
    const paid = order('PAID', '2026-09-29T12:00:00Z', [item(1, 10)]);
    const supplier = (status: ShopSupplierOrderDto['status'], quantity: number): ShopSupplierOrderDto => ({
      id: quantity, supplierName: 'Fournisseur', reference: null, status,
      createdAt: '2026-09-29T13:00:00Z', receivedAt: status === 'RECEIVED' ? '2026-09-30T10:00:00Z' : null,
      lines: [{ id: quantity, variantId: 42, productName: 'T-shirt', variantName: 'M', sku: 'TS-M', quantity }]
    });
    const variant = (stockOnHand: number) => [{ ...product, variants: [{ ...product.variants[0], stockOnHand, stockReserved: 0 }] }];

    const missing = buildStockRows(variant(-7), [paid], Date.now(), [supplier('ORDERED', 2)])[0];
    expect([missing.toOrder, missing.awaitingSupplier, missing.readyToHandOver, missing.surplus])
      .toEqual([5, 2, 3, 0]);
    expect(missing.status).toBe('to-order');

    const awaiting = buildStockRows(variant(-7), [paid], Date.now(), [supplier('ORDERED', 7)])[0];
    expect(awaiting.toOrder).toBe(0);
    expect(awaiting.status).toBe('awaiting');

    const received = buildStockRows(variant(2), [paid], Date.now(), [supplier('RECEIVED', 9)])[0];
    expect(received.awaitingSupplier).toBe(0);
    expect(received.readyToHandOver).toBe(10);
    expect(received.surplus).toBe(2);
  });
});

describe('ShopStockOverviewComponent', () => {
  beforeAll(() => registerLocaleData(localeFr));

  it('prépare les besoins et lance la commande avec le fournisseur et les quantités', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1200, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: -2, stockReserved: 0, version: 0
      }]
    };
    const paidOrder: ShopAdminOrderDto = {
      orderNumber: 'CMD-1', status: 'PAID', createdAt: '2026-09-29T10:00:00Z',
      total: { amountInCents: 2400, currency: 'EUR' }, customerEmail: null,
      customerTribeId: null, refundRequested: false,
      items: [{ id: 1, productVariantId: 42, productName: 'T-shirt', variantName: 'M', sku: 'TS-M',
        quantity: 2, unitPrice: { amountInCents: 1200, currency: 'EUR' },
        lineTotal: { amountInCents: 2400, currency: 'EUR' }, status: 'PENDING', stockReserved: false }]
    };
    const supplierOrder: ShopSupplierOrderDto = {
      id: 9, supplierName: 'Fournisseur A', reference: null, status: 'ORDERED',
      createdAt: '2026-09-30T10:00:00Z', receivedAt: null,
      lines: [{ id: 1, variantId: 42, productName: 'T-shirt', variantName: 'M', sku: 'TS-M',
        quantity: 2, unitCostAmountInCents: 800, lineTotalAmountInCents: 1600 }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService',
      ['products', 'orders', 'supplierOrders', 'createSupplierOrderDraft', 'placeSupplierOrderDraft']);
    api.products.and.returnValue(of([product]));
    api.orders.and.returnValue(of([paidOrder]));
    api.supplierOrders.and.returnValue(of([]));
    api.createSupplierOrderDraft.and.returnValue(of({ ...supplierOrder, status: 'DRAFT' }));
    api.placeSupplierOrderDraft.and.returnValue(of(supplierOrder));
    TestBed.configureTestingModule({
      imports: [ShopStockOverviewComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopStockOverviewComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.prepareSupplierOrder();
    expect(component.purchaseDrafts[42]).toBe(2);
    component.supplierName = 'Fournisseur A';
    component.createSupplierOrder();
    expect(api.createSupplierOrderDraft).toHaveBeenCalledWith({
      supplierName: 'Fournisseur A', reference: null,
      lines: [{ variantId: 42, quantity: 2 }]
    });
    expect(api.placeSupplierOrderDraft).toHaveBeenCalledWith(9);
    expect(component.activeDraftId).toBeNull();
    expect(component.draftDirty).toBeFalse();
    fixture.destroy();
  });

  it('laisse la commande fournisseur accessible sans besoin et ouvre la modale', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1200, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: 7, stockReserved: 0, version: 0
      }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'supplierOrders']);
    api.products.and.returnValue(of([product]));
    api.orders.and.returnValue(of([]));
    api.supplierOrders.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [ShopStockOverviewComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopStockOverviewComponent);
    fixture.detectChanges();

    const button = [...fixture.nativeElement.querySelectorAll('button')]
      .find((element: HTMLButtonElement) => element.textContent?.trim() === 'Commande fournisseur') as HTMLButtonElement;
    expect(button.disabled).toBeFalse();
    button.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('dialog')?.open).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Nouvelle commande fournisseur');
    expect(fixture.nativeElement.textContent).not.toContain('Prix unitaire');
    expect(fixture.nativeElement.textContent).not.toContain('Achat supplémentaire validé');
    expect(fixture.componentInstance.purchaseDrafts[42]).toBeUndefined();
    fixture.destroy();
  });

  it('ouvre le détail chronologique au clic sur la ligne', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1200, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: 7, stockReserved: 2, version: 0
      }]
    };
    const customerOrder: ShopAdminOrderDto = {
      orderNumber: 'CMD-1', status: 'PENDING_PAYMENT', createdAt: '2026-09-29T10:00:00Z',
      total: { amountInCents: 2400, currency: 'EUR' }, customerEmail: null,
      customerTribeId: null, refundRequested: false,
      items: [{ id: 1, productVariantId: 42, productName: 'T-shirt', variantName: 'M',
        sku: 'TS-M', quantity: 2, unitPrice: { amountInCents: 1200, currency: 'EUR' },
        lineTotal: { amountInCents: 2400, currency: 'EUR' }, status: 'PENDING', stockReserved: true }]
    };
    const supplierOrder: ShopSupplierOrderDto = {
      id: 9, supplierName: 'Fournisseur A', reference: null, status: 'ORDERED',
      createdAt: '2026-09-30T10:00:00Z', receivedAt: null,
      lines: [{ id: 1, variantId: 42, productName: 'T-shirt', variantName: 'M', sku: 'TS-M', quantity: 5 }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'supplierOrders']);
    api.products.and.returnValue(of([product]));
    api.orders.and.returnValue(of([customerOrder]));
    api.supplierOrders.and.returnValue(of([supplierOrder]));
    TestBed.configureTestingModule({
      imports: [ShopStockOverviewComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopStockOverviewComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.stock-history-row')).toBeNull();
    expect(fixture.nativeElement.textContent).not.toContain('Commandé fournisseur');
    (fixture.nativeElement.querySelector('.stock-variant-row') as HTMLTableRowElement).click();
    fixture.detectChanges();

    const entries = [...fixture.nativeElement.querySelectorAll('.stock-activity-list > li')]
      .map((element: Element) => element.textContent ?? '');
    expect(entries.length).toBe(2);
    expect(entries[0]).toContain('Commande fournisseur n°9');
    expect(entries[0]).toContain('À recevoir');
    expect(entries[1]).toContain('Commande client CMD-1');
    expect(entries[1]).toContain('2 articles commandés');
    expect(entries[1]).toContain('Article : En attente · Commande : En attente de paiement');
    fixture.destroy();
  });

  it('affiche le tableau même si les commandes client sont indisponibles', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: []
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'supplierOrders']);
    api.products.and.returnValue(of([product]));
    api.orders.and.returnValue(throwError(() => new Error('orders unavailable')));
    api.supplierOrders.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [ShopStockOverviewComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopStockOverviewComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.loading).toBeFalse();
    expect(fixture.componentInstance.loaded).toBeTrue();
    expect(fixture.nativeElement.querySelector('[role="alert"]')?.textContent).toContain('commandes client');
    fixture.destroy();
  });

  it('affiche les colonnes de besoin sans les contrôles retirés', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1200, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: 7, stockReserved: 2, version: 0
      }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'supplierOrders']);
    api.products.and.callFake(() => of([structuredClone(product)]));
    api.orders.and.returnValue(of([]));
    api.supplierOrders.and.returnValue(of([]));
    TestBed.configureTestingModule({
      imports: [ShopStockOverviewComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopStockOverviewComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.rows[0].available).toBe(5);
    expect(fixture.nativeElement.querySelector('tbody tr')?.textContent).toContain('TS-M');
    const headings = [...fixture.nativeElement.querySelectorAll('.stock-table thead th')].map((element: Element) => element.textContent?.trim());
    expect(headings).toEqual(['Produit / variante', 'Commandes client', 'À commander chez le fournisseur',
      'Commande fournisseur en cours', 'En stock']);
    expect(fixture.nativeElement.querySelector('#stock-count-42')).toBeNull();
    expect(fixture.nativeElement.querySelector('#purchase-count-42')).toBeNull();
    fixture.destroy();
  });

  it('recharge le stock courant quand une autre opération a modifié la variante', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1200, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: 7, stockReserved: 0, version: 0
      }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'supplierOrders', 'updateVariantStock']);
    api.products.and.callFake(() => of([structuredClone(product)]));
    api.orders.and.returnValue(of([]));
    api.supplierOrders.and.returnValue(of([]));
    api.updateVariantStock.and.returnValue(throwError(() => ({ status: 409 })));
    TestBed.configureTestingModule({
      imports: [ShopStockOverviewComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopStockOverviewComponent);
    fixture.detectChanges();

    product.variants[0].stockOnHand = 5;
    product.variants[0].version = 1;
    fixture.componentInstance.stockDrafts[42] = 8;
    fixture.componentInstance.saveStock(fixture.componentInstance.rows[0]);

    expect(api.updateVariantStock).toHaveBeenCalledWith(42, 8, 0);
    expect(api.products).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.rows[0].variant.stockOnHand).toBe(5);
    expect(fixture.componentInstance.stockDrafts[42]).toBe(5);
    expect(fixture.componentInstance.rowErrors[42]).toContain('a changé');
  });
});
