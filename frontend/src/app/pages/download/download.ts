import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Header } from '../../ui/header/header';
import { Footer } from '../../ui/footer/footer';
import { Button } from '../../ui/button/button';
import { Input } from '../../ui/input/input';
import { Callout } from '../../ui/callout/callout';
import { Share, ShareMetadata } from '../../fileshare/share';

type DownloadState = 'chargement' | 'protege' | 'libre' | 'expire';

function formatFileSize(bytes: number): string {
  const units = ['octets', 'Ko', 'Mo', 'Go'];
  let value = bytes;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex++;
  }
  const formatted = unitIndex === 0 ? `${value}` : value.toFixed(1).replace('.', ',');
  return `${formatted} ${units[unitIndex]}`;
}

/**
 * Écran Téléchargement (US02/US09) — lit le token dans l'URL, charge les vraies métadonnées
 * (`GET /shares/{token}`), puis bascule entre 3 états structurels (protégé/libre/expiré).
 * Le lien invalide/inconnu (404) partage le même état visuel que le lien expiré (410) — la
 * maquette n'a pas d'écran dédié pour ce cas.
 */
@Component({
  selector: 'app-download',
  imports: [Header, Footer, Button, Input, Callout],
  template: `
    <div class="download">
      <app-header />

      <div class="download__stage">
        <div class="download__card">
          <h1 class="download__title">Télécharger un fichier</h1>

          <div class="download__body">
            @switch (state()) {
              @case ('chargement') {
                <p>Chargement...</p>
              }
              @case ('expire') {
                <app-callout type="error">Ce fichier n'est plus disponible en téléchargement car il a expiré.</app-callout>
              }
              @default {
                <div class="download__file">
                  <svg class="download__file-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M0 0.9918C0 0.44405 0.44749 0 0.9985 0L13 0L17.9997 5L18 18.9925C18 19.5489 17.5551 20 17.0066 20L0.9934 20C0.44476 20 0 19.5447 0 19.0082L0 0.9918L0 0.9918ZM12 6L12 2L2 2L2 18L16 18L16 6L12 6L12 6ZM8 7.5C8 8.3284 7.3284 9 6.5 9C5.67157 9 5 8.3284 5 7.5C5 6.67157 5.67157 6 6.5 6C7.3284 6 8 6.67157 8 7.5L8 7.5ZM14.5 15L10.5 8L5 15L14.5 15L14.5 15Z" fill="#000000" fill-rule="evenodd" transform="translate(3 2)" />
                  </svg>
                  <div class="download__file-info">
                    <span class="download__file-name">{{ fileName() }}</span>
                    <span class="download__file-size">{{ fileSize() }}</span>
                  </div>
                </div>

                <app-callout [type]="expiryCalloutType()">{{ expiryMessage() }}</app-callout>

                @if (state() === 'protege') {
                  <label class="download__field">
                    <span class="download__field-label">Mot de passe</span>
                    <app-input type="password" placeholder="Saisissez le mot de passe..." [(value)]="password" />
                  </label>
                }

                @if (apiErrorMessage(); as message) {
                  <app-callout type="error">{{ message }}</app-callout>
                }
              }
            }
          </div>

          @if (state() === 'protege' || state() === 'libre') {
            <app-button
              variant="primary"
              size="medium"
              [fullWidth]="true"
              [disabled]="protectedAndEmpty() || submitting()"
              (click)="onDownloadClick()"
            >
              <svg slot="icon" aria-hidden="true" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M8.61133 0.522871C7.76268 0.148042 6.84005 -0.0293788 5.91291 0.00396228C4.98577 0.0373042 4.07827 0.280541 3.25873 0.71536C2.4392 1.15018 1.72898 1.76525 1.18155 2.51427C0.634123 3.26329 0.263747 4.12674 0.0983071 5.03961C-0.0671319 5.95248 -0.0233249 6.89099 0.226432 7.78448C0.476189 8.67797 0.925386 9.50317 1.54021 10.1979C1.83301 10.5288 2.3386 10.5597 2.66948 10.2669C3.00035 9.97407 3.03122 9.46848 2.73841 9.1376C2.28398 8.62408 1.95197 8.01416 1.76736 7.35375C1.58276 6.69334 1.55038 5.99966 1.67266 5.32493C1.79494 4.6502 2.0687 4.01199 2.47332 3.45837C2.87794 2.90475 3.40288 2.45013 4.00863 2.12874C4.61437 1.80736 5.28513 1.62757 5.97041 1.60293C6.65569 1.57828 7.33763 1.70942 7.9649 1.98647C8.59216 2.26352 9.14841 2.67925 9.59175 3.20238C10.0351 3.72551 10.354 4.3424 10.5244 5.0066C10.6152 5.36039 10.9341 5.60777 11.2993 5.60777L12.1399 5.60777C12.6784 5.60735 13.2031 5.77853 13.6377 6.09648C14.0723 6.41443 14.3943 6.8626 14.5569 7.37595C14.7196 7.8893 14.7144 8.44112 14.5422 8.95134C14.37 9.46155 14.0396 9.90361 13.5991 10.2134C13.2377 10.4675 13.1508 10.9665 13.4049 11.3279C13.6591 11.6894 14.1581 11.7763 14.5195 11.5222C15.2382 11.0168 15.7771 10.2955 16.0581 9.46306C16.3392 8.63061 16.3476 7.73026 16.0822 6.89269C15.8168 6.05512 15.2915 5.3239 14.5824 4.80514C13.8735 4.2865 13.0177 4.00721 12.1393 4.00777L11.8866 4.00777C11.6391 3.33803 11.2761 2.71508 10.8124 2.16793C10.2126 1.46017 9.45999 0.897699 8.61133 0.522871L8.61133 0.522871ZM8.93931 6.80777C8.93931 6.36594 8.58114 6.00777 8.13931 6.00777C7.69748 6.00777 7.33931 6.36594 7.33931 6.80777L7.33931 10.8764L6.03833 9.57542C5.72591 9.263 5.21938 9.263 4.90696 9.57542C4.59454 9.88784 4.59454 10.3944 4.90696 10.7068L7.57362 13.3735C7.72365 13.5235 7.92714 13.6078 8.13931 13.6078C8.35148 13.6078 8.55497 13.5235 8.705 13.3735L11.3717 10.7068C11.6841 10.3944 11.6841 9.88784 11.3717 9.57542C11.0592 9.263 10.5527 9.263 10.2403 9.57542L8.93931 10.8764L8.93931 6.80777L8.93931 6.80777Z" fill="currentColor" fill-rule="evenodd" transform="translate(-0.139 1.192)" />
              </svg>
              {{ submitting() ? 'Vérification...' : 'Télécharger' }}
            </app-button>
          }
        </div>
      </div>

      <app-footer />
    </div>
  `,
  styles: `
    .download {
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      background: linear-gradient(164.76deg, #ffb88c 2.29%, #de6262 97.71%);
    }

    .download__stage {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }

    .download__card {
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

    .download__title {
      margin: 0;
      width: 100%;
      text-align: center;
      font-family: var(--font-family-base);
      font-weight: 700;
      font-size: 28px;
      line-height: 40px;
      color: #000000;
    }

    .download__body {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .download__file {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .download__file-icon {
      flex-shrink: 0;
      width: 24px;
      height: 24px;
    }

    .download__file-info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }

    .download__file-name {
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #000000;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .download__file-size {
      font-family: var(--font-family-sans);
      font-size: 14px;
      line-height: 16px;
      color: #000000;
    }

    .download__field {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .download__field-label {
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: var(--color-select-text);
    }
  `,
})
export class Download {
  private route = inject(ActivatedRoute);
  private share = inject(Share);

  private token = this.route.snapshot.paramMap.get('token') ?? '';

  protected state = signal<DownloadState>('chargement');
  protected metadata = signal<ShareMetadata | null>(null);
  protected fileName = signal('');
  protected fileSize = signal('');
  protected password = signal('');
  protected submitting = signal(false);
  protected apiErrorMessage = signal<string | null>(null);

  protected protectedAndEmpty = computed(() => this.state() === 'protege' && !this.password());

  protected expiresInDays = computed(() => {
    const meta = this.metadata();
    if (!meta) {
      return 0;
    }
    const ms = new Date(meta.expiresAt).getTime() - Date.now();
    return Math.max(1, Math.ceil(ms / (1000 * 60 * 60 * 24)));
  });

  protected expiryCalloutType = computed(() => (this.expiresInDays() <= 1 ? 'alert' : 'info'));

  protected expiryMessage = computed(() =>
    this.expiresInDays() <= 1
      ? 'Ce fichier expirera demain.'
      : `Ce fichier expirera dans ${this.expiresInDays()} jours.`,
  );

  constructor() {
    if (!this.token) {
      this.state.set('expire');
      return;
    }

    this.share.getMetadata(this.token).subscribe({
      next: (meta) => {
        this.metadata.set(meta);
        this.fileName.set(meta.name);
        this.fileSize.set(formatFileSize(meta.size));
        this.state.set(meta.passwordProtected ? 'protege' : 'libre');
      },
      error: () => this.state.set('expire'),
    });
  }

  protected onDownloadClick(): void {
    if (this.state() === 'libre') {
      window.location.href = this.share.downloadUrl(this.token);
      return;
    }

    this.apiErrorMessage.set(null);
    this.submitting.set(true);

    this.share.authenticate(this.token, this.password()).subscribe({
      next: (response) => {
        this.submitting.set(false);
        window.location.href = this.share.downloadUrl(this.token, response.accessToken);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.apiErrorMessage.set(error.error?.detail ?? 'Une erreur est survenue, réessayez.');
      },
    });
  }
}
