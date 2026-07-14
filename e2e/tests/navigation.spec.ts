import { test, expect } from '@playwright/test';
import { seedAuthSession } from './auth-session';

test('Téléversement → Connexion → Créer un compte → Connexion', async ({ page }) => {
  await page.goto('/');

  await page.getByRole('link', { name: 'Se connecter' }).click();
  await expect(page).toHaveURL('/login');
  await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();

  await page.getByRole('link', { name: 'Créer un compte' }).click();
  await expect(page).toHaveURL('/register');
  await expect(page.getByRole('heading', { name: 'Créer un compte' })).toBeVisible();

  await page.getByRole('link', { name: "J'ai déjà un compte" }).click();
  await expect(page).toHaveURL('/login');
  await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
});

test('Mon espace → Ajouter des fichiers → Téléversement', async ({ page }) => {
  await seedAuthSession(page);
  await page.goto('/my-space');

  await page.getByRole('link', { name: 'Ajouter des fichiers' }).click();
  await expect(page).toHaveURL('/');
});
