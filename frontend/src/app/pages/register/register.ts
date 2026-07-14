import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Header } from '../../ui/header/header';
import { Footer } from '../../ui/footer/footer';
import { Button } from '../../ui/button/button';
import { Input } from '../../ui/input/input';
import { Callout } from '../../ui/callout/callout';
import { Auth } from '../../auth/auth';

const MIN_PASSWORD_LENGTH = 8;

/**
 * Écran Créer un compte (US03) — crée le compte via `POST /auth/register`, puis renvoie vers
 * la connexion (l'endpoint d'inscription ne délivre pas de token).
 */
@Component({
  selector: 'app-register',
  imports: [Header, Footer, Button, Input, Callout],
  template: `
    <div class="auth">
      <app-header />

      <div class="auth__stage">
        <div class="auth__card">
          <h1 class="auth__title">Créer un compte</h1>

          @if (errorMessage(); as message) {
            <app-callout type="error">{{ message }}</app-callout>
          }

          <form class="auth__form" (submit)="onSubmit($event)">
            <label class="auth__field">
              <span class="auth__field-label">Email</span>
              <app-input type="email" placeholder="Saisissez votre email..." [(value)]="email" />
            </label>
            <label class="auth__field">
              <span class="auth__field-label">Mot de passe</span>
              <app-input type="password" placeholder="Saisissez votre mot de passe..." [(value)]="password" />
            </label>
            <label class="auth__field">
              <span class="auth__field-label">Vérification du mot de passe</span>
              <app-input type="password" placeholder="Saisissez le à nouveau" [(value)]="passwordConfirmation" />
            </label>

            <div class="auth__actions">
              <app-button variant="tertiary" size="medium" [fullWidth]="true" routerLink="/login">
                J'ai déjà un compte
              </app-button>
              <app-button variant="primary" size="medium" type="submit" [fullWidth]="true" [disabled]="submitting()">
                Créer mon compte
              </app-button>
            </div>
          </form>
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
export class Register {
  private auth = inject(Auth);
  private router = inject(Router);

  protected email = signal('');
  protected password = signal('');
  protected passwordConfirmation = signal('');
  protected errorMessage = signal<string | null>(null);
  protected submitting = signal(false);

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.errorMessage.set(null);

    if (this.password().length < MIN_PASSWORD_LENGTH) {
      this.errorMessage.set(`Le mot de passe doit contenir au moins ${MIN_PASSWORD_LENGTH} caractères.`);
      return;
    }

    if (this.password() !== this.passwordConfirmation()) {
      this.errorMessage.set('Les deux mots de passe ne correspondent pas.');
      return;
    }

    this.submitting.set(true);

    this.auth.register({ email: this.email(), password: this.password() }).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registered: 'true' } }),
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(error.error?.detail ?? 'Une erreur est survenue, réessayez.');
      },
    });
  }
}
