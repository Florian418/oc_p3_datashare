import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Header } from '../../ui/header/header';
import { Footer } from '../../ui/footer/footer';
import { Button } from '../../ui/button/button';
import { Input } from '../../ui/input/input';
import { Select, SelectOption } from '../../ui/select/select';
import { FileShare } from '../../fileshare/fileshare';

type UploadState = 'vide' | 'ajoute' | 'erreur' | 'succes';

const MAX_SIZE_BYTES = 1_073_741_824; // 1 Go (US01), même limite que le backend

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
 * Écran Téléversement (US01/US07) — 4 états visuels (vide/ajouté/erreur/succès) pilotés par
 * `state`, câblés sur `POST /files` réel.
 */
@Component({
  selector: 'app-upload',
  imports: [Header, Footer, Button, Input, Select],
  template: `
    <div class="upload">
      <app-header />

      <input
        #fileInput
        type="file"
        hidden
        (change)="onFileSelected($event)"
      />

      <div
        class="upload__stage"
        [class.upload__stage--drag-over]="dragHover()"
        (dragover)="onDragOver($event)"
        (dragleave)="onDragLeave($event)"
        (drop)="onDrop($event)"
      >
        @switch (state()) {
          @case ('vide') {
            <div class="upload__intro">
              <h1 class="upload__title">Tu veux partager un fichier ?</h1>
              <button type="button" class="upload__trigger" aria-label="Choisir un fichier à partager" (click)="fileInput.click()">
                <span class="upload__trigger-inner">
                  <svg class="upload__trigger-icon" aria-hidden="true" viewBox="0 0 48.004 39.995" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M25.2527 1.52583C22.7625 0.429537 20.0562 -0.0879734 17.3373 0.0122066C14.6183 0.112377 11.9574 0.827647 9.55464 2.10421C7.15189 3.38079 5.0698 5.18545 3.46489 7.38253C1.85999 9.57961 0.774051 12.1119 0.288701 14.7891C-0.196649 17.4663 -0.0687685 20.2187 0.662731 22.8393C1.39422 25.4599 2.71029 27.8807 4.512 29.9195C5.24343 30.7472 6.50736 30.8252 7.33505 30.0938C8.16274 29.3623 8.24077 28.0984 7.50933 27.2707C6.108 25.685 5.08439 23.8022 4.51545 21.7639C3.94651 19.7256 3.84705 17.5849 4.22454 15.5026C4.60204 13.4204 5.44666 11.4508 6.69492 9.74197C7.94317 8.03313 9.56258 6.6295 11.4314 5.63661C13.3002 4.64372 15.3698 4.08741 17.4845 4.00949C19.5993 3.93158 21.7042 4.33409 23.641 5.18676C25.5778 6.03944 27.2961 7.32009 28.6667 8.93244C30.0374 10.5448 31.0246 12.4469 31.5543 14.4957C31.7826 15.3784 32.5789 15.9951 33.4907 15.9951L36.0093 15.9951L36.0107 15.9951C37.7876 15.9966 39.5135 16.5896 40.916 17.6806C42.3189 18.7719 43.3185 20.2993 43.7569 22.0218C44.1953 23.7443 44.0476 25.5637 43.3371 27.1929C42.6267 28.8221 41.3939 30.1683 39.8333 31.0191C38.8635 31.5478 38.5059 32.7626 39.0347 33.7324C39.5634 34.7022 40.7782 35.0598 41.748 34.5311C44.0888 33.2549 45.938 31.2356 47.0037 28.7918C48.0694 26.348 48.2909 23.6188 47.6333 21.0351C46.9757 18.4514 45.4764 16.1603 43.372 14.5234C41.2676 12.8864 38.6781 11.9968 36.012 11.9951L34.9688 11.9951C34.2389 9.93226 33.1373 8.01554 31.7144 6.3417C29.9521 4.26869 27.7429 2.62213 25.2527 1.52583L25.2527 1.52583ZM25.4249 18.5809C24.6438 17.7998 23.3775 17.7998 22.5965 18.5809L14.5965 26.5809C13.8154 27.3619 13.8154 28.6283 14.5965 29.4093C15.3775 30.1903 16.6438 30.1903 17.4249 29.4093L22.0107 24.8235L22.0107 37.9951C22.0107 39.0997 22.9061 39.9951 24.0107 39.9951C25.1152 39.9951 26.0107 39.0997 26.0107 37.9951L26.0107 24.8235L30.5965 29.4093C31.3775 30.1903 32.6438 30.1903 33.4249 29.4093C34.2059 28.6283 34.2059 27.3619 33.4249 26.5809L25.4249 18.5809L25.4249 18.5809Z" fill="#FFEEEC" fill-rule="evenodd" />
                  </svg>
                </span>
              </button>
            </div>
          }
          @case ('ajoute') {
            <div class="upload__card">
              <h1 class="upload__card-title">Ajouter un fichier</h1>
              <div class="upload__file">
                <svg class="upload__file-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M0 0.9918C0 0.44405 0.44749 0 0.9985 0L13 0L17.9997 5L18 18.9925C18 19.5489 17.5551 20 17.0066 20L0.9934 20C0.44476 20 0 19.5447 0 19.0082L0 0.9918L0 0.9918ZM12 6L12 2L2 2L2 18L16 18L16 6L12 6L12 6ZM8 7.5C8 8.3284 7.3284 9 6.5 9C5.67157 9 5 8.3284 5 7.5C5 6.67157 5.67157 6 6.5 6C7.3284 6 8 6.67157 8 7.5L8 7.5ZM14.5 15L10.5 8L5 15L14.5 15L14.5 15Z" fill="#000000" fill-rule="evenodd" transform="translate(3 2)" />
                </svg>
                <div class="upload__file-info">
                  <span class="upload__file-name">{{ fileName() }}</span>
                  <span class="upload__file-size">{{ fileSize() }}</span>
                </div>
                <app-button variant="secondary" size="small" (click)="fileInput.click()">Changer</app-button>
              </div>

              @if (apiErrorMessage(); as message) {
                <p class="upload__error" role="alert">{{ message }}</p>
              }

              <form class="upload__form" (submit)="onSubmit($event)">
                <label class="upload__field">
                  <span class="upload__field-label">Mot de passe</span>
                  <app-input type="password" placeholder="Optionnel" [(value)]="password" />
                </label>
                <label class="upload__field">
                  <span class="upload__field-label">Expiration</span>
                  <app-select [options]="expirationOptions" [(value)]="expirationOption" />
                </label>
                <app-button variant="primary" size="medium" type="submit" [disabled]="submitting()">
                  <svg slot="icon" aria-hidden="true" viewBox="0 0 16.268 13.598" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M8.60463 0.519915C7.75612 0.146362 6.83398 -0.029976 5.90751 0.00415909C4.98104 0.0382941 4.07437 0.282013 3.25566 0.716993C2.43694 1.15197 1.72749 1.7669 1.18063 2.51553C0.633775 3.26416 0.26375 4.12703 0.0983721 5.03925C-0.0670059 5.95148 -0.0234319 6.88932 0.225817 7.78228C0.475067 8.67524 0.923506 9.50008 1.53742 10.1948C1.83 10.5259 2.33557 10.5571 2.66664 10.2645C2.99772 9.97192 3.02893 9.46635 2.73635 9.13527C2.28259 8.6218 1.95114 8.01213 1.76691 7.35212C1.58268 6.69211 1.55047 5.99892 1.67271 5.32466C1.79495 4.65041 2.06844 4.01264 2.47264 3.4593C2.87684 2.90597 3.40122 2.45146 4.00635 2.12995C4.61149 1.80844 5.28164 1.6283 5.96642 1.60307C6.6512 1.57784 7.33279 1.70818 7.95994 1.98428C8.5871 2.26039 9.1435 2.67508 9.58732 3.19717C10.0311 3.71926 10.3508 4.33518 10.5224 4.99861C10.6136 5.3517 10.9322 5.59836 11.2969 5.59836L12.1364 5.59836L12.1369 5.59836C12.6995 5.59884 13.246 5.78663 13.6901 6.13211C14.1344 6.47769 14.4509 6.96137 14.5897 7.50682C14.7286 8.05226 14.6818 8.62841 14.4568 9.14433C14.2318 9.66025 13.8415 10.0866 13.3473 10.356C12.9594 10.5675 12.8163 11.0534 13.0278 11.4413C13.2393 11.8292 13.7252 11.9722 14.1131 11.7608C14.9194 11.3212 15.5564 10.6257 15.9234 9.78389C16.2905 8.94213 16.3668 8.0021 16.1403 7.11216C15.9138 6.22222 15.3974 5.43305 14.6725 4.86921C13.9477 4.30537 13.0557 3.99897 12.1374 3.99836L11.8831 3.99836C11.6347 3.32927 11.2707 2.70712 10.8064 2.16088C10.2059 1.45452 9.45314 0.893468 8.60463 0.519915L8.60463 0.519915ZM8.70257 6.23268C8.55254 6.08265 8.34906 5.99836 8.13689 5.99836C7.92471 5.99836 7.72123 6.08265 7.5712 6.23268L4.90454 8.89934C4.59212 9.21176 4.59212 9.71829 4.90454 10.0307C5.21695 10.3431 5.72349 10.3431 6.03591 10.0307L7.33689 8.72973L7.33689 12.7984C7.33689 13.2402 7.69506 13.5984 8.13689 13.5984C8.57872 13.5984 8.93689 13.2402 8.93689 12.7984L8.93689 8.72974L10.2379 10.0307C10.5503 10.3431 11.0568 10.3431 11.3692 10.0307C11.6817 9.71829 11.6817 9.21176 11.3692 8.89934L8.70257 6.23268L8.70257 6.23268Z" fill="currentColor" fill-rule="evenodd" />
                  </svg>
                  {{ submitting() ? 'Téléversement...' : 'Téléverser' }}
                </app-button>
              </form>
            </div>
          }
          @case ('erreur') {
            <div class="upload__card">
              <h1 class="upload__card-title">Ajouter un fichier</h1>
              <div class="upload__file">
                <svg class="upload__file-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M0 0.9918C0 0.44405 0.44749 0 0.9985 0L13 0L17.9997 5L18 18.9925C18 19.5489 17.5551 20 17.0066 20L0.9934 20C0.44476 20 0 19.5447 0 19.0082L0 0.9918L0 0.9918ZM12 6L12 2L2 2L2 18L16 18L16 6L12 6L12 6ZM8 7.5C8 8.3284 7.3284 9 6.5 9C5.67157 9 5 8.3284 5 7.5C5 6.67157 5.67157 6 6.5 6C7.3284 6 8 6.67157 8 7.5L8 7.5ZM14.5 15L10.5 8L5 15L14.5 15L14.5 15Z" fill="#000000" fill-rule="evenodd" transform="translate(3 2)" />
                </svg>
                <div class="upload__file-info">
                  <span class="upload__file-name">{{ fileName() }}</span>
                  <span class="upload__file-size upload__file-size--error">{{ fileSize() }}</span>
                </div>
                <app-button variant="secondary" size="small" (click)="fileInput.click()">Changer</app-button>
              </div>
              <p class="upload__error" role="alert">La taille des fichiers est limitée à 1 Go</p>
              <form class="upload__form">
                <label class="upload__field">
                  <span class="upload__field-label">Mot de passe</span>
                  <app-input type="password" placeholder="Optionnel" />
                </label>
                <label class="upload__field">
                  <span class="upload__field-label">Expiration</span>
                  <app-select [options]="expirationOptions" value="7j" />
                </label>
                <app-button variant="primary" size="medium" type="submit" [disabled]="true" [fullWidth]="true">
                  <svg slot="icon" aria-hidden="true" viewBox="0 0 16.268 13.598" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M8.60463 0.519915C7.75612 0.146362 6.83398 -0.029976 5.90751 0.00415909C4.98104 0.0382941 4.07437 0.282013 3.25566 0.716993C2.43694 1.15197 1.72749 1.7669 1.18063 2.51553C0.633775 3.26416 0.26375 4.12703 0.0983721 5.03925C-0.0670059 5.95148 -0.0234319 6.88932 0.225817 7.78228C0.475067 8.67524 0.923506 9.50008 1.53742 10.1948C1.83 10.5259 2.33557 10.5571 2.66664 10.2645C2.99772 9.97192 3.02893 9.46635 2.73635 9.13527C2.28259 8.6218 1.95114 8.01213 1.76691 7.35212C1.58268 6.69211 1.55047 5.99892 1.67271 5.32466C1.79495 4.65041 2.06844 4.01264 2.47264 3.4593C2.87684 2.90597 3.40122 2.45146 4.00635 2.12995C4.61149 1.80844 5.28164 1.6283 5.96642 1.60307C6.6512 1.57784 7.33279 1.70818 7.95994 1.98428C8.5871 2.26039 9.1435 2.67508 9.58732 3.19717C10.0311 3.71926 10.3508 4.33518 10.5224 4.99861C10.6136 5.3517 10.9322 5.59836 11.2969 5.59836L12.1364 5.59836L12.1369 5.59836C12.6995 5.59884 13.246 5.78663 13.6901 6.13211C14.1344 6.47769 14.4509 6.96137 14.5897 7.50682C14.7286 8.05226 14.6818 8.62841 14.4568 9.14433C14.2318 9.66025 13.8415 10.0866 13.3473 10.356C12.9594 10.5675 12.8163 11.0534 13.0278 11.4413C13.2393 11.8292 13.7252 11.9722 14.1131 11.7608C14.9194 11.3212 15.5564 10.6257 15.9234 9.78389C16.2905 8.94213 16.3668 8.0021 16.1403 7.11216C15.9138 6.22222 15.3974 5.43305 14.6725 4.86921C13.9477 4.30537 13.0557 3.99897 12.1374 3.99836L11.8831 3.99836C11.6347 3.32927 11.2707 2.70712 10.8064 2.16088C10.2059 1.45452 9.45314 0.893468 8.60463 0.519915L8.60463 0.519915ZM8.70257 6.23268C8.55254 6.08265 8.34906 5.99836 8.13689 5.99836C7.92471 5.99836 7.72123 6.08265 7.5712 6.23268L4.90454 8.89934C4.59212 9.21176 4.59212 9.71829 4.90454 10.0307C5.21695 10.3431 5.72349 10.3431 6.03591 10.0307L7.33689 8.72973L7.33689 12.7984C7.33689 13.2402 7.69506 13.5984 8.13689 13.5984C8.57872 13.5984 8.93689 13.2402 8.93689 12.7984L8.93689 8.72974L10.2379 10.0307C10.5503 10.3431 11.0568 10.3431 11.3692 10.0307C11.6817 9.71829 11.6817 9.21176 11.3692 8.89934L8.70257 6.23268L8.70257 6.23268Z" fill="currentColor" fill-rule="evenodd" />
                  </svg>
                  Téléverser
                </app-button>
              </form>
            </div>
          }
          @case ('succes') {
            <div class="upload__card upload__card--success">
              <h1 class="upload__card-title">Ajouter un fichier</h1>
              <div class="upload__file">
                <svg class="upload__file-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M0 0.9918C0 0.44405 0.44749 0 0.9985 0L13 0L17.9997 5L18 18.9925C18 19.5489 17.5551 20 17.0066 20L0.9934 20C0.44476 20 0 19.5447 0 19.0082L0 0.9918L0 0.9918ZM12 6L12 2L2 2L2 18L16 18L16 6L12 6L12 6ZM8 7.5C8 8.3284 7.3284 9 6.5 9C5.67157 9 5 8.3284 5 7.5C5 6.67157 5.67157 6 6.5 6C7.3284 6 8 6.67157 8 7.5L8 7.5ZM14.5 15L10.5 8L5 15L14.5 15L14.5 15Z" fill="#000000" fill-rule="evenodd" transform="translate(3 2)" />
                </svg>
                <div class="upload__file-info">
                  <span class="upload__file-name">{{ fileName() }}</span>
                  <span class="upload__file-size">{{ fileSize() }}</span>
                </div>
              </div>
              <p class="upload__success-text">Félicitations, ton fichier sera conservé chez nous pendant {{ expirationLabel() }} !</p>
              <div class="upload__link">
                <a [href]="downloadUrl()">{{ downloadUrl() }}</a>
              </div>
              <app-button variant="primary" size="medium" [fullWidth]="true" (click)="copyLink()">
                <svg slot="icon" aria-hidden="true" viewBox="0 0 14.933 14.933" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M1.75621 1.75621C1.85623 1.65619 1.99188 1.6 2.13333 1.6L8.13333 1.6C8.27478 1.6 8.41044 1.65619 8.51046 1.75621C8.61048 1.85623 8.66667 1.99188 8.66667 2.13333L8.66667 2.8C8.66667 3.24183 9.02484 3.6 9.46667 3.6C9.90849 3.6 10.2667 3.24183 10.2667 2.8L10.2667 2.13333C10.2667 1.56754 10.0419 1.02492 9.64183 0.624839C9.24175 0.224761 8.69913 0 8.13333 0L2.13333 0C1.56754 0 1.02492 0.224761 0.624839 0.624839C0.224761 1.02492 0 1.56754 0 2.13333L0 8.13333C0 8.69913 0.224761 9.24175 0.624839 9.64183C1.02492 10.0419 1.56754 10.2667 2.13333 10.2667L2.8 10.2667C3.24183 10.2667 3.6 9.90849 3.6 9.46667C3.6 9.02484 3.24183 8.66667 2.8 8.66667L2.13333 8.66667C1.99188 8.66667 1.85623 8.61048 1.75621 8.51046C1.65619 8.41044 1.6 8.27478 1.6 8.13333L1.6 2.13333C1.6 1.99188 1.65619 1.85623 1.75621 1.75621L1.75621 1.75621ZM6.8 4.66667C5.62179 4.66667 4.66667 5.62179 4.66667 6.8L4.66667 12.8C4.66667 13.9782 5.62179 14.9333 6.8 14.9333L12.8 14.9333C13.9782 14.9333 14.9333 13.9782 14.9333 12.8L14.9333 6.8C14.9333 5.62179 13.9782 4.66667 12.8 4.66667L6.8 4.66667L6.8 4.66667ZM6.26667 6.8C6.26667 6.50545 6.50545 6.26667 6.8 6.26667L12.8 6.26667C13.0946 6.26667 13.3333 6.50545 13.3333 6.8L13.3333 12.8C13.3333 13.0946 13.0946 13.3333 12.8 13.3333L6.8 13.3333C6.50545 13.3333 6.26667 13.0946 6.26667 12.8L6.26667 6.8L6.26667 6.8Z" fill="currentColor" fill-rule="evenodd" />
                </svg>
                {{ copied() ? 'Lien copié !' : 'Copier le lien' }}
              </app-button>
            </div>
          }
        }
      </div>

      @if (state() === 'vide' || state() === 'succes') {
        <app-footer />
      }
    </div>
  `,
  styles: `
    .upload {
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      background: linear-gradient(164.76deg, #ffb88c 2.29%, #de6262 97.71%);
    }

    .upload__stage {
      flex: 1;
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .upload__stage--drag-over::after {
      content: '';
      position: absolute;
      inset: 8px;
      border: 2px dashed var(--color-primary);
      border-radius: 16px;
      pointer-events: none;
    }

    /* --- État vide --- */

    .upload__intro {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 24px;
      padding: 0 24px;
      text-align: center;
    }

    .upload__title {
      margin: 0;
      font-family: var(--font-family-base);
      font-weight: 400;
      font-size: 32px;
      line-height: 40px;
      color: #000000;
    }

    .upload__trigger {
      appearance: none;
      border: none;
      cursor: pointer;
      width: 144px;
      height: 144px;
      border-radius: 999px;
      background-color: rgb(47 25 13 / 15%);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .upload__trigger-inner {
      width: 96px;
      height: 96px;
      border-radius: 999px;
      background-color: #100218;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .upload__trigger-icon {
      width: 48px;
      height: 48px;
    }

    /* --- Carte (fichier ajouté / erreur / succès) --- */

    .upload__card {
      width: 100%;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      gap: 24px;
      align-items: center;
      padding: 24px;
      background-color: #ffffff;
      box-shadow: 0 0 12px 0 rgb(0 0 0 / 25%);
      border-radius: 16px 16px 0 0;
      position: absolute;
      bottom: 0;
      left: 0;
    }

    .upload__card-title {
      margin: 0;
      width: 100%;
      text-align: center;
      font-family: var(--font-family-base);
      font-weight: 700;
      font-size: 28px;
      line-height: 40px;
      color: #000000;
    }

    .upload__file {
      width: 100%;
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .upload__file-icon {
      flex-shrink: 0;
      width: 24px;
      height: 24px;
    }

    .upload__file-info {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }

    .upload__file-name {
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #000000;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .upload__file-size {
      font-family: var(--font-family-sans);
      font-size: 14px;
      line-height: 16px;
      color: #000000;
    }

    .upload__file-size--error {
      color: var(--color-danger);
    }

    .upload__error {
      width: 100%;
      margin: 0;
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: var(--color-danger);
    }

    .upload__form {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 16px;
      align-items: center;
    }

    .upload__field {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .upload__field-label {
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: var(--color-select-text);
    }

    .upload__success-text {
      width: 100%;
      margin: 0;
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #000000;
    }

    .upload__link {
      width: 100%;
      box-sizing: border-box;
      padding: 8px 16px;
      border-radius: 8px;
      background-color: rgb(255 94 0 / 3%);
      border: 1px solid rgb(215 99 11 / 20%);
    }

    .upload__link a {
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #d8640b;
    }

    /* --- Desktop : carte centrée flottante, pas de bottom-sheet --- */
    @media (min-width: 768px) {
      .upload__card {
        position: static;
        width: 412px;
        border-radius: 16px;
        padding: 24px 32px;
      }

      .upload__card--success {
        width: 544px;
      }
    }
  `,
})
export class Upload {
  private fileShare = inject(FileShare);

  protected state = signal<UploadState>('vide');
  protected fileName = signal('');
  protected fileSize = signal('');
  protected password = signal('');
  protected expirationOption = signal('7j');
  protected submitting = signal(false);
  protected apiErrorMessage = signal<string | null>(null);
  protected downloadUrl = signal('');
  protected copied = signal(false);
  protected dragHover = signal(false);

  private selectedFile: File | null = null;

  protected expirationOptions: SelectOption[] = [
    { value: '1j', label: 'Une journée' },
    { value: '2j', label: 'Deux jours' },
    { value: '3j', label: 'Trois jours' },
    { value: '4j', label: 'Quatre jours' },
    { value: '5j', label: 'Cinq jours' },
    { value: '6j', label: 'Six jours' },
    { value: '7j', label: 'Sept jours' },
  ];

  protected expirationLabel = computed(() => {
    const option = this.expirationOptions.find((o) => o.value === this.expirationOption());
    return option ? option.label.toLowerCase() : '';
  });

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // permet de resélectionner le même fichier plus tard

    if (file) {
      this.handleFile(file);
    }
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragHover.set(true);
  }

  protected onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragHover.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragHover.set(false);

    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.handleFile(file);
    }
  }

  private handleFile(file: File): void {
    this.selectedFile = file;
    this.fileName.set(file.name);
    this.fileSize.set(formatFileSize(file.size));
    this.apiErrorMessage.set(null);
    this.state.set(file.size > MAX_SIZE_BYTES ? 'erreur' : 'ajoute');
  }

  protected onSubmit(event: Event): void {
    event.preventDefault();
    if (!this.selectedFile) {
      return;
    }

    this.apiErrorMessage.set(null);
    this.submitting.set(true);

    const expiresInDays = parseInt(this.expirationOption(), 10);
    const password = this.password().trim() || null;

    this.fileShare.upload(this.selectedFile, expiresInDays, password, []).subscribe({
      next: (response) => {
        this.submitting.set(false);
        this.downloadUrl.set(response.downloadUrl);
        this.state.set('succes');
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.apiErrorMessage.set(error.error?.detail ?? 'Une erreur est survenue, réessayez.');
      },
    });
  }

  protected copyLink(): void {
    navigator.clipboard.writeText(this.downloadUrl());
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 2000);
  }
}
