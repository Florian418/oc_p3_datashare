import { Component, input, model } from '@angular/core';

export interface SwitchOption {
  value: string;
  label: string;
}

/**
 * Groupe de filtres à onglets — `role="group"` (pas `radiogroup`) : choix assumé de ne pas
 * promettre une navigation clavier par flèches qui n'est pas implémentée.
 */
@Component({
  selector: 'app-switch',
  template: `
    <div class="switch" role="group" [attr.aria-label]="ariaLabel() || null">
      @for (option of options(); track option.value) {
        <button
          type="button"
          class="switch__tab"
          [class.switch__tab--selected]="option.value === value()"
          [attr.aria-pressed]="option.value === value()"
          (click)="value.set(option.value)"
        >
          {{ option.label }}
        </button>
      }
    </div>
  `,
  styles: `
    .switch {
      display: inline-flex;
      background-color: var(--color-switch-bg);
      border: 1px solid var(--color-switch-border);
      border-radius: 999px;
    }

    .switch__tab {
      appearance: none;
      border: none;
      background: transparent;
      padding: 8px 16px;
      font-family: var(--font-family-base);
      font-size: 16px;
      line-height: 16px;
      color: var(--color-switch-text);
      white-space: nowrap;
      cursor: pointer;
    }

    .switch__tab:first-child {
      border-radius: 999px 0 0 999px;
    }

    .switch__tab:last-child {
      border-radius: 0 999px 999px 0;
    }

    .switch__tab--selected {
      background-color: var(--color-switch-selected-bg);
      color: var(--color-switch-selected-text);
    }
  `,
})
export class Switch {
  options = input.required<SwitchOption[]>();
  ariaLabel = input('');
  value = model('');
}
