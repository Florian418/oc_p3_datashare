import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Header } from '../../ui/header/header';
import { Footer } from '../../ui/footer/footer';
import { Button } from '../../ui/button/button';
import { Input } from '../../ui/input/input';
import { Callout } from '../../ui/callout/callout';
import { Auth } from '../../auth/auth';

/**
 * Écran Connexion (US04) — authentifie via `POST /auth/login` et démarre la session.
 */
@Component({
  selector: 'app-login',
  imports: [Header, Footer, Button, Input, Callout],
  template: `
    <div class="auth">
      <app-header />

      <div class="auth__stage">
        <div class="auth__card">
          <h1 class="auth__title">Connexion</h1>

          @if (errorMessage(); as message) {
            <app-callout type="error">{{ message }}</app-callout>
          } @else if (accountCreated()) {
            <app-callout type="info">Votre compte a bien été créé, vous pouvez vous connecter.</app-callout>
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

            <div class="auth__actions">
              <app-button variant="tertiary" size="medium" [fullWidth]="true" routerLink="/register">
                Créer un compte
              </app-button>
              <app-button variant="primary" size="medium" type="submit" [fullWidth]="true" [disabled]="submitting()">
                Connexion
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
export class Login {
  private auth = inject(Auth);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  protected email = signal('');
  protected password = signal('');
  protected errorMessage = signal<string | null>(null);
  protected submitting = signal(false);
  protected accountCreated = signal(this.route.snapshot.queryParamMap.get('registered') === 'true');

  protected onSubmit(event: Event): void {
    event.preventDefault();
    this.errorMessage.set(null);
    this.submitting.set(true);

    this.auth.login({ email: this.email(), password: this.password() }).subscribe({
      next: () => this.router.navigateByUrl('/my-space'),
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(error.error?.detail ?? 'Une erreur est survenue, réessayez.');
      },
    });
  }
}
