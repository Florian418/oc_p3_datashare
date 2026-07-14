import { test, expect } from '@playwright/test';

test('Créer un compte → Connexion → Mon espace', async ({ page }) => {
  const email = `e2e_${crypto.randomUUID()}@example.com`;
  const password = 'secret123';

  await page.goto('/register');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mot de passe', { exact: true }).fill(password);
  await page.getByLabel('Vérification du mot de passe').fill(password);
  await page.getByRole('button', { name: 'Créer mon compte' }).click();
  await expect(page).toHaveURL('/login?registered=true');
  await expect(page.getByText('Votre compte a bien été créé')).toBeVisible();

  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mot de passe', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Connexion' }).click();
  await expect(page).toHaveURL('/my-space');
});

test('Connexion avec un mauvais mot de passe reste sur la page et affiche une erreur', async ({ page }) => {
  const email = `e2e_${crypto.randomUUID()}@example.com`;
  const password = 'secret123';

  await page.goto('/register');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mot de passe', { exact: true }).fill(password);
  await page.getByLabel('Vérification du mot de passe').fill(password);
  await page.getByRole('button', { name: 'Créer mon compte' }).click();
  await expect(page).toHaveURL('/login?registered=true');

  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mot de passe', { exact: true }).fill('mot-de-passe-incorrect');
  await page.getByRole('button', { name: 'Connexion' }).click();

  await expect(page.getByText('Email ou mot de passe invalide')).toBeVisible();
  await expect(page).toHaveURL(/\/login/);
});
