import { Component, input } from '@angular/core';
import { Button } from '../button/button';

@Component({
  selector: 'app-header',
  imports: [Button],
  template: `
    <header class="header">
      <a class="header__logo" href="/">DataShare</a>
      @if (loggedIn()) {
        <app-button variant="dark" size="medium">Mon espace</app-button>
      } @else {
        <app-button variant="dark" size="medium" routerLink="/login">Se connecter</app-button>
      }
    </header>
  `,
  styles: `
    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 72px;
      padding: 0 16px;
      max-width: 1280px;
      margin: 0 auto;
    }

    .header__logo {
      font-family: var(--font-family-base);
      font-weight: 700;
      font-size: 32px;
      line-height: 40px;
      color: #000000;
      text-decoration: none;
    }
  `,
})
export class Header {
  loggedIn = input(false);
}
