import { test, expect } from '@playwright/test';

const PNG_BUFFER = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0x0d, 0x49, 0x48, 0x44, 0x52]);
// en-tête DOS/PE ("MZ") — vérifie que le serveur rejette un exécutable même envoyé sous une
// apparence inoffensive (nom de fichier .txt), le contenu réel prime toujours.
const PE_HEADER_BUFFER = Buffer.from([0x4d, 0x5a, 0, 0, 0x03, 0, 0, 0, 0x04, 0, 0, 0]);

async function selectFile(page: import('@playwright/test').Page, file: { name: string; mimeType: string; buffer: Buffer }) {
  const fileChooserPromise = page.waitForEvent('filechooser');
  await page.getByRole('button', { name: 'Choisir un fichier à partager' }).click();
  const chooser = await fileChooserPromise;
  await chooser.setFiles(file);
}

test('Téléversement anonyme, de bout en bout', async ({ page }) => {
  await page.goto('/');

  await selectFile(page, { name: 'photo.png', mimeType: 'image/png', buffer: PNG_BUFFER });

  await expect(page.getByRole('heading', { name: 'Ajouter un fichier' })).toBeVisible();
  await expect(page.locator('.upload__file-name')).toHaveText('photo.png');

  await page.getByRole('button', { name: 'Téléverser' }).click();

  await expect(page.getByText('Félicitations')).toBeVisible();
  await expect(page.locator('.upload__link a')).toHaveAttribute('href', /\/download\//);
});

test('Téléversement lié au compte quand connecté', async ({ page }) => {
  const email = `e2e_upload_${crypto.randomUUID()}@example.com`;
  const password = 'secret123';

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

  await page.goto('/');
  await selectFile(page, { name: 'photo.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();

  await expect(page.getByText('Félicitations')).toBeVisible();
});

test('La durée d\'expiration choisie est reflétée dans le message de succès', async ({ page }) => {
  await page.goto('/');

  await selectFile(page, { name: 'photo.png', mimeType: 'image/png', buffer: PNG_BUFFER });
  await page.locator('select').selectOption('1j');
  await page.getByRole('button', { name: 'Téléverser' }).click();

  await expect(page.locator('.upload__success-text')).toHaveText(
    'Félicitations, ton fichier sera conservé chez nous pendant une journée !',
  );
});

test('Un exécutable déguisé est rejeté avec un message clair', async ({ page }) => {
  await page.goto('/');

  await selectFile(page, { name: 'notes.txt', mimeType: 'text/plain', buffer: PE_HEADER_BUFFER });
  await page.getByRole('button', { name: 'Téléverser' }).click();

  await expect(page.getByText('Type de fichier non autorisé')).toBeVisible();
});
