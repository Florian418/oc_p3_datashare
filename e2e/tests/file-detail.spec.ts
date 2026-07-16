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

test('Le bouton Accéder de Mon espace ouvre le détail du fichier', async ({ page }) => {
  const email = `e2e_detail_${crypto.randomUUID()}@example.com`;
  await registerAndLogin(page, email, 'secret123');

  await page.goto('/');
  await selectFile(page, { name: 'detail.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/my-space');
  const row = page.locator('.my-space__row', { hasText: 'detail.png' });
  await row.locator('.my-space__row-buttons').getByRole('link', { name: 'Accéder' }).click();

  await expect(page.getByRole('heading', { name: 'Détail du fichier' })).toBeVisible();
  await expect(page.getByText('detail.png')).toBeVisible();
});

test('Ajout et retrait d\'un tag depuis le détail du fichier', async ({ page }) => {
  const email = `e2e_tags_${crypto.randomUUID()}@example.com`;
  await registerAndLogin(page, email, 'secret123');

  await page.goto('/');
  await selectFile(page, { name: 'tags.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/my-space');
  const row = page.locator('.my-space__row', { hasText: 'tags.png' });
  await row.locator('.my-space__row-buttons').getByRole('link', { name: 'Accéder' }).click();
  await expect(page.getByRole('heading', { name: 'Détail du fichier' })).toBeVisible();

  await expect(page.getByText('Aucun tag pour l\'instant.')).toBeVisible();

  const tagChip = page.locator('.file-detail__tag', { hasText: 'vacances' });

  await page.getByLabel('Nouveau tag').fill('vacances');
  await page.getByRole('button', { name: 'Ajouter' }).click();
  await expect(tagChip).toBeVisible();
  await expect(page.getByText('Aucun tag pour l\'instant.')).toHaveCount(0);

  await page.getByRole('button', { name: 'Retirer le tag vacances' }).click();
  await expect(tagChip).toHaveCount(0);
  await expect(page.getByText('Aucun tag pour l\'instant.')).toBeVisible();
});

test('Le bouton Accéder au fichier mène au vrai téléchargement', async ({ page }) => {
  const email = `e2e_detail_dl_${crypto.randomUUID()}@example.com`;
  await registerAndLogin(page, email, 'secret123');

  await page.goto('/');
  await selectFile(page, { name: 'via-detail.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();

  await page.goto('/my-space');
  const row = page.locator('.my-space__row', { hasText: 'via-detail.png' });
  await row.locator('.my-space__row-buttons').getByRole('link', { name: 'Accéder' }).click();

  await page.getByRole('link', { name: 'Accéder au fichier' }).click();
  await expect(page).toHaveURL(/\/download\//);

  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Télécharger' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe('via-detail.png');
});
