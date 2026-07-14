import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { seedAuthSession } from './auth-session';

const routes = ['/', '/login', '/register', '/download/00000000-0000-0000-0000-000000000000', '/my-space'];

const viewports = {
  desktop: { width: 1440, height: 1024 },
  mobile: { width: 393, height: 852 },
};

for (const route of routes) {
  for (const [viewportName, size] of Object.entries(viewports)) {
    test(`${route} — pas de violation a11y (${viewportName})`, async ({ page }) => {
      await page.setViewportSize(size);
      if (route === '/my-space') {
        await seedAuthSession(page);
      }
      await page.goto(route);

      const results = await new AxeBuilder({ page })
        // Contraste du texte Secondary/Tertiary (#e27f29 sur blanc) hors scope v1 : ADR-013.
        .disableRules(['color-contrast'])
        .analyze();

      expect(results.violations).toEqual([]);
    });
  }
}
