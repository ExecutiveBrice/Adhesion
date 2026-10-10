import { test, expect, signIn } from './fixtures';

test('Connexion: une session anonyme est redirigée puis peut se connecter', async ({ page, api }) => {
  await page.goto('/boutique');
  await expect(page).toHaveURL(/\/login\?returnUrl=/);
  await signIn(page);
  await page.goto('/boutique');
  await expect(page.getByRole('heading', { name: 'T-shirt de test' })).toBeVisible();
  const request = api.requests.find(item => item.pathname === '/auth/signin');
  expect(request?.body).toMatchObject({ username: 'membre@example.test', password: 'Secret-test-42' });
});

test('ACCESS-002: le référent consulte ses adhésions sans actions de secrétariat', async ({ page, api }) => {
  api.managedSections = [{ id: 2, nom: 'Yoga', couleur: '#5CBBAF' }];
  await signIn(page);
  await page.goto('/adhesions');
  await expect(page.getByText('Adhésions de mes sections · consultation seule')).toBeVisible();
  await expect(page.getByText('Yoga de test', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Gérer', exact: true })).toHaveCount(0);
  await expect(page.locator('button[title="Marquer le paiement comme validé"]')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Ouvrir la fiche adhérent' })).toHaveCount(0);
  expect(api.requests.filter(item => item.pathname === '/adhesion/page')).toHaveLength(0);
  expect(api.requests.some(item => item.pathname === '/adhesion/managed/page')).toBe(true);
});

test('Accès: un membre sans section ne peut pas ouvrir la liste gérée', async ({ page }) => {
  await signIn(page);
  await page.goto('/adhesions');
  await expect(page).toHaveURL(/\/profil$/);
  await expect(page.getByText('Adhésions de mes sections · consultation seule')).toHaveCount(0);
});

test('SHOP-001: du catalogue à la commande, le panier est vidé après création', async ({ page, api }) => {
  await signIn(page);
  await page.goto('/boutique');
  await page.getByRole('link', { name: 'Voir le détail de T-shirt de test' }).click();
  await page.getByRole('button', { name: 'Ajouter au panier' }).click();
  await page.goto('/boutique/panier');
  await expect(page.getByLabel('Quantité', { exact: true })).toHaveValue('1');
  await page.getByRole('link', { name: 'Passer au paiement' }).click();
  await page.getByRole('button', { name: 'Créer la commande et poursuivre' }).click();
  await expect(page).toHaveURL(/\/boutique\/commandes\/TEST-0001$/);
  await expect(page.getByRole('heading', { name: 'Prête à être réglée' })).toBeVisible();
  const creation = api.requests.filter(item => item.method === 'POST' && item.pathname === '/shop/orders');
  expect(creation).toHaveLength(1);
  expect(creation[0].body).toEqual({ items: [{ variantId: 11, quantity: 1 }] });
  expect(creation[0].idempotencyKey).toBeTruthy();
  await page.goto('/boutique/panier');
  await expect(page.getByRole('heading', { name: 'Votre panier est vide' })).toBeVisible();
});

test('Commande: un échec de création conserve le panier et permet de réessayer', async ({ page, api }) => {
  await signIn(page);
  await page.goto('/boutique/produits/1');
  await page.getByRole('button', { name: 'Ajouter au panier' }).click();
  api.rejectCheckout = true;
  await page.goto('/boutique/checkout');
  await page.getByRole('button', { name: 'Créer la commande et poursuivre' }).click();
  await expect(page.getByRole('alert').filter({ hasText: 'Aucun paiement n’a été lancé' })).toBeVisible();
  api.rejectCheckout = false;
  await page.getByRole('button', { name: 'Créer la commande et poursuivre' }).click();
  await expect(page).toHaveURL(/\/boutique\/commandes\/TEST-0001$/);
});

test('SHOP-003: un retour navigateur ne confirme pas un paiement en attente', async ({ page, api }) => {
  await signIn(page);
  await page.goto('/boutique/commandes/TEST-0001/paiement?status=PAID');
  await expect(page.getByRole('heading', { name: 'Vérification du paiement' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Merci pour votre commande' })).toHaveCount(0);
  await expect.poll(() => api.requests.filter(item => item.pathname.endsWith('/payment/verify')).length).toBeGreaterThan(0);
});

test('SHOP-003: la confirmation du serveur ouvre la page de succès', async ({ page, api }) => {
  await signIn(page);
  await page.goto('/boutique/commandes/TEST-0001/paiement');
  await expect(page.getByRole('button', { name: 'Vérifier maintenant' })).toBeEnabled();
  api.paymentConfirmed = true;
  await page.getByRole('button', { name: 'Vérifier maintenant' }).click();
  await expect(page).toHaveURL(/\/paiement-reussi$/);
  await expect(page.getByRole('heading', { name: 'Merci pour votre commande' })).toBeVisible();
});
