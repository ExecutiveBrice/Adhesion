import { test as base, expect, Page } from '@playwright/test';
import type { ShopOrderDto, ShopProductDto, CartQuoteDto } from '../../src/app/shop/models/shop.models';

const cents = (amountInCents: number) => ({ amountInCents, currency: 'EUR' });
const product: ShopProductDto = {
  id: 1, name: 'T-shirt de test', slug: 't-shirt-test', description: 'Produit fictif', imageUrl: null,
  categories: [{ id: 1, name: 'Vêtements', slug: 'vetements' }],
  variants: [{ id: 11, sku: 'TEST-M', label: 'M', price: cents(2000), available: true }]
};

export interface ApiFixture {
  roles: string[];
  inscriptionEnabled: boolean | undefined;
  rejectSignup: boolean;
  managedSections: { id: number; nom: string; couleur: string }[];
  paymentConfirmed: boolean;
  rejectCheckout: boolean;
  requests: { method: string; pathname: string; search: string; body: any; idempotencyKey?: string }[];
}

function session(roles: string[]) {
  const payload = Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url');
  // Session fictive acceptée par les gardes UI ; aucune signature ni authentification serveur n'est testée ici.
  return { token: `test.${payload}.test`, id: 1, username: 'membre@example.test', roles };
}

export async function signIn(page: Page) {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill('membre@example.test');
  await page.locator('input[name="password"]').fill('Secret-test-42');
  await page.locator('form').getByRole('button', { name: 'Connexion', exact: true }).click();
  await expect(page).toHaveURL(/\/accueil$/);
}

export const test = base.extend<{ api: ApiFixture }>({
  api: [async ({ context, page }, use) => {
    const api: ApiFixture = { roles: ['ROLE_USER'], inscriptionEnabled: true, rejectSignup: false, managedSections: [], paymentConfirmed: false,
      rejectCheckout: false, requests: [] };
    const unexpected: string[] = [];
    const pageErrors: string[] = [];
    page.on('pageerror', error => pageErrors.push(error.message));
    let order: ShopOrderDto | null = null;
    const newOrder = (): ShopOrderDto => ({ id: 1, orderNumber: 'TEST-0001', status: 'PENDING_PAYMENT',
      items: [{ productId: 1, productName: product.name, variantId: 11, variantName: 'M', sku: 'TEST-M',
        unitPrice: cents(2000), quantity: 1, lineTotal: cents(2000) }],
      subtotal: cents(2000), discountTotal: cents(0), feesTotal: cents(0), total: cents(2000),
      refundRequested: false, createdAt: new Date().toISOString(),
      paymentExpiresAt: new Date(Date.now() + 86_400_000).toISOString() });

    await context.route('http://localhost:8000/**', async route => {
      const request = route.request();
      const { pathname, search } = new URL(request.url());
      const method = request.method();
      const body = request.postData() ? request.postDataJSON() : null;
      api.requests.push({ method, pathname, search, body, idempotencyKey: request.headers()['idempotency-key'] });
      const respond = (json: unknown, status = 200) => route.fulfill({ status, json,
        headers: { 'access-control-allow-origin': 'http://127.0.0.1:4300' } });
      if (method === 'POST' && pathname === '/auth/signin') return respond(session(api.roles));
      if (method === 'POST' && pathname === '/auth/signup') return api.rejectSignup
        ? respond({ message: 'Erreur: cet e-mail est déjà utilisé' }, 400)
        : respond({ message: 'User registered successfully!' });
      if (method === 'GET' && pathname === '/param/isClose') return respond(false);
      if (method === 'GET' && pathname === '/param/allBoolean') return respond([
        { paramName: 'Show_Boutique', paramValue: true }, { paramName: 'Show_Chat', paramValue: false },
        ...(api.inscriptionEnabled === undefined ? [] : [{ paramName: 'Inscription', paramValue: api.inscriptionEnabled }]),
        { paramName: 'Ouvert', paramValue: true }
      ]);
      const emptyEndpoints = ['/param/allText', '/param/agendas', '/param/salles', '/activite/calendrier',
        '/activite/calendrier/google', '/publicites', '/chat', '/activite/all', '/files/allFilesName'];
      if (method === 'GET' && emptyEndpoints.includes(pathname)) return respond([]);
      if (method === 'GET' && pathname === '/user/referentActivite') return respond(false);
      if (method === 'GET' && pathname === '/section-management/sections') return respond(api.managedSections);
      if (method === 'GET' && pathname === '/tribu/getConnectedTribu') return respond({
        id: 1, uuid: 'tribu-test', adherents: [{ id: 1, nom: 'TEST', prenom: 'Alice',
          user: { id: 1, username: 'membre@example.test', roles: api.roles }, mineur: false,
          adhesions: [], accords: [], activitesNm1: [], cours: [], documents: [] }]
      });
      if (method === 'GET' && pathname === '/adhesion/managed/page') return respond({
        content: [{ id: 21, adherent: { id: 1, nom: 'TEST', prenom: 'Alice', email: 'membre@example.test', tribuId: 'tribu-test' },
          activite: { id: 5, nom: 'Yoga de test', horaire: 'Lundi 18h', section: { id: 2, nom: 'Yoga' } },
          statutActuel: 'Validée', paiements: [], accords: [], tarif: 100,
          validPaiementSecretariat: false, validDocumentSecretariat: false, flag: false }],
        number: 0, totalElements: 1, totalPages: 1
      });
      if (method === 'GET' && pathname === '/shop/products') return respond([product]);
      if (method === 'GET' && pathname === '/shop/products/1') return respond(product);
      if (method === 'GET' && pathname === '/shop/orders') return respond(order ? [order] : []);
      if (method === 'POST' && pathname === '/shop/cart/quote') {
        const items = body.items.map((entry: { variantId: number; quantity: number }) => ({ productId: 1,
          productName: product.name, variantId: entry.variantId, variantName: 'M', sku: 'TEST-M',
          unitPrice: cents(2000), quantity: entry.quantity, lineTotal: cents(2000 * entry.quantity), available: true }));
        const total = cents(items.reduce((sum: number, item: { lineTotal: { amountInCents: number } }) => sum + item.lineTotal.amountInCents, 0));
        const quote: CartQuoteDto = { items, subtotal: total, discountTotal: cents(0), feesTotal: cents(0), total };
        return respond(quote);
      }
      if (method === 'POST' && pathname === '/shop/orders') {
        if (api.rejectCheckout) return respond({ message: 'Erreur fictive de création' }, 503);
        order = newOrder(); return respond(order, 201);
      }
      if (method === 'GET' && pathname === '/shop/orders/TEST-0001') {
        order ??= newOrder();
        if (api.paymentConfirmed) order.status = 'PAID';
        return respond(order);
      }
      if (method === 'POST' && pathname === '/shop/orders/TEST-0001/payment/verify') {
        order ??= newOrder();
        if (api.paymentConfirmed) order.status = 'PAID';
        return respond(order);
      }
      unexpected.push(`${method} ${pathname}`);
      return respond({ message: 'Requête non prévue par la fixture' }, 501);
    });
    await use(api);
    expect(unexpected, 'Toute nouvelle requête doit être représentée explicitement dans la fixture').toEqual([]);
    expect(pageErrors, 'Le parcours ne doit pas masquer une exception Angular').toEqual([]);
  }, { auto: true }]
});
export { expect };
