import { test, expect, signIn } from './fixtures';

const users = [
  { id: 2, adherent: 'Élodie Martin', username: 'elodie@example.test', roles: ['ROLE_USER'] },
  { id: 3, adherent: 'Paul Durand', username: 'paul@example.test', roles: ['ROLE_USER'] }
];

test.beforeEach(async ({ page, api }) => {
  api.roles = ['ROLE_ADMIN'];
  await page.route('http://localhost:8000/param/**', async route => {
    const pathname = new URL(route.request().url()).pathname;
    if (['/param/allNumber', '/param/sections', '/param/taches-seance'].includes(pathname)) {
      await route.fulfill({ json: [] });
    } else {
      await route.fallback();
    }
  });
  await page.route('http://localhost:8000/user/allLite', route => route.fulfill({ json: users }));
  await signIn(page);
  await page.goto('/admin');
  await page.getByRole('button', { name: 'Utilisateurs et rôles' }).click();
  await page.getByRole('button', { name: 'Prendre la place d’un utilisateur' }).click();
});

test('ACCESS-005: le sélecteur filtre les adhérents par nom ou identifiant', async ({ page }) => {
  const search = page.getByRole('searchbox', { name: 'Rechercher un utilisateur à impersonnaliser' });
  await search.fill(' ELODIE ');
  await expect(page.getByRole('button', { name: 'Élodie Martin', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Paul Durand', exact: true })).toHaveCount(0);
  await search.fill('PAUL@EXAMPLE');
  await expect(page.getByRole('button', { name: 'Paul Durand', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Élodie Martin', exact: true })).toHaveCount(0);
  await search.fill('introuvable');
  await expect(page.getByText('Aucun utilisateur trouvé', { exact: true })).toBeVisible();
  await search.fill('');
  await expect(page.getByRole('button', { name: 'Élodie Martin', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Paul Durand', exact: true })).toBeVisible();
});

test('ACCESS-005: la sélection recharge la page avec la nouvelle session', async ({ page }) => {
  const adminSession = await page.evaluate(() => JSON.parse(localStorage.getItem('auth-user')!));
  const targetSession = { ...adminSession, token: adminSession.token.replace('test.', 'target.'),
    id: 2, username: users[0].username, roles: ['ROLE_USER'] };
  await page.route('http://localhost:8000/auth/impersonate/**', route => route.fulfill({ json: targetSession }));
  // Un marqueur en mémoire disparaît uniquement si le document est réellement rechargé.
  await page.evaluate(() => { (window as any).__beforeImpersonation = true; });
  await page.getByRole('button', { name: 'Élodie Martin', exact: true }).click();
  await expect(page).toHaveURL(/\/accueil$/);
  expect(await page.evaluate(() => (window as any).__beforeImpersonation)).toBeUndefined();
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem('auth-user')!))).toEqual(targetSession);
  expect(await page.evaluate(() => localStorage.getItem('auth-token'))).toBe(targetSession.token);
  await expect(page.getByRole('link', { name: 'Administration', exact: true })).toHaveCount(0);
});

test('ACCESS-005: un refus conserve la session et affiche une erreur', async ({ page }) => {
  const adminSession = await page.evaluate(() => localStorage.getItem('auth-user'));
  await page.route('http://localhost:8000/auth/impersonate/**', route => route.fulfill({
    status: 403, json: { message: 'Refus fictif' }
  }));
  await page.getByRole('button', { name: 'Élodie Martin', exact: true }).click();
  await expect(page.getByRole('alert').filter({ hasText: 'L’impersonnalisation a échoué.' })).toBeVisible();
  await expect(page).toHaveURL(/\/admin$/);
  expect(await page.evaluate(() => localStorage.getItem('auth-user'))).toBe(adminSession);
});
