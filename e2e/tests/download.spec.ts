import { test, expect } from '@playwright/test';

const PNG_BUFFER = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0x0d, 0x49, 0x48, 0x44, 0x52]);

async function uploadFile(page: import('@playwright/test').Page, filename: string, password?: string) {
  await page.goto('/');
  const fileChooserPromise = page.waitForEvent('filechooser');
  await page.getByRole('button', { name: 'Choisir un fichier à partager' }).click();
  const chooser = await fileChooserPromise;
  await chooser.setFiles({ name: filename, mimeType: 'image/png', buffer: PNG_BUFFER });
  if (password) {
    await page.locator('input[type=password]').fill(password);
  }
  await page.getByRole('button', { name: 'Téléverser' }).click();
  await expect(page.getByText('Félicitations')).toBeVisible();
  return page.locator('.upload__link a').textContent();
}

test('Téléchargement d\'un fichier libre, de bout en bout', async ({ page }) => {
  const link = await uploadFile(page, 'photo.png');

  await page.goto(link!);
  await expect(page.getByRole('heading', { name: 'Télécharger un fichier' })).toBeVisible();
  await expect(page.locator('.download__file-name')).toHaveText('photo.png');
  await expect(page.getByText(/expirera/)).toBeVisible();

  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Télécharger' }).click();
  const download = await downloadPromise;

  expect(download.suggestedFilename()).toBe('photo.png');
});

test('Téléchargement d\'un fichier protégé : mauvais mot de passe puis bon mot de passe', async ({ page }) => {
  const password = 'secret123';
  const link = await uploadFile(page, 'secret.png', password);

  await page.goto(link!);
  await expect(page.locator('input[type=password]')).toBeVisible();

  await page.locator('input[type=password]').fill('wrong-password');
  await page.getByRole('button', { name: 'Télécharger' }).click();
  await expect(page.getByText('invalide')).toBeVisible();

  await page.locator('input[type=password]').fill(password);
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Télécharger' }).click();
  const download = await downloadPromise;

  expect(download.suggestedFilename()).toBe('secret.png');
});

test('Un lien inconnu affiche un message d\'indisponibilité', async ({ page }) => {
  await page.goto('/download/00000000-0000-0000-0000-000000000000');

  await expect(page.getByText("n'est plus disponible")).toBeVisible();
  await expect(page.getByRole('button', { name: 'Télécharger' })).toHaveCount(0);
});
