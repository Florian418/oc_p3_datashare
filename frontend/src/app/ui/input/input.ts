import { Component, input, model } from '@angular/core';

export type InputType = 'text' | 'email' | 'password';

/**
 * Champ de saisie du design system — wrapper autour d'un `<input>` natif.
 */
@Component({
  selector: 'app-input',
  template: `
    <input
      class="input"
      [type]="type()"
      [placeholder]="placeholder()"
      [disabled]="disabled()"
      [attr.aria-invalid]="invalid() || null"
      [value]="value()"
      (input)="value.set($any($event.target).value)"
    />
  `,
  styles: `
    .input {
      width: 100%;
      min-width: 240px;
      box-sizing: border-box;
      padding: 12px 16px;
      border-radius: 8px;
      border: 1px solid var(--color-input-border);
      background-color: var(--color-input-bg);
      font-family: var(--font-family-base);
      font-size: 16px;
      line-height: 16px;
    }

    .input::placeholder {
      color: var(--color-input-placeholder);
    }

    .input:disabled {
      cursor: not-allowed;
      background-color: var(--color-disabled-bg);
      border-color: var(--color-disabled-bg);
      color: var(--color-disabled-text);
    }

    .input:disabled::placeholder {
      color: var(--color-disabled-text);
    }
  `,
})
export class Input {
  type = input<InputType>('text');
  placeholder = input('');
  disabled = input(false);
  /** Pose uniquement `aria-invalid` (sémantique) — pas de style d'erreur visuel dans le design
   * system, chaque écran gère le sien selon son contexte. */
  invalid = input(false);
  value = model('');
}
