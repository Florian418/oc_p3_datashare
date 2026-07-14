import { Component, input, model } from '@angular/core';

export interface SelectOption {
  value: string;
  label: string;
}

/**
 * Liste déroulante du design system — `<select>` natif stylisé (pas de combobox custom ARIA) :
 * l'accessibilité/clavier est garantie gratuitement par le navigateur, seule limite acceptée
 * est un panneau d'options natif OS (pas pixel-perfect avec la maquette).
 */
@Component({
  selector: 'app-select',
  template: `
    <div class="select">
      <select
        [disabled]="disabled()"
        [attr.aria-invalid]="invalid() || null"
        [value]="value()"
        (change)="value.set($any($event.target).value)"
      >
        @for (option of options(); track option.value) {
          <option [value]="option.value">{{ option.label }}</option>
        }
      </select>
      <svg class="select__chevron" aria-hidden="true" width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M1.36569 0.234315C1.05327 -0.078105 0.546734 -0.078105 0.234315 0.234315C-0.078105 0.546734 -0.078105 1.05327 0.234315 1.36568L4.23431 5.36569C4.54673 5.67811 5.05327 5.67811 5.36569 5.36569L9.36569 1.36568C9.67811 1.05327 9.67811 0.546734 9.36569 0.234315C9.05327 -0.078105 8.54673 -0.078105 8.23431 0.234315L4.8 3.66862L1.36569 0.234315L1.36569 0.234315Z" fill="currentColor" fill-rule="evenodd" transform="translate(3.2 5.2)" /></svg>
    </div>
  `,
  styles: `
    .select {
      position: relative;
      width: 100%;
      min-width: 240px;
    }

    select {
      width: 100%;
      box-sizing: border-box;
      appearance: none;
      -webkit-appearance: none;
      -moz-appearance: none;
      padding: 12px 36px 12px 16px;
      border-radius: 8px;
      border: 1px solid var(--color-input-border);
      background-color: var(--color-input-bg);
      font-family: var(--font-family-base);
      font-size: 16px;
      line-height: 16px;
      color: var(--color-select-text);
      cursor: pointer;
    }

    select:disabled {
      cursor: not-allowed;
      background-color: var(--color-disabled-bg);
      border-color: var(--color-disabled-bg);
      color: var(--color-disabled-text);
    }

    .select__chevron {
      position: absolute;
      top: 50%;
      right: 12px;
      transform: translateY(-50%);
      color: var(--color-select-text);
      pointer-events: none;
    }

    select:disabled ~ .select__chevron {
      color: var(--color-disabled-text);
    }
  `,
})
export class Select {
  options = input.required<SelectOption[]>();
  disabled = input(false);
  invalid = input(false);
  value = model('');
}
