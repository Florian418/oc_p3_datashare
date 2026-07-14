import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface ShareMetadata {
  name: string;
  mime: string;
  size: number;
  expiresAt: string;
  passwordProtected: boolean;
}

export interface ShareAccessResponse {
  accessToken: string;
  expiresIn: number;
}

/**
 * Appels à l'accès public d'un fichier partagé (`/shares/{token}`, US02).
 */
@Service()
export class Share {
  private http = inject(HttpClient);

  getMetadata(token: string) {
    return this.http.get<ShareMetadata>(`${environment.apiUrl}/shares/${token}`);
  }

  /**
   * Vérifie le mot de passe d'un partage protégé et renvoie un token d'accès éphémère à
   * utiliser sur {@link downloadUrl}.
   */
  authenticate(token: string, password: string) {
    return this.http.post<ShareAccessResponse>(`${environment.apiUrl}/shares/${token}/authenticate`, { password });
  }

  /**
   * Lien de téléchargement réel — jamais appelé via `HttpClient` (le fichier peut peser jusqu'à
   * 1 Go), utilisé comme cible d'une vraie navigation navigateur (`window.location.href`) pour
   * un téléchargement natif en flux.
   */
  downloadUrl(token: string, accessToken?: string): string {
    const base = `${environment.apiUrl}/shares/${token}/download`;
    return accessToken ? `${base}?access_token=${encodeURIComponent(accessToken)}` : base;
  }
}
