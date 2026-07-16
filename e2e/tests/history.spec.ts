import { test, expect } from '@playwright/test';

const PNG_BUFFER = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0x0d, 0x49, 0x48, 0x44, 0x52]);

async function selectFile(page: import('@playwright/test').Page, file: { name: string; mimeType: string; buffer: Buffer }) {
  const fileChooserPromise = page.waitForEvent('filechooser');
  await page.getByRole('button', { name: 'Choisir un fichier à partager' }).click();
  const chooser = await fileChooserPromise;
  await chooser.setFiles(file);
}

async function registerAndLogin(page: import('@playwright/test').Page, email: string, password: string) {
  await page.goto('/register');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mot de passe', { exact: true }).fill(password);
  await page.getByLabel('Vérification du mot de passe').fill(password);
  await page.getByRole('button', { name: 'Créer mon compte' }).click();
  await page.waitForURL('**/login*');

  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mot de passe', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Connexion' }).click();
  await page.waitForURL('**/my-space');
}

test('Les fichiers déposés en étant connecté apparaissent dans Mon espace', async ({ page }) => {
  const email = `e2e_history_${crypto.randomUUID()}@example.com`;
  await registerAndLogin(page, email, 'secret123');

  await page.goto('/');
  await selectFile(page, { name: 'libre.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/');
  await selectFile(page, { name: 'protege.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByLabel('Mot de passe', { exact: true }).fill('s3cret!!');
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/my-space');

  const libreRow = page.locator('.my-space__row', { hasText: 'libre.png' });
  const protegeRow = page.locator('.my-space__row', { hasText: 'protege.png' });
  await expect(libreRow).toBeVisible();
  await expect(protegeRow).toBeVisible();
  await expect(protegeRow.locator('.my-space__lock')).toBeVisible();
  await expect(libreRow.locator('.my-space__lock')).toHaveCount(0);
});

test('Le filtre Actifs/Expiré s\'applique à la liste réelle', async ({ page }) => {
  const email = `e2e_history_${crypto.randomUUID()}@example.com`;
  await registerAndLogin(page, email, 'secret123');

  await page.goto('/');
  await selectFile(page, { name: 'actif.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/my-space');
  await page.getByRole('button', { name: 'Actifs' }).click();
  await expect(page.locator('.my-space__row', { hasText: 'actif.png' })).toBeVisible();

  await page.getByRole('button', { name: 'Expiré' }).click();
  await expect(page.locator('.my-space__row', { hasText: 'actif.png' })).toHaveCount(0);
});

test('Le filtre par tag limite la liste aux fichiers correspondants (facultatif, US08)', async ({ page }) => {
  const email = `e2e_history_${crypto.randomUUID()}@example.com`;
  await registerAndLogin(page, email, 'secret123');

  await page.goto('/');
  await selectFile(page, { name: 'alpha.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/');
  await selectFile(page, { name: 'beta.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/my-space');
  const taggedRow = page.locator('.my-space__row', { hasText: 'alpha.png' });
  await taggedRow.locator('.my-space__row-buttons').getByRole('link', { name: 'Accéder' }).click();
  await page.getByLabel('Nouveau tag').fill('vacances');
  await page.getByRole('button', { name: 'Ajouter' }).click();
  await expect(page.locator('.file-detail__tag', { hasText: 'vacances' })).toBeVisible();

  await page.goto('/my-space');
  await expect(page.locator('.my-space__row', { hasText: 'alpha.png' })).toBeVisible();
  await expect(page.locator('.my-space__row', { hasText: 'beta.png' })).toBeVisible();

  await page.getByLabel('Filtrer par tag').fill('vacances');
  await expect(page.locator('.my-space__row', { hasText: 'alpha.png' })).toBeVisible();
  await expect(page.locator('.my-space__row', { hasText: 'beta.png' })).toHaveCount(0);
});
