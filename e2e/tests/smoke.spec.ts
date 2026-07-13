import { test, expect } from '@playwright/test';

test('l\'application se charge', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle('DataShare');
  await expect(page.locator('app-root')).toBeVisible();
});
