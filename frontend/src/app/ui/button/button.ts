import { Component, computed, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';

export type ButtonVariant = 'primary' | 'secondary' | 'tertiary' | 'dark';
export type ButtonSize = 'small' | 'medium';

@Component({
  selector: 'app-button',
  imports: [NgTemplateOutlet, RouterLink],
  template: `
    <ng-template #content>
      <span class="btn__icon" aria-hidden="true">
        <ng-content select="[slot=icon]" />
      </span>
      <span class="btn__label">
        <ng-content />
      </span>
    </ng-template>

    @if (routerLink(); as link) {
      <a [routerLink]="link" [class]="classes()">
        <ng-container [ngTemplateOutlet]="content" />
      </a>
    } @else {
      <button [type]="type()" [class]="classes()" [disabled]="disabled()">
        <ng-container [ngTemplateOutlet]="content" />
      </button>
    }
  `,
  styles: `
    :host {
      display: contents;
    }

    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      gap: 8px;
      border-radius: 8px;
      border: 1px solid transparent;
      font-family: var(--font-family-base);
      font-size: 16px;
      font-weight: 400;
      white-space: nowrap;
      text-decoration: none;
      cursor: pointer;
    }

    .btn--full-width {
      display: flex;
      width: 100%;
    }

    .btn--small {
      padding: 8px 12px;
    }

    .btn--medium {
      padding: 12px;
    }

    .btn--primary {
      background-color: var(--color-primary-bg);
      border-color: var(--color-primary-border);
      color: var(--color-primary-text);
    }

    .btn--secondary {
      background-color: transparent;
      border-color: var(--color-secondary-border);
      color: var(--color-secondary-text);
    }

    .btn--tertiary {
      background-color: transparent;
      color: var(--color-secondary-text);
    }

    .btn--dark {
      background-color: var(--color-dark-bg);
      color: var(--color-dark-text);
    }

    .btn:disabled {
      cursor: not-allowed;
    }

    .btn--primary:disabled {
      background-color: var(--color-disabled-bg);
      border-color: var(--color-disabled-bg);
      color: var(--color-disabled-text);
    }

    .btn--secondary:disabled {
      border-color: var(--color-disabled-text);
      color: var(--color-disabled-text);
    }

    .btn--tertiary:disabled {
      color: var(--color-disabled-text);
    }

    .btn--dark:disabled {
      background-color: var(--color-disabled-dark-bg);
      color: var(--color-disabled-text);
    }

    .btn__icon {
      display: inline-flex;
      width: 16px;
      height: 16px;
    }

    .btn__icon:empty {
      display: none;
    }
  `,
})
export class Button {
  variant = input<ButtonVariant>('primary');
  size = input<ButtonSize>('medium');
  type = input<'button' | 'submit'>('button');
  disabled = input(false);
  fullWidth = input(false);
  routerLink = input<string | undefined>(undefined);

  protected classes = computed(
    () => `btn btn--${this.variant()} btn--${this.size()}${this.fullWidth() ? ' btn--full-width' : ''}`,
  );
}
