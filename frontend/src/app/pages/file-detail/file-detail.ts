import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Header } from '../../ui/header/header';
import { Footer } from '../../ui/footer/footer';
import { Button } from '../../ui/button/button';
import { Input } from '../../ui/input/input';
import { Callout } from '../../ui/callout/callout';
import { FileShare, FileHistoryItem, FileTag } from '../../fileshare/fileshare';

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
 * Écran de détail d'un fichier déposé par l'utilisateur authentifié (US08) — accessible via le
 * bouton "Accéder" de {@link MySpace}. Affiche les infos du fichier, un lien vers l'écran
 * Téléchargement (US02) et la gestion des tags (ajout/retrait).
 */
@Component({
  selector: 'app-file-detail',
  imports: [Header, Footer, Button, Input, Callout, RouterLink],
  template: `
    <div class="file-detail">
      <app-header />

      <div class="file-detail__stage">
        <div class="file-detail__card">
          <a class="file-detail__back" routerLink="/my-space">← Retour à Mon espace</a>

          <h1 class="file-detail__title">Détail du fichier</h1>

          @if (loading()) {
            <p>Chargement...</p>
          } @else if (errorMessage(); as message) {
            <app-callout type="error">{{ message }}</app-callout>
          } @else if (file(); as fileItem) {
            <div class="file-detail__info">
              <div class="file-detail__row">
                <span class="file-detail__label">Nom</span>
                <span class="file-detail__value">{{ fileItem.name }}</span>
              </div>
              <div class="file-detail__row">
                <span class="file-detail__label">Taille</span>
                <span class="file-detail__value">{{ sizeLabel() }}</span>
              </div>
              <div class="file-detail__row">
                <span class="file-detail__label">Expiration</span>
                <span class="file-detail__value">{{ expiresLabel() }}</span>
              </div>
              <div class="file-detail__row">
                <span class="file-detail__label">Protection</span>
                <span class="file-detail__value">{{ fileItem.passwordProtected ? 'Par mot de passe' : 'Aucune' }}</span>
              </div>
            </div>

            <app-button variant="primary" size="medium" [fullWidth]="true" [routerLink]="'/download/' + fileItem.token">
              Accéder au fichier
            </app-button>

            <div class="file-detail__tags">
              <span class="file-detail__label">Tags</span>

              @if (fileItem.tags.length === 0) {
                <p class="file-detail__no-tags">Aucun tag pour l'instant.</p>
              } @else {
                <ul class="file-detail__tag-list">
                  @for (tag of fileItem.tags; track tag.id) {
                    <li class="file-detail__tag">
                      {{ tag.label }}
                      <button
                        type="button"
                        class="file-detail__tag-remove"
                        [disabled]="removingTagId() === tag.id"
                        (click)="onRemoveTag(tag.id)"
                        [attr.aria-label]="'Retirer le tag ' + tag.label"
                      >
                        ×
                      </button>
                    </li>
                  }
                </ul>
              }

              <form class="file-detail__tag-form" (submit)="onAddTag($event)">
                <label class="file-detail__tag-input-label">
                  <span class="file-detail__sr-only">Nouveau tag</span>
                  <app-input placeholder="Nouveau tag..." [(value)]="newTagLabel" [disabled]="addingTag()" />
                </label>
                <app-button type="submit" variant="secondary" size="small" [disabled]="!newTagLabel().trim() || addingTag()">
                  {{ addingTag() ? 'Ajout...' : 'Ajouter' }}
                </app-button>
              </form>

              @if (tagErrorMessage(); as message) {
                <app-callout type="error">{{ message }}</app-callout>
              }
            </div>
          }
        </div>
      </div>

      <app-footer />
    </div>
  `,
  styles: `
    .file-detail {
      min-height: 100dvh;
      display: flex;
      flex-direction: column;
      background: linear-gradient(164.76deg, #ffb88c 2.29%, #de6262 97.71%);
    }

    .file-detail__stage {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }

    .file-detail__card {
      width: 100%;
      max-width: 640px;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding: 24px;
      background-color: #ffffff;
      box-shadow: 0 0 12px 0 rgb(0 0 0 / 25%);
      border-radius: 16px;
    }

    .file-detail__back {
      align-self: flex-start;
      font-family: var(--font-family-sans);
      font-size: 14px;
      color: var(--color-secondary-text);
      text-decoration: none;
    }

    .file-detail__back:hover {
      text-decoration: underline;
    }

    .file-detail__title {
      margin: 0;
      font-family: var(--font-family-base);
      font-weight: 700;
      font-size: 28px;
      line-height: 40px;
      color: #000000;
    }

    .file-detail__info {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .file-detail__row {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
    }

    .file-detail__label {
      color: rgb(0 0 0 / 60%);
    }

    .file-detail__value {
      color: #000000;
      text-align: right;
      overflow-wrap: anywhere;
    }

    .file-detail__tags {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .file-detail__no-tags {
      margin: 0;
      font-family: var(--font-family-sans);
      font-size: 14px;
      color: rgb(0 0 0 / 60%);
    }

    .file-detail__tag-list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .file-detail__tag {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 8px;
      border-radius: 999px;
      background-color: var(--color-file-row-bg);
      border: 1px solid var(--color-file-row-border);
      font-family: var(--font-family-sans);
      font-size: 14px;
      color: #000000;
    }

    .file-detail__tag-remove {
      appearance: none;
      border: none;
      background: transparent;
      padding: 0;
      line-height: 1;
      color: var(--color-danger);
      cursor: pointer;
    }

    .file-detail__tag-form {
      display: flex;
      gap: 8px;
      align-items: center;
    }

    .file-detail__tag-input-label {
      flex: 1;
    }

    .file-detail__sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
  `,
})
export class FileDetail {
  private route = inject(ActivatedRoute);
  private fileShare = inject(FileShare);

  private id = Number(this.route.snapshot.paramMap.get('id'));

  protected loading = signal(true);
  protected errorMessage = signal<string | null>(null);
  protected file = signal<FileHistoryItem | null>(null);

  protected newTagLabel = signal('');
  protected addingTag = signal(false);
  protected removingTagId = signal<number | null>(null);
  protected tagErrorMessage = signal<string | null>(null);

  protected sizeLabel = computed(() => formatFileSize(this.file()?.size ?? 0));

  protected expiresLabel = computed(() => {
    const fileItem = this.file();
    if (!fileItem) {
      return '';
    }
    const msUntilExpiry = new Date(fileItem.expiresAt).getTime() - Date.now();
    if (msUntilExpiry <= 0) {
      return 'Expiré';
    }
    const daysUntilExpiry = Math.ceil(msUntilExpiry / (1000 * 60 * 60 * 24));
    return daysUntilExpiry <= 1 ? 'Expire demain' : `Expire dans ${daysUntilExpiry} jours`;
  });

  constructor() {
    this.fileShare.get(this.id).subscribe({
      next: (fileItem) => {
        this.file.set(fileItem);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Ce fichier est introuvable.');
        this.loading.set(false);
      },
    });
  }

  protected onAddTag(event: Event): void {
    event.preventDefault();
    const label = this.newTagLabel().trim();
    if (!label) {
      return;
    }

    this.tagErrorMessage.set(null);
    this.addingTag.set(true);

    this.fileShare.addTag(this.id, label).subscribe({
      next: (tags: FileTag[]) => {
        this.updateTags(tags);
        this.newTagLabel.set('');
        this.addingTag.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.addingTag.set(false);
        this.tagErrorMessage.set(error.error?.detail ?? 'Impossible d\'ajouter ce tag, réessayez.');
      },
    });
  }

  protected onRemoveTag(tagId: number): void {
    this.tagErrorMessage.set(null);
    this.removingTagId.set(tagId);

    this.fileShare.removeTag(this.id, tagId).subscribe({
      next: (tags: FileTag[]) => {
        this.updateTags(tags);
        this.removingTagId.set(null);
      },
      error: (error: HttpErrorResponse) => {
        this.removingTagId.set(null);
        this.tagErrorMessage.set(error.error?.detail ?? 'Impossible de retirer ce tag, réessayez.');
      },
    });
  }

  private updateTags(tags: FileTag[]): void {
    const fileItem = this.file();
    if (fileItem) {
      this.file.set({ ...fileItem, tags });
    }
  }
}
