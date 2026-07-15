import { Component } from '@angular/core';
import { Header } from '../../ui/header/header';
import { Footer } from '../../ui/footer/footer';
import { Button } from '../../ui/button/button';

/**
 * Écran affiché pour toute route inconnue (route wildcard `**`) — pas d'écran dédié dans la
 * maquette, réutilise le pattern de carte déjà établi (Login/Register/Téléchargement).
 */
@Component({
  selector: 'app-not-found',
  imports: [Header, Footer, Button],
  template: `
    <div class="not-found">
      <app-header />

      <div class="not-found__stage">
        <div class="not-found__card">
          <p class="not-found__code" aria-hidden="true">404</p>
          <h1 class="not-found__title">Page introuvable</h1>
          <p class="not-found__message">Cette page n'existe pas ou plus.</p>
          <app-button variant="primary" size="medium" [fullWidth]="true" routerLink="/">Retour à l'accueil</app-button>
        </div>
      </div>

      <app-footer />
    </div>
  `,
  styles: `
    .not-found {
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      background: linear-gradient(164.76deg, #ffb88c 2.29%, #de6262 97.71%);
    }

    .not-found__stage {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }

    .not-found__card {
      width: 100%;
      max-width: 640px;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 40px 24px;
      background-color: #ffffff;
      box-shadow: 0 0 12px 0 rgb(0 0 0 / 25%);
      border-radius: 16px;
    }

    .not-found__code {
      margin: 0;
      font-family: var(--font-family-base);
      font-weight: 700;
      font-size: 80px;
      line-height: 1;
      color: var(--color-primary);
    }

    .not-found__title {
      margin: 0;
      text-align: center;
      font-family: var(--font-family-base);
      font-weight: 700;
      font-size: 28px;
      line-height: 40px;
      color: #000000;
    }

    .not-found__message {
      margin: 0 0 16px;
      text-align: center;
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #000000;
    }
  `,
})
export class NotFound {}
