import { Component } from '@angular/core';

/**
 * Pied de page statique, visible uniquement en desktop (absent de la maquette mobile).
 */
@Component({
  selector: 'app-footer',
  template: `
    <footer class="footer">
      <div class="footer__inner">Copyright DataShare© 2025</div>
    </footer>
  `,
  styles: `
    .footer {
      display: none;
    }

    .footer__inner {
      box-sizing: border-box;
      max-width: 1280px;
      margin: 0 auto;
      padding: 16px;
      text-align: left;
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #ffffff;
    }

    @media (min-width: 768px) {
      .footer {
        display: block;
      }
    }
  `,
})
export class Footer {}
