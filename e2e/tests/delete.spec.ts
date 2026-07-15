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

test('Suppression confirmée : le fichier disparaît de la liste et reste absent après rechargement', async ({ page }) => {
  const email = `e2e_delete_${crypto.randomUUID()}@example.com`;
  await registerAndLogin(page, email, 'secret123');

  await page.goto('/');
  await selectFile(page, { name: 'a-supprimer.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/my-space');
  const row = page.locator('.my-space__row', { hasText: 'a-supprimer.png' });
  await expect(row).toBeVisible();

  await row.getByRole('button', { name: 'Supprimer' }).click();
  await row.getByRole('button', { name: 'Confirmer' }).click();
  await expect(row).toHaveCount(0);

  await page.reload();
  await expect(page.locator('.my-space__row', { hasText: 'a-supprimer.png' })).toHaveCount(0);
});

test('Annulation : le fichier reste dans la liste', async ({ page }) => {
  const email = `e2e_delete_${crypto.randomUUID()}@example.com`;
  await registerAndLogin(page, email, 'secret123');

  await page.goto('/');
  await selectFile(page, { name: 'a-garder.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/my-space');
  const row = page.locator('.my-space__row', { hasText: 'a-garder.png' });
  await expect(row).toBeVisible();

  await row.getByRole('button', { name: 'Supprimer' }).click();
  await row.getByRole('button', { name: 'Annuler' }).click();

  await expect(row).toBeVisible();
  await expect(row.getByRole('button', { name: 'Supprimer' })).toBeVisible();

  await page.reload();
  await expect(page.locator('.my-space__row', { hasText: 'a-garder.png' })).toBeVisible();
});
