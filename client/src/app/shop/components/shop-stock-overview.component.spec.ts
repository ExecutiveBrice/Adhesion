import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ShopAdminOrderDto, ShopAdminProductDto } from '../models/shop.models';
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
    expect(rows[0].outflow30Days).toBe(5); // Réglées récentes, remboursement inclus.
    expect(rows[0].status).toBe('watch');
  });

  it('signale une rupture sans inventer de quantité disponible pour un stock non suivi', () => {
    const rows = buildStockRows([{
      ...product,
      variants: [
        { ...product.variants[0], stockOnHand: 2, stockReserved: 2 },
        { ...product.variants[0], id: 43, sku: 'TS-L', stockTracked: false, stockOnHand: null, stockReserved: 0 }
      ]
    }], []);

    expect(rows[0].status).toBe('out');
    expect(rows[0].available).toBe(0);
    expect(rows[1].status).toBe('untracked');
    expect(rows[1].available).toBeNull();
  });
});

describe('ShopStockOverviewComponent', () => {
  beforeAll(() => registerLocaleData(localeFr));

  it('affiche le disponible et ajuste seulement le stock de la variante', async () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1200, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: 7, stockReserved: 2, version: 0
      }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'updateVariantStock']);
    api.products.and.callFake(() => of([structuredClone(product)]));
    api.orders.and.returnValue(of([]));
    api.updateVariantStock.and.callFake((_id, quantity) => {
      product.variants[0].stockOnHand = quantity;
      product.variants[0].version++;
      return of(structuredClone(product.variants[0]));
    });
    TestBed.configureTestingModule({
      imports: [ShopStockOverviewComponent],
      providers: [{ provide: ShopAdminApiService, useValue: api }]
    });
    const fixture = TestBed.createComponent(ShopStockOverviewComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.rows[0].available).toBe(5);
    expect(fixture.nativeElement.querySelector('tbody tr')?.textContent).toContain('TS-M');
    const stockInput = fixture.nativeElement.querySelector('#stock-count-42') as HTMLInputElement;
    stockInput.value = '9';
    stockInput.dispatchEvent(new Event('input', { bubbles: true }));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.componentInstance.stockDrafts[42]).toBe(9);
    const stockForm = fixture.nativeElement.querySelector('.stock-adjust') as HTMLFormElement;
    stockForm.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.updateVariantStock).toHaveBeenCalledWith(42, 9, 0);
    expect(fixture.componentInstance.rows[0].available).toBe(7);
    expect(fixture.componentInstance.message).toContain('mis à jour');
    expect(fixture.nativeElement.textContent).toContain('mis à jour');

    const purchaseInput = fixture.nativeElement.querySelector('#purchase-count-42') as HTMLInputElement;
    purchaseInput.value = '1.5';
    purchaseInput.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.invalidPurchaseCount).toBe(1);
    expect((fixture.nativeElement.querySelector('.stock-purchase-actions .btn-primary') as HTMLButtonElement).disabled).toBeTrue();
  });

  it('recharge le stock courant quand une autre opération a modifié la variante', () => {
    const product: ShopAdminProductDto = {
      id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{
        id: 42, sku: 'TS-M', label: 'M', priceAmountInCents: 1200, currency: 'EUR',
        active: true, displayOrder: 0, stockTracked: true, stockOnHand: 7, stockReserved: 0, version: 0
      }]
    };
    const api = jasmine.createSpyObj<ShopAdminApiService>('ShopAdminApiService', ['products', 'orders', 'updateVariantStock']);
    api.products.and.callFake(() => of([structuredClone(product)]));
    api.orders.and.returnValue(of([]));
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
