import { test, expect } from './fixtures';

test('ACCESS-006: le menu ouvre l’inscription activée puis permet de revenir à la connexion', async ({ page }) => {
  await page.goto('/login');
  const menu = page.getByRole('navigation', { name: 'Navigation principale' });
  await menu.getByRole('button', { name: 'Inscription', exact: true }).click();
  await expect(page.getByRole('heading', { name: "Page d'inscription" })).toBeVisible();
  await expect(page.locator('input[name="password"]')).toHaveCount(1);
  await menu.getByRole('button', { name: 'Connexion', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Page de connexion' })).toBeVisible();
  await page.getByRole('button', { name: 'Récupération de mot de passe', exact: true }).click();
  await menu.getByRole('button', { name: 'Inscription', exact: true }).click();
  await expect(page.getByRole('heading', { name: "Page d'inscription" })).toBeVisible();
});

for (const [label, value] of [['désactivé', false], ['absent', undefined]] as const) {
  test(`Inscription: le menu masque le bouton quand le paramètre est ${label}`, async ({ page, api }) => {
    api.inscriptionEnabled = value;
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Page de connexion' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Inscription', exact: true })).toHaveCount(0);
    await expect(page.getByRole('heading', { name: "Page d'inscription" })).toHaveCount(0);
  });
}

test('ACCESS-006: une inscription réussie connecte le nouveau compte avant d’ouvrir l’accueil', async ({ page, api }) => {
  await page.goto('/login');
  await page.getByRole('button', { name: 'Inscription', exact: true }).click();
  await page.locator('input[name="username"]').fill('nouveau@example.test');
  await page.locator('input[name="password"]').fill('Secret-test-42');
  await page.getByRole('button', { name: /Accepter l'utilisation des données personnelles/ }).click();
  await expect(page).toHaveURL(/\/accueil$/);
  const requests = api.requests.filter(item => ['/auth/signup', '/auth/signin'].includes(item.pathname));
  expect(requests.map(item => item.pathname)).toEqual(['/auth/signup', '/auth/signin']);
  expect(requests[0].body).toEqual({ username: 'nouveau@example.test', password: 'Secret-test-42' });
  await expect(page.getByRole('button', { name: 'Inscription', exact: true })).toHaveCount(0);
});

test('Inscription: un refus conserve le formulaire et permet de réessayer', async ({ page, api }) => {
  api.rejectSignup = true;
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/login');
  await page.getByRole('button', { name: 'Inscription', exact: true }).click();
  await page.locator('input[name="username"]').fill('nouveau@example.test');
  await page.locator('input[name="password"]').fill('Secret-test-42');
  const submit = page.getByRole('button', { name: /Accepter l'utilisation des données personnelles/ });
  await submit.click();
  await expect(page.getByRole('alert').filter({ hasText: 'cet e-mail est déjà utilisé' })).toBeVisible();
  await expect(page.locator('input[name="username"]')).toHaveValue('nouveau@example.test');
  expect(api.requests.filter(item => item.pathname === '/auth/signin')).toHaveLength(0);
  api.rejectSignup = false;
  await submit.click();
  await expect(page).toHaveURL(/\/accueil$/);
});
