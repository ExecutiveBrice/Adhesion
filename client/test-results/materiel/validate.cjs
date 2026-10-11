const { chromium, expect } = require('@playwright/test');
const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../dist/browser');
const zones = [];
const items = [];
const loans = [];
const errors = [];
let id = 0;
const server = http.createServer((req, res) => {
  const pathname = new URL(req.url, 'http://localhost').pathname.replace(/^\/adhesion/, '');
  const file = path.join(root, decodeURIComponent(pathname === '/' ? '/index.html' : pathname));
  if (!file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
  const mime = { '.html': 'text/html', '.js': 'application/javascript', '.css': 'text/css', '.png': 'image/png', '.json': 'application/json' };
  fs.readFile(file, (error, data) => {
    if (error) { res.writeHead(404).end(); return; }
    res.writeHead(200, { 'Content-Type': mime[path.extname(file)] || 'application/octet-stream' }).end(data);
  });
});

(async () => {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
  try {
    const context = await browser.newContext({ viewport: { width: 1600, height: 1000 }, locale: 'fr-FR', timezoneId: 'Europe/Paris' });
    await context.addInitScript(() => {
      localStorage.setItem('auth-token', 'local-test-token');
      localStorage.setItem('auth-user', JSON.stringify({ id: 3, username: 'camille@example.org', roles: ['ROLE_ADMIN', 'ROLE_MEMBRECA'] }));
    });
    await context.route('http://localhost:8000/**', async route => {
      const req = route.request();
      const endpoint = new URL(req.url()).pathname;
      const method = req.method();
      let data = [];
      let status = 200;
      if (endpoint === '/param/isClose') data = false;
      if (endpoint === '/materiel/membres-ca') data = [{ id: 3, nom: 'Camille Martin' }, { id: 4, nom: 'Alex Durand' }];
      if (endpoint === '/materiel/zonages') {
        if (method === 'POST') zones.push({ id: ++id, ...req.postDataJSON() });
        data = method === 'POST' ? zones.at(-1) : zones;
      }
      if (endpoint.startsWith('/materiel/zonages/') && method === 'DELETE') {
        status = 409; data = { detail: 'Ce zonage contient du matériel et ne peut pas être supprimé' };
      }
      if (endpoint === '/materiel') {
        if (method === 'POST') {
          const input = req.postDataJSON();
          items.push({ id: ++id, nom: input.nom, zonage: zones.find(z => z.id === input.zonageId), emprunteurId: null, emprunteurNom: null, dateEmprunt: null, horsService: input.horsService ?? false });
        }
        data = method === 'POST' ? items.at(-1) : items;
      }
      if (/\/materiel\/\d+\/(emprunt|retour)$/.test(endpoint)) {
        const item = items.find(i => i.id === Number(endpoint.split('/')[2]));
        const emprunt = endpoint.endsWith('/emprunt');
        const borrowerId = req.postDataJSON()?.emprunteurId ?? 3;
        const borrowerName = borrowerId === 3 ? 'Camille Martin' : 'Alex Durand';
        if (emprunt) loans.unshift({ id: ++id, materielId: item.id, emprunteurId: borrowerId, emprunteurNom: borrowerName,
          dateEmprunt: '2026-10-10T20:30:00Z', dateRetour: null, enregistreParNom: 'Camille Martin', rapporteParNom: null });
        else Object.assign(loans.find(l => l.materielId === item.id && !l.dateRetour), { dateRetour: '2026-10-10T21:00:00Z', rapporteParNom: 'Camille Martin' });
        Object.assign(item, { emprunteurId: emprunt ? borrowerId : null, emprunteurNom: emprunt ? borrowerName : null, dateEmprunt: emprunt ? '2026-10-10T20:30:00Z' : null });
        data = item;
      }
      if (/\/materiel\/\d+\/historique$/.test(endpoint)) data = loans.filter(l => l.materielId === Number(endpoint.split('/')[2]));
      if (/\/materiel\/\d+\/hors-service$/.test(endpoint)) {
        const item = items.find(i => i.id === Number(endpoint.split('/')[2]));
        item.horsService = req.postDataJSON().horsService;
        data = item;
      }
      await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) });
    });
    const page = await context.newPage();
    page.on('pageerror', error => { errors.push(error.message); console.error('PAGE ERROR:', error.message); });
    page.on('console', msg => { if (msg.type() === 'error') console.error('CONSOLE:', msg.text()); });
    await page.goto(url + '/adhesion/#/admin');
    await page.getByRole('button', { name: /Zonages du matériel/ }).click();
    await page.locator('#zonageNom').fill('Réserve — étagère A');
    await page.locator('#zonageCouleur').fill('#4285f4');
    await page.locator('app-zonages').getByRole('button', { name: 'Ajouter', exact: true }).click();
    await expect(page.getByRole('status')).toContainText('Zonage ajouté.');
    await page.getByRole('link', { name: 'Matériel', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Matériel', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Paramétrage' }).click();
    await page.getByLabel('Nom du matériel', { exact: true }).fill('Vidéoprojecteur n° 1');
    await page.getByLabel('Zonage du stock', { exact: true }).selectOption({ label: 'Réserve — étagère A' });
    await page.getByRole('button', { name: 'Ajouter le matériel' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.locator('tbody')).toContainText('Vidéoprojecteur n° 1');
    await expect(page.locator('tbody')).toContainText('Disponible');
    await page.getByRole('button', { name: 'Emprunter Vidéoprojecteur n° 1' }).click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await expect(page.getByLabel('Membre du CA emprunteur')).toBeEnabled();
    await expect.poll(() => page.getByLabel('Membre du CA emprunteur').evaluate(e => e.selectedOptions[0]?.textContent ?? '')).toBe('Camille Martin');
    await page.screenshot({ path: path.join(__dirname, 'emprunteur.png'), fullPage: true, animations: 'disabled' });
    await expect(page.locator('tbody')).toContainText('Disponible');
    await page.getByLabel('Membre du CA emprunteur').selectOption({ label: 'Alex Durand' });
    await page.getByRole('button', { name: 'Confirmer l’emprunt' }).click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.locator('tbody')).toContainText('Alex Durand');
    await expect(page.locator('tbody')).toContainText('10/10/2026 22:30');
    await page.screenshot({ path: path.join(__dirname, 'desktop.png'), fullPage: true, animations: 'disabled' });
    await page.reload();
    await expect(page.locator('tbody')).toContainText('Alex Durand');
    await page.getByRole('button', { name: 'Journal d’emprunt de Vidéoprojecteur n° 1' }).click();
    await expect(page.getByRole('dialog')).toContainText('En cours');
    await expect(page.getByRole('dialog')).toContainText('Alex Durand');
    await expect(page.getByRole('dialog')).toContainText('Camille Martin');
    await page.getByRole('dialog').getByRole('button', { name: 'Fermer', exact: true }).last().click();
    await expect(page.locator('#materielHS2')).toHaveClass(/btn-secondary/);
    await page.locator('#materielHS2').click();
    await expect(page.locator('#materielHS2')).toHaveClass(/btn-danger/);
    await expect(page.locator('#materielHS2')).toHaveAttribute('aria-pressed', 'true');
    await expect(page.locator('tbody')).toContainText('Emprunté');
    await expect(page.locator('tbody')).not.toContainText('Hors service (HS)');
    await page.getByRole('button', { name: 'Restituer Vidéoprojecteur n° 1' }).click();
    await expect(page.locator('tbody')).not.toContainText('Alex Durand');
    await expect(page.getByRole('button', { name: 'Emprunter Vidéoprojecteur n° 1' })).toBeEnabled();
    await page.reload();
    await expect(page.locator('tbody')).toContainText('Disponible');
    await expect(page.locator('#materielHS2')).toHaveClass(/btn-danger/);
    await page.getByRole('button', { name: 'Journal d’emprunt de Vidéoprojecteur n° 1' }).click();
    await expect(page.getByRole('dialog')).toContainText('Rapporté');
    await expect(page.getByRole('dialog')).toContainText('Alex Durand');
    await expect(page.getByRole('dialog')).toContainText('Camille Martin');
    await page.screenshot({ path: path.join(__dirname, 'journal.png'), fullPage: true, animations: 'disabled' });
    await page.getByRole('dialog').getByRole('button', { name: 'Fermer', exact: true }).last().click();
    await page.locator('#materielHS2').click();
    await expect(page.locator('#materielHS2')).toHaveClass(/btn-secondary/);
    await expect(page.locator('#materielHS2')).toHaveAttribute('aria-pressed', 'false');
    await expect(page.locator('tbody')).toContainText('Disponible');
    await page.locator('#materielHS2').click();
    await expect(page.locator('#materielHS2')).toHaveClass(/btn-danger/);
    await page.getByRole('link', { name: 'Administration', exact: true }).click();
    await page.getByRole('button', { name: /Zonages du matériel/ }).click();
    await page.getByRole('button', { name: 'Supprimer le zonage Réserve — étagère A' }).click();
    await expect(page.getByRole('alert')).toContainText('Ce zonage contient du matériel');

    await context.addInitScript(() => localStorage.setItem('auth-user', JSON.stringify({ id: 3, username: 'camille@example.org', roles: ['ROLE_MEMBRECA'] })));
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(url + '/adhesion/#/materiel');
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Matériel', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Paramétrage' })).toHaveCount(0);
    await expect(page.locator('#materielHS2')).toBeDisabled();
    await expect(page.locator('#materielHS2')).toHaveClass(/btn-danger/);
    expect(await page.locator('.zone-label').evaluate(e => getComputedStyle(e).whiteSpace)).toBe('nowrap');
    expect(await page.locator('.materiel-etat').evaluate(e => {
      const [badge, bouton] = [...e.children].map(child => child.getBoundingClientRect());
      return Math.abs((badge.top + badge.height / 2) - (bouton.top + bouton.height / 2)) < 1;
    })).toBeTruthy();
    await page.getByRole('button', { name: 'Emprunter Vidéoprojecteur n° 1' }).click();
    await expect(page.locator('tbody')).toContainText('Camille Martin');
    await page.screenshot({ path: path.join(__dirname, 'mobile.png'), fullPage: true, animations: 'disabled' });
    await page.getByRole('button', { name: 'Journal d’emprunt de Vidéoprojecteur n° 1' }).click();
    await expect(page.getByRole('dialog').locator('article')).toHaveCount(2);
    await expect(page.getByRole('dialog')).toContainText('Alex Durand');
    await expect(page.getByRole('dialog')).toContainText('En cours');
    await page.screenshot({ path: path.join(__dirname, 'journal-mobile.png'), fullPage: true, animations: 'disabled' });
    await page.getByRole('dialog').getByRole('button', { name: 'Fermer', exact: true }).last().click();
    console.log('MOBILE SIZE:', await page.evaluate(() => ({ width: innerWidth, scroll: document.documentElement.scrollWidth,
      overflow: [...document.querySelectorAll('body *')].filter(e => e.getBoundingClientRect().right > innerWidth && getComputedStyle(e).position === 'absolute')
        .map(e => ({ tag: e.tagName, cls: e.className, text: e.textContent.slice(0, 45), right: e.getBoundingClientRect().right })) })));
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
    expect(errors).toEqual([]);
    console.log('PASS: administrator preselection, loan to a CA member, return by a different administrator, persistent loan history, gray/red HS indicator, HS allows borrowing, non-wrapping zone and state/HS group, CA direct loan and journal, desktop and mobile views.');
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => server.close());
