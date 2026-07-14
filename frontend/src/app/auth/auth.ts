import { Service, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthSession {
  token: string;
  expiresAt: string;
}

const STORAGE_KEY = 'datashare.auth';

/**
 * État d'authentification courant (session JWT persistée en `localStorage`) et appels aux
 * endpoints `/auth/register` et `/auth/login`.
 */
@Service()
export class Auth {
  private http = inject(HttpClient);
  private session = signal<AuthSession | null>(readStoredSession());

  isLoggedIn = computed(() => this.session() !== null);

  register(request: RegisterRequest) {
    return this.http.post<void>(`${environment.apiUrl}/auth/register`, request);
  }

  /**
   * Authentifie l'utilisateur et démarre la session (token stocké en `localStorage`, survit
   * à un rechargement de page).
   */
  login(request: LoginRequest) {
    return this.http.post<AuthSession>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((session) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        this.session.set(session);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }
}

function readStoredSession(): AuthSession | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }

  const session = JSON.parse(raw) as AuthSession;
  if (new Date(session.expiresAt).getTime() <= Date.now()) {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
  return session;
}
