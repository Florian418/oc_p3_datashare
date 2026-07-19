import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Button } from '../../ui/button/button';
import { Switch, SwitchOption } from '../../ui/switch/switch';
import { Input } from '../../ui/input/input';
import { FileShare, FileHistoryItem } from '../../fileshare/fileshare';
import { Auth } from '../../auth/auth';

type FileType = 'image' | 'audio' | 'video';
type FileStatus = 'active' | 'expired';

interface FileItem {
  id: string;
  name: string;
  type: FileType;
  status: FileStatus;
  expiresLabel: string;
  protected: boolean;
  tags: string[];
}

function fileTypeFromMime(mime: string): FileType {
  if (mime.startsWith('audio/')) return 'audio';
  if (mime.startsWith('video/')) return 'video';
  return 'image';
}

function toFileItem(item: FileHistoryItem): FileItem {
  const msUntilExpiry = new Date(item.expiresAt).getTime() - Date.now();
  const expired = msUntilExpiry <= 0;
  const daysUntilExpiry = Math.ceil(msUntilExpiry / (1000 * 60 * 60 * 24));
  const expiresLabel = expired ? 'Expiré' : daysUntilExpiry <= 1 ? 'Expire demain' : `Expire dans ${daysUntilExpiry} jours`;

  return {
    id: String(item.id),
    name: item.name,
    type: fileTypeFromMime(item.mime),
    status: expired ? 'expired' : 'active',
    expiresLabel,
    protected: item.passwordProtected,
    tags: item.tags.map((tag) => tag.label),
  };
}

/**
 * Écran Mon espace (US05/US06/US08) — sidebar desktop / drawer mobile, liste de fichiers
 * filtrable (réutilise {@link Switch}, déjà interactif nativement), câblée sur `GET /files`.
 */
@Component({
  selector: 'app-my-space',
  imports: [RouterLink, Button, Switch, Input],
  host: {
    '(document:click)': 'closeMenusOutside($event)',
  },
  template: `
    <div class="my-space">
      <aside class="my-space__sidebar">
        <a class="my-space__logo" routerLink="/">DataShare</a>
        <nav class="my-space__nav">
          <span class="my-space__nav-pill">Mes fichiers</span>
        </nav>
        <p class="my-space__sidebar-footer">Copyright DataShare© 2026</p>
      </aside>

      @if (drawerOpen()) {
        <div class="my-space__scrim" (click)="closeDrawer()"></div>
        <aside class="my-space__drawer">
          <div class="my-space__drawer-header">
            <button type="button" class="my-space__icon-btn" (click)="closeDrawer()" aria-label="Fermer le menu">
              <svg width="14.5" height="14.5" viewBox="0 0 14.5 14.5" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                <path d="M2.13388 0.366117C1.64573 -0.122039 0.854273 -0.122039 0.366117 0.366117C-0.122039 0.854273 -0.122039 1.64573 0.366117 2.13388L5.48222 7.25L0.366117 12.3661C-0.122039 12.8543 -0.122039 13.6457 0.366117 14.1339C0.854273 14.622 1.64573 14.622 2.13388 14.1339L7.25 9.01778L12.3661 14.1339C12.8543 14.622 13.6457 14.622 14.1339 14.1339C14.622 13.6457 14.622 12.8543 14.1339 12.3661L9.01778 7.25L14.1339 2.13388C14.622 1.64573 14.622 0.854273 14.1339 0.366117C13.6457 -0.122039 12.8543 -0.122039 12.3661 0.366117L7.25 5.48222L2.13388 0.366117L2.13388 0.366117Z" fill="#FFFFFF" fill-rule="evenodd" />
              </svg>
            </button>
            <span class="my-space__logo my-space__logo--drawer">DataShare</span>
          </div>
          <nav class="my-space__nav">
            <span class="my-space__nav-pill">Mes fichiers</span>
          </nav>
          <button type="button" class="my-space__drawer-logout" (click)="logout()">
            <svg width="13.6" height="13.6" viewBox="0 0 13.6 13.6" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
              <path d="M1.75621 1.75621C1.85623 1.65619 1.99188 1.6 2.13333 1.6L4.8 1.6C5.24183 1.6 5.6 1.24183 5.6 0.8C5.6 0.358172 5.24183 0 4.8 0L2.13333 0C1.56754 0 1.02492 0.224761 0.624839 0.624839C0.224761 1.02492 0 1.56754 0 2.13333L0 11.4667C0 12.0325 0.224761 12.5751 0.624839 12.9752C1.02492 13.3752 1.56754 13.6 2.13333 13.6L4.8 13.6C5.24183 13.6 5.6 13.2418 5.6 12.8C5.6 12.3582 5.24183 12 4.8 12L2.13333 12C1.99189 12 1.85623 11.9438 1.75621 11.8438C1.65619 11.7438 1.6 11.6081 1.6 11.4667L1.6 2.13333C1.6 1.99188 1.65619 1.85623 1.75621 1.75621L1.75621 1.75621ZM10.0324 2.90098C9.71993 2.58856 9.2134 2.58856 8.90098 2.90098C8.58856 3.2134 8.58856 3.71993 8.90098 4.03235L10.8686 6L4.8 6C4.35817 6 4 6.35817 4 6.8C4 7.24183 4.35817 7.6 4.8 7.6L10.8686 7.6L8.90098 9.56765C8.58856 9.88007 8.58856 10.3866 8.90098 10.699C9.2134 11.0114 9.71993 11.0114 10.0324 10.699L13.3657 7.36569C13.6781 7.05327 13.6781 6.54673 13.3657 6.23431L10.0324 2.90098L10.0324 2.90098Z" fill="currentColor" fill-rule="evenodd" />
            </svg>
            Déconnexion
          </button>
          <p class="my-space__drawer-footer">Copyright DataShare© 2026</p>
        </aside>
      }

      <div class="my-space__main">
        <header class="my-space__topbar">
          <button type="button" class="my-space__icon-btn my-space__menu-btn" (click)="openDrawer()" aria-label="Ouvrir le menu">
            <svg width="20.5" height="14.5" viewBox="0 0 20.5 14.5" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
              <path d="M1.25 0C0.559644 0 0 0.559644 0 1.25C0 1.94036 0.559644 2.5 1.25 2.5L19.25 2.5C19.9404 2.5 20.5 1.94036 20.5 1.25C20.5 0.559644 19.9404 0 19.25 0L1.25 0L1.25 0ZM1.25 6C0.559644 6 0 6.55964 0 7.25C0 7.94036 0.559644 8.5 1.25 8.5L19.25 8.5C19.9404 8.5 20.5 7.94036 20.5 7.25C20.5 6.55964 19.9404 6 19.25 6L1.25 6L1.25 6ZM1.25 12C0.559644 12 0 12.5596 0 13.25C0 13.9404 0.559644 14.5 1.25 14.5L19.25 14.5C19.9404 14.5 20.5 13.9404 20.5 13.25C20.5 12.5596 19.9404 12 19.25 12L1.25 12L1.25 12Z" fill="#1E1E1E" fill-rule="evenodd" />
            </svg>
          </button>

          <div class="my-space__topbar-actions">
            <app-button variant="dark" size="small" routerLink="/">Ajouter des fichiers</app-button>
            <app-button variant="tertiary" size="small" (click)="logout()">
              <svg slot="icon" width="13.6" height="13.6" viewBox="0 0 13.6 13.6" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                <path d="M1.75621 1.75621C1.85623 1.65619 1.99188 1.6 2.13333 1.6L4.8 1.6C5.24183 1.6 5.6 1.24183 5.6 0.8C5.6 0.358172 5.24183 0 4.8 0L2.13333 0C1.56754 0 1.02492 0.224761 0.624839 0.624839C0.224761 1.02492 0 1.56754 0 2.13333L0 11.4667C0 12.0325 0.224761 12.5751 0.624839 12.9752C1.02492 13.3752 1.56754 13.6 2.13333 13.6L4.8 13.6C5.24183 13.6 5.6 13.2418 5.6 12.8C5.6 12.3582 5.24183 12 4.8 12L2.13333 12C1.99189 12 1.85623 11.9438 1.75621 11.8438C1.65619 11.7438 1.6 11.6081 1.6 11.4667L1.6 2.13333C1.6 1.99188 1.65619 1.85623 1.75621 1.75621L1.75621 1.75621ZM10.0324 2.90098C9.71993 2.58856 9.2134 2.58856 8.90098 2.90098C8.58856 3.2134 8.58856 3.71993 8.90098 4.03235L10.8686 6L4.8 6C4.35817 6 4 6.35817 4 6.8C4 7.24183 4.35817 7.6 4.8 7.6L10.8686 7.6L8.90098 9.56765C8.58856 9.88007 8.58856 10.3866 8.90098 10.699C9.2134 11.0114 9.71993 11.0114 10.0324 10.699L13.3657 7.36569C13.6781 7.05327 13.6781 6.54673 13.3657 6.23431L10.0324 2.90098L10.0324 2.90098Z" fill="currentColor" fill-rule="evenodd" />
              </svg>
              Déconnexion
            </app-button>
          </div>

          <div class="my-space__user">
            <span class="my-space__avatar" aria-hidden="true">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="12" cy="8" r="4" fill="#ffffff" />
                <path d="M4 20.5C4 16.634 7.582 13.5 12 13.5C16.418 13.5 20 16.634 20 20.5" fill="#ffffff" />
              </svg>
            </span>
            <span class="my-space__username">Utilisateur</span>
          </div>
        </header>

        <div class="my-space__body">
          <h1 class="my-space__title">Mes fichiers</h1>

          <div class="my-space__filters">
            <app-switch [options]="filterOptions" [(value)]="filter" ariaLabel="Filtrer les fichiers" />
            <label class="my-space__tag-filter-label">
              <span class="my-space__sr-only">Filtrer par tag</span>
              <app-input placeholder="Filtrer par tag..." [(value)]="tagFilter" />
            </label>
          </div>

          <ul class="my-space__list">
            @for (file of filteredFiles(); track file.id) {
              <li class="my-space__row">
                <svg class="my-space__row-icon" width="18" height="20" viewBox="0 0 18 20" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                  @switch (file.type) {
                    @case ('image') {
                      <path d="M0 0.9918C0 0.44405 0.44749 0 0.9985 0L13 0L17.9997 5L18 18.9925C18 19.5489 17.5551 20 17.0066 20L0.9934 20C0.44476 20 0 19.5447 0 19.0082L0 0.9918L0 0.9918ZM12 6L12 2L2 2L2 18L16 18L16 6L12 6L12 6ZM8 7.5C8 8.3284 7.3284 9 6.5 9C5.67157 9 5 8.3284 5 7.5C5 6.67157 5.67157 6 6.5 6C7.3284 6 8 6.67157 8 7.5L8 7.5ZM14.5 15L10.5 8L5 15L14.5 15L14.5 15Z" fill="#000000" fill-rule="evenodd" />
                    }
                    @case ('audio') {
                      <path d="M0 0.9918C0 0.44405 0.44749 0 0.9985 0L13 0L17.9997 5L18 18.9925C18 19.5489 17.5551 20 17.0066 20L0.9934 20C0.44476 20 0 19.5447 0 19.0082L0 0.9918L0 0.9918ZM13 6L13 8L10 8L10 12.5C10 13.8807 8.8807 15 7.5 15C6.11929 15 5 13.8807 5 12.5C5 11.1193 6.11929 10 7.5 10C7.6712 10 7.8384 10.0172 8 10.05L8 6L12 6L12 2L2 2L2 18L16 18L16 6L13 6L13 6Z" fill="#000000" fill-rule="evenodd" />
                    }
                    @case ('video') {
                      <path d="M0.9985 0C0.44749 0 0 0.44405 0 0.9918L0 19.0082C0 19.5447 0.44476 20 0.9934 20L17.0066 20C17.5551 20 18 19.5489 18 18.9925L17.9997 5L13 0L0.9985 0L0.9985 0ZM12 2L12 6L16 6L16 18L2 18L2 2L12 2L12 2ZM12.0008 9.667L7.1219 6.41435C7.0562 6.37054 6.979 6.34717 6.9 6.34717C6.6791 6.34717 6.5 6.52625 6.5 6.74717L6.5 13.2524C6.5 13.3314 6.5234 13.4086 6.5672 13.4743C6.6897 13.6581 6.9381 13.7078 7.1219 13.5852L12.0008 10.3326C12.0447 10.3033 12.0824 10.2656 12.1117 10.2217C12.2343 10.0379 12.1846 9.7895 12.0008 9.667L12.0008 9.667Z" fill="#000000" fill-rule="evenodd" />
                    }
                  }
                </svg>

                <div class="my-space__row-info">
                  <span class="my-space__row-name">{{ file.name }}</span>
                  <span class="my-space__row-status" [class.my-space__row-status--expired]="file.status === 'expired'">
                    {{ file.expiresLabel }}
                  </span>
                  @if (file.tags.length > 0) {
                    <span class="my-space__row-tags">{{ file.tags.join(', ') }}</span>
                  }
                </div>

                @if (file.status === 'expired') {
                  <span class="my-space__row-expired-note">Ce fichier a expiré, il n'est plus stocké chez nous</span>
                } @else {
                  <div class="my-space__row-actions" [class.my-space__row-actions--confirming]="confirmingDeleteId() === file.id">
                    @if (file.protected) {
                      <svg class="my-space__lock" width="13.6" height="14.933" viewBox="0 0 13.6 14.933" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                        <path d="M10.9333 4.13333L10.9333 6L11.4667 6C12.6449 6 13.6 6.95513 13.6 8.13333L13.6 12.8C13.6 13.9782 12.6449 14.9333 11.4667 14.9333L2.13333 14.9333C0.955126 14.9333 0 13.9782 0 12.8L0 8.13333C0 6.95513 0.955126 6 2.13333 6L2.66667 6L2.66667 4.13333C2.66667 3.03711 3.10214 1.98578 3.87729 1.21062C4.65244 0.435475 5.70377 0 6.8 0C7.89623 0 8.94756 0.435475 9.72271 1.21062C10.4979 1.98578 10.9333 3.03711 10.9333 4.13333L10.9333 4.13333ZM5.00866 2.342C5.48375 1.8669 6.12812 1.6 6.8 1.6C7.47188 1.6 8.11624 1.8669 8.59134 2.342C9.06643 2.81709 9.33333 3.46145 9.33333 4.13333L9.33333 6L4.26667 6L4.26667 4.13333C4.26667 3.46145 4.53357 2.81709 5.00866 2.342L5.00866 2.342ZM2.13333 7.6L11.4667 7.6C11.7612 7.6 12 7.83878 12 8.13333L12 12.8C12 13.0946 11.7612 13.3333 11.4667 13.3333L2.13333 13.3333C1.83878 13.3333 1.6 13.0946 1.6 12.8L1.6 8.13333C1.6 7.83878 1.83878 7.6 2.13333 7.6L2.13333 7.6Z" fill="#1E1E1E" fill-rule="evenodd" />
                      </svg>
                    }

                    @if (confirmingDeleteId() === file.id) {
                      <div class="my-space__row-confirm">
                        <span class="my-space__row-confirm-text">Supprimer ?</span>
                        <app-button variant="tertiary" size="small" [disabled]="deletingId() === file.id" (click)="cancelDelete()">
                          Annuler
                        </app-button>
                        <app-button variant="primary" size="small" [disabled]="deletingId() === file.id" (click)="confirmDelete(file)">
                          {{ deletingId() === file.id ? 'Suppression...' : 'Confirmer' }}
                        </app-button>
                      </div>
                    } @else {
                      <div class="my-space__row-buttons">
                        <app-button variant="secondary" size="small" (click)="requestDelete(file.id)">
                          <svg slot="icon" width="13.6" height="14.933" viewBox="0 0 13.6 14.933" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                            <path d="M10.2667 2.13333L10.2667 2.66667L12.8 2.66667C13.2418 2.66667 13.6 3.02484 13.6 3.46667C13.6 3.90849 13.2418 4.26667 12.8 4.26667L12.2667 4.26667L12.2667 12.8C12.2667 13.3658 12.0419 13.9084 11.6418 14.3085C11.2417 14.7086 10.6991 14.9333 10.1333 14.9333L3.46667 14.9333C2.90087 14.9333 2.35825 14.7086 1.95817 14.3085C1.55809 13.9084 1.33333 13.3658 1.33333 12.8L1.33333 4.26667L0.8 4.26667C0.358172 4.26667 0 3.90849 0 3.46667C0 3.02484 0.358172 2.66667 0.8 2.66667L3.33333 2.66667L3.33333 2.13333C3.33333 1.56754 3.5581 1.02492 3.95817 0.624839C4.35825 0.224761 4.90087 0 5.46667 0L8.13333 0C8.69913 0 9.24175 0.224761 9.64183 0.624839C10.0419 1.02492 10.2667 1.56754 10.2667 2.13333L10.2667 2.13333ZM5.08954 1.75621C5.18956 1.65619 5.32522 1.6 5.46667 1.6L8.13333 1.6C8.27478 1.6 8.41044 1.65619 8.51046 1.75621C8.61048 1.85623 8.66667 1.99188 8.66667 2.13333L8.66667 2.66667L4.93333 2.66667L4.93333 2.13333C4.93333 1.99189 4.98952 1.85623 5.08954 1.75621L5.08954 1.75621ZM2.93333 4.26667L10.6667 4.26667L10.6667 12.8C10.6667 12.9414 10.6105 13.0771 10.5105 13.1771C10.4104 13.2771 10.2748 13.3333 10.1333 13.3333L3.46667 13.3333C3.32522 13.3333 3.18956 13.2771 3.08954 13.1771C2.98952 13.0771 2.93333 12.9414 2.93333 12.8L2.93333 4.26667L2.93333 4.26667ZM6.26667 6.8C6.26667 6.35817 5.90849 6 5.46667 6C5.02484 6 4.66667 6.35817 4.66667 6.8L4.66667 10.8C4.66667 11.2418 5.02484 11.6 5.46667 11.6C5.90849 11.6 6.26667 11.2418 6.26667 10.8L6.26667 6.8L6.26667 6.8ZM8.93333 6.8C8.93333 6.35817 8.57516 6 8.13333 6C7.69151 6 7.33333 6.35817 7.33333 6.8L7.33333 10.8C7.33333 11.2418 7.69151 11.6 8.13333 11.6C8.57516 11.6 8.93333 11.2418 8.93333 10.8L8.93333 6.8L8.93333 6.8Z" fill="currentColor" fill-rule="evenodd" />
                          </svg>
                          Supprimer
                        </app-button>
                        <app-button variant="secondary" size="small" [routerLink]="'/my-space/' + file.id">
                          Accéder
                          <svg slot="icon-end" width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                            <path d="M6.03235 0.234315C5.71993 -0.078105 5.2134 -0.078105 4.90098 0.234315C4.58856 0.546734 4.58856 1.05327 4.90098 1.36568L8.20197 4.66667L0.8 4.66667C0.358172 4.66667 0 5.02484 0 5.46667C0 5.90849 0.358172 6.26667 0.8 6.26667L8.20196 6.26667L4.90098 9.56765C4.58856 9.88007 4.58856 10.3866 4.90098 10.699C5.2134 11.0114 5.71993 11.0114 6.03235 10.699L10.699 6.03235C11.0114 5.71993 11.0114 5.2134 10.699 4.90098L6.03235 0.234315L6.03235 0.234315Z" fill="currentColor" fill-rule="evenodd" transform="translate(2.533 2.533)" />
                          </svg>
                        </app-button>
                      </div>

                      <details class="my-space__row-menu" name="file-row-menu">
                        <summary class="my-space__row-menu-trigger" aria-label="Actions pour {{ file.name }}">
                          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="8" cy="3.33" r="1.47" fill="currentColor" />
                            <circle cx="8" cy="8" r="1.47" fill="currentColor" />
                            <circle cx="8" cy="12.67" r="1.47" fill="currentColor" />
                          </svg>
                        </summary>
                        <div class="my-space__row-menu-panel">
                          <button type="button" class="my-space__row-menu-item" (click)="requestDelete(file.id)">Supprimer</button>
                          <a class="my-space__row-menu-item" [routerLink]="'/my-space/' + file.id">Accéder</a>
                        </div>
                      </details>
                    }
                  </div>
                }
              </li>
            }
          </ul>
        </div>
      </div>
    </div>
  `,
  styles: `
    .my-space {
      min-height: 100dvh;
      display: flex;
      background-color: #fff8f3;
    }

    .my-space__logo {
      font-family: var(--font-family-base);
      font-weight: 700;
      font-size: 32px;
      line-height: 40px;
      color: #ffffff;
      text-decoration: none;
    }

    .my-space__nav-pill {
      display: block;
      box-sizing: border-box;
      width: 100%;
      padding: 8px 16px;
      border-radius: 12px;
      background-color: rgb(255 255 255 / 44%);
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #803a00;
    }

    /* --- Sidebar (desktop) --- */

    .my-space__sidebar {
      display: none;
    }

    /* --- Drawer + scrim (mobile) --- */

    .my-space__scrim {
      position: fixed;
      inset: 0;
      background-color: rgb(0 0 0 / 38%);
      z-index: 10;
    }

    .my-space__drawer {
      position: fixed;
      top: 0;
      left: 0;
      bottom: 0;
      width: 295px;
      max-width: 85vw;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      padding: 24px;
      gap: 24px;
      background: linear-gradient(164.76deg, #ffb88c 2.29%, #de6262 97.71%);
      box-shadow: 4px 0 32px 0 rgb(0 0 0 / 25%);
      z-index: 11;
    }

    .my-space__drawer-header {
      display: flex;
      align-items: center;
      gap: 24px;
    }

    .my-space__logo--drawer {
      font-size: 24px;
      line-height: 32px;
    }

    .my-space__drawer-logout {
      appearance: none;
      box-sizing: border-box;
      width: 100%;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      border-radius: 12px;
      border: 1px solid rgb(255 255 255 / 44%);
      background: transparent;
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #f1e9e2;
      cursor: pointer;
    }

    .my-space__drawer-footer {
      margin: auto 0 0;
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #f1e9e2;
    }

    /* --- Topbar --- */

    .my-space__main {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
    }

    .my-space__topbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 16px 24px;
      background-color: #ffeee3;
      border-bottom: 1px solid rgb(216 97 28 / 29%);
    }

    .my-space__icon-btn {
      appearance: none;
      border: none;
      background: transparent;
      padding: 0;
      display: inline-flex;
      cursor: pointer;
    }

    .my-space__menu-btn {
      display: inline-flex;
    }

    .my-space__topbar-actions {
      display: none;
      align-items: center;
      gap: 16px;
    }

    .my-space__user {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .my-space__avatar {
      width: 40px;
      height: 40px;
      border-radius: 999px;
      background-color: rgb(0 0 0 / 50%);
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .my-space__username {
      font-family: var(--font-family-sans);
      font-weight: 600;
      font-size: 16px;
      line-height: 24px;
      color: #000000;
    }

    /* --- Body / list --- */

    .my-space__body {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 24px;
      padding: 24px;
    }

    .my-space__title {
      margin: 0;
      font-family: var(--font-family-base);
      font-weight: 700;
      font-size: 28px;
      line-height: 40px;
      color: #000000;
    }

    .my-space__filters {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 16px;
    }

    .my-space__tag-filter-label {
      flex: 1;
      min-width: 200px;
    }

    .my-space__sr-only {
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

    .my-space__list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .my-space__row {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 16px;
      padding: 16px 8px;
      border-radius: 8px;
      background-color: var(--color-file-row-bg);
      border: 1px solid var(--color-file-row-border);
    }

    .my-space__row-icon {
      flex-shrink: 0;
    }

    .my-space__row-info {
      flex: 1;
      min-width: 140px;
      display: flex;
      flex-direction: column;
    }

    .my-space__row-name {
      font-family: var(--font-family-sans);
      font-size: 16px;
      line-height: 24px;
      color: #000000;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .my-space__row-status {
      font-family: var(--font-family-sans);
      font-size: 14px;
      line-height: 16px;
      color: #000000;
    }

    .my-space__row-status--expired {
      color: #c62020;
    }

    .my-space__row-tags {
      font-family: var(--font-family-sans);
      font-size: 14px;
      line-height: 16px;
      color: rgb(0 0 0 / 60%);
    }

    .my-space__row-expired-note {
      display: none;
    }

    .my-space__row-actions {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .my-space__row-actions--confirming {
      flex-basis: 100%;
      justify-content: flex-end;
    }

    .my-space__row-buttons {
      display: none;
      align-items: center;
      gap: 8px;
    }

    .my-space__row-confirm {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .my-space__row-confirm-text {
      font-family: var(--font-family-sans);
      font-size: 14px;
      line-height: 16px;
      color: #000000;
      white-space: nowrap;
    }

    /* --- Mobile row menu (details/summary, no JS needed) --- */

    .my-space__row-menu {
      position: relative;
    }

    .my-space__row-menu-trigger {
      appearance: none;
      list-style: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border-radius: 8px;
      border: 1px solid var(--color-secondary-border);
      color: var(--color-secondary-text);
      cursor: pointer;
    }

    .my-space__row-menu-trigger::-webkit-details-marker {
      display: none;
    }

    .my-space__row-menu-panel {
      position: absolute;
      top: calc(100% + 4px);
      right: 0;
      z-index: 1;
      display: flex;
      flex-direction: column;
      min-width: 140px;
      padding: 8px;
      gap: 4px;
      background-color: #ffffff;
      border: 1px solid var(--color-input-border);
      border-radius: 8px;
      box-shadow: 0 4px 12px 0 rgb(0 0 0 / 25%);
    }

    .my-space__row:last-child .my-space__row-menu-panel {
      top: auto;
      bottom: calc(100% + 4px);
    }

    .my-space__row-menu-item {
      appearance: none;
      border: none;
      background: transparent;
      text-align: left;
      text-decoration: none;
      padding: 8px;
      border-radius: 4px;
      font-family: var(--font-family-sans);
      font-size: 16px;
      color: var(--color-secondary-text);
      cursor: pointer;
    }

    .my-space__row-menu-item:hover {
      background-color: var(--color-file-row-bg);
    }

    /* --- Desktop layout --- */
    @media (min-width: 768px) {
      .my-space__sidebar {
        display: flex;
        flex-direction: column;
        width: 259px;
        flex-shrink: 0;
        border-right: 2px solid rgb(38 54 26 / 10%);
        background: linear-gradient(164.76deg, #ffb88c 2.29%, #de6262 97.71%);
        padding: 32px 0;
      }

      .my-space__sidebar .my-space__logo {
        display: flex;
        align-items: center;
        height: 72px;
        padding: 0 32px;
        box-sizing: border-box;
      }

      .my-space__sidebar .my-space__nav {
        padding: 24px;
      }

      .my-space__sidebar-footer {
        margin: auto 16px 0;
        font-family: var(--font-family-sans);
        font-size: 16px;
        line-height: 24px;
        white-space: nowrap;
        color: #f1e9e2;
      }

      .my-space__scrim,
      .my-space__drawer {
        display: none;
      }

      .my-space__menu-btn,
      .my-space__user {
        display: none;
      }

      .my-space__topbar-actions {
        display: flex;
        margin-left: auto;
      }

      .my-space__row {
        padding-left: 16px;
        padding-right: 16px;
      }

      .my-space__row-expired-note {
        display: block;
        flex-shrink: 0;
        white-space: nowrap;
        text-align: right;
        font-family: var(--font-family-sans);
        font-size: 14px;
        line-height: 16px;
        color: rgb(0 0 0 / 50%);
      }

      .my-space__row-buttons {
        display: flex;
      }

      .my-space__row-menu {
        display: none;
      }

      .my-space__row-actions--confirming {
        flex-basis: auto;
        justify-content: flex-start;
      }
    }
  `,
})
export class MySpace {
  private fileShare = inject(FileShare);
  private elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private auth = inject(Auth);
  private router = inject(Router);

  protected drawerOpen = signal(false);
  protected filter = signal('tous');
  protected tagFilter = signal('');

  protected filterOptions: SwitchOption[] = [
    { value: 'tous', label: 'Tous' },
    { value: 'actifs', label: 'Actifs' },
    { value: 'expire', label: 'Expiré' },
  ];

  private files = signal<FileItem[]>([]);

  protected filteredFiles = computed(() => {
    const filter = this.filter();
    const tagFilter = this.tagFilter().trim().toLowerCase();
    let files = this.files();
    if (filter === 'actifs') files = files.filter((file) => file.status === 'active');
    if (filter === 'expire') files = files.filter((file) => file.status === 'expired');
    if (tagFilter) files = files.filter((file) => file.tags.some((tag) => tag.toLowerCase().includes(tagFilter)));
    return files;
  });

  protected confirmingDeleteId = signal<string | null>(null);
  protected deletingId = signal<string | null>(null);

  constructor() {
    this.fileShare.list().subscribe({
      next: (items) => this.files.set(items.map(toFileItem)),
      error: () => this.files.set([]),
    });
  }

  protected openDrawer(): void {
    this.drawerOpen.set(true);
  }

  protected closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/');
  }

  protected closeMenusOutside(event: MouseEvent): void {
    const target = event.target as Node;
    const openMenus = this.elementRef.nativeElement.querySelectorAll<HTMLDetailsElement>('details.my-space__row-menu[open]');
    for (const menu of openMenus) {
      if (!menu.contains(target)) {
        menu.open = false;
      }
    }
  }

  protected requestDelete(id: string): void {
    this.confirmingDeleteId.set(id);
  }

  protected cancelDelete(): void {
    this.confirmingDeleteId.set(null);
  }

  protected confirmDelete(file: FileItem): void {
    this.deletingId.set(file.id);
    this.fileShare.delete(Number(file.id)).subscribe({
      next: () => {
        this.files.update((files) => files.filter((f) => f.id !== file.id));
        this.confirmingDeleteId.set(null);
        this.deletingId.set(null);
      },
      error: () => this.deletingId.set(null),
    });
  }
}
