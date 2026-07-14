import { Page } from '@playwright/test';

const STORAGE_KEY = 'datashare.auth';

/**
 * Simule une session authentifiée (localStorage) avant le chargement de la page, pour
 * atteindre une route protégée par une garde d'authentification sans dépendre du backend réel.
 */
export async function seedAuthSession(page: Page): Promise<void> {
  await page.addInitScript(
    ({ key, session }) => localStorage.setItem(key, JSON.stringify(session)),
    { key: STORAGE_KEY, session: { token: 'e2e-fake-token', expiresAt: new Date(Date.now() + 3600_000).toISOString() } },
  );
}
