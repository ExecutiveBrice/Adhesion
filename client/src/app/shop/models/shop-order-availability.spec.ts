import { buildOrderAvailability } from './shop-order-availability';
import { ShopAdminOrderDto, ShopAdminProductDto } from './shop.models';

describe('Disponibilité des commandes', () => {
  function product(stockOnHand: number): ShopAdminProductDto {
    return { id: 1, name: 'T-shirt', slug: 't-shirt', description: null, imageUrl: null,
      active: true, displayOrder: 0, categories: [], variants: [{ id: 42, sku: 'TS-M', label: 'M',
        priceAmountInCents: 1500, currency: 'EUR', active: true, displayOrder: 0,
        stockTracked: true, stockOnHand, stockReserved: 0, version: 0 }] };
  }
  function order(id: number, quantity: number, status: ShopAdminOrderDto['status'] = 'PAID'): ShopAdminOrderDto {
    return { orderNumber: `CMD-${id}`, status, createdAt: `2026-09-${20 + id}T10:00:00Z`,
      total: { amountInCents: 1500 * quantity, currency: 'EUR' }, customerEmail: null,
      customerTribeId: null, refundRequested: false, items: [{ id, productVariantId: 42,
        productName: 'T-shirt', variantName: 'M', sku: 'TS-M', quantity, status: 'PENDING',
        stockReserved: true, unitPrice: { amountInCents: 1500, currency: 'EUR' },
        lineTotal: { amountInCents: 1500 * quantity, currency: 'EUR' } }] };
  }

  it('réintègre les pièces payées et attribue une pénurie par ancienneté sans compter deux fois le stock', () => {
    const first = order(1, 2);
    const second = order(2, 2);
    const states = buildOrderAvailability([product(-1)], [second, first]);
    expect(states.get(1)?.state).toBe('ready');
    expect(states.get(2)?.missingQuantity).toBe(1);
    expect(buildOrderAvailability([product(0)], [first, second]).get(2)?.state).toBe('ready');
  });

  it('privilégie les commandes payées et ne réintègre pas une réservation non payée', () => {
    const pending = order(1, 2, 'PENDING_PAYMENT');
    const paid = order(2, 2);
    const states = buildOrderAvailability([product(1)], [pending, paid]);
    expect(states.get(2)?.state).toBe('ready');
    expect(states.get(1)?.missingQuantity).toBe(1);
  });

  it('ignore les lignes clôturées et les commandes annulées dans la répartition', () => {
    const delivered = order(1, 3);
    delivered.items[0].status = 'COMPLETED';
    const cancelled = order(2, 3, 'CANCELLED');
    const unpaid = order(3, 2, 'PENDING_PAYMENT');
    expect(buildOrderAvailability([product(1)], [delivered, cancelled, unpaid]).get(3)?.missingQuantity).toBe(1);
  });

  it('signale les stocks inconnus et ne réintègre pas les lignes sans réservation', () => {
    const paid = order(1, 2);
    paid.items[0].stockReserved = false;
    expect(buildOrderAvailability([product(1)], [paid]).get(1)?.missingQuantity).toBe(1);
    expect(buildOrderAvailability([], [paid]).get(1)?.state).toBe('unknown');
    const untracked = product(1);
    untracked.variants[0].stockTracked = false;
    expect(buildOrderAvailability([untracked], [paid]).get(1)?.label).toBe('Stock non suivi');
  });
});
