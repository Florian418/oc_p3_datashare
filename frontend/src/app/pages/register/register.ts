import { Component } from '@angular/core';
import { Header } from '../../ui/header/header';
import { Footer } from '../../ui/footer/footer';
import { Button } from '../../ui/button/button';
import { Input } from '../../ui/input/input';

/**
 * Écran Créer un compte (US03) — statique pour l'instant, pas encore câblé sur l'API réelle.
 */
@Component({
  selector: 'app-register',
  imports: [Header, Footer, Button, Input],
  template: `
    <div class="auth">
      <app-header />

      <div class="auth__stage">
        <div class="auth__card">
          <h1 class="auth__title">Créer un compte</h1>

          <form class="auth__form">
            <label class="auth__field">
              <span class="auth__field-label">Email</span>
              <app-input type="email" placeholder="Saisissez votre email..." />
            </label>
            <label class="auth__field">
              <span class="auth__field-label">Mot de passe</span>
              <app-input type="password" placeholder="Saisissez votre mot de passe..." />
            </label>
            <label class="auth__field">
              <span class="auth__field-label">Vérification du mot de passe</span>
              <app-input type="password" placeholder="Saisissez le à nouveau" />
            </label>
          </form>

          <div class="auth__actions">
            <app-button variant="tertiary" size="medium" [fullWidth]="true" routerLink="/login">
              J'ai déjà un compte
            </app-button>
            <app-button variant="primary" size="medium" type="submit" [fullWidth]="true">
              Créer mon compte
            </app-button>
          </div>
        </div>
      </div>

      <app-footer />
    </div>
  `,
  styles: `
    .auth {
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      background: linear-gradient(164.76deg, #ffb88c 2.29%, #de6262 97.71%);
    }

    .auth__stage {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }

    .auth__card {
      width: 100%;
      max-width: 640px;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      gap: 24px;
      padding: 24px;
      background-color: #ffffff;
      box-shadow: 0 0 12px 0 rgb(0 0 0 / 25%);
      border-radius: 16px;
    }

    .auth__title {
      margin: 0;
      width: 100%;
      text-align: center;
      font-family: var(--font-family-base);
      font-weight: 400;
      font-size: 32px;
      line-height: 40px;
      color: #000000;
    }

    .auth__form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .auth__field {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .auth__field-label {
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: var(--color-select-text);
    }

    .auth__actions {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
  `,
})
export class Register {}
