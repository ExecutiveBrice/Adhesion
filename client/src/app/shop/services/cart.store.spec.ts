import { of, throwError } from 'rxjs';
import { CartStore } from './cart.store';
import { ShopApiService } from './shop-api.service';
import { CartQuoteDto } from '../models/shop.models';

describe('CartStore', () => {
  const quote: CartQuoteDto = {
    items: [{ productId: 1, productName: 'T-shirt', variantId: 4, variantName: 'M', sku: 'TS-M', quantity: 2,
      unitPrice: { amountInCents: 1500, currency: 'EUR' }, lineTotal: { amountInCents: 3000, currency: 'EUR' }, available: true }],
    subtotal: { amountInCents: 3000, currency: 'EUR' }, discountTotal: { amountInCents: 0, currency: 'EUR' },
    feesTotal: { amountInCents: 0, currency: 'EUR' }, total: { amountInCents: 3000, currency: 'EUR' }
  };

  beforeEach(() => sessionStorage.clear());

  it('ne conserve que les références et quantités, puis demande le total au serveur', async () => {
    const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['quote']);
    api.quote.and.returnValue(of(quote));
    const store = new CartStore(api);

    store.add(4); store.add(4);
    const result = await store.refreshQuote();

    expect(store.entries()).toEqual([{ variantId: 4, quantity: 2 }]);
    expect(api.quote).toHaveBeenCalledWith([{ variantId: 4, quantity: 2 }]);
    expect(result?.total.amountInCents).toBe(3000);
  });

  it('supprime un article lorsque la quantité est invalide', () => {
    const store = new CartStore(jasmine.createSpyObj<ShopApiService>('ShopApiService', ['quote']));
    store.add(4);
    store.setQuantity(4, 0);
    expect(store.isEmpty()).toBeTrue();
  });

  it('expose une erreur utile sans inventer de total lorsque le serveur refuse le panier', async () => {
    const api = jasmine.createSpyObj<ShopApiService>('ShopApiService', ['quote']);
    api.quote.and.returnValue(throwError(() => new Error('unavailable')));
    const store = new CartStore(api);
    store.add(4);
    await store.refreshQuote();
    expect(store.quote()).toBeNull();
    expect(store.error()).toContain('plus disponible');
  });
});
