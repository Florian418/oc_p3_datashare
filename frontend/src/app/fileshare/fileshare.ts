import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface UploadFileResponse {
  token: string;
  downloadUrl: string;
  expiresAt: string;
}

/**
 * Appels aux endpoints de partage de fichiers (`/files`).
 */
@Service()
export class FileShare {
  private http = inject(HttpClient);

  /**
   * @param file fichier à envoyer
   * @param expiresInDays durée de validité en jours (1-7)
   * @param password mot de passe de protection, `null` si le partage est libre
   * @param tags tags associés (aucun pour l'instant, pas de champ dans l'écran actuel)
   */
  upload(file: File, expiresInDays: number, password: string | null, tags: string[]) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('expiresInDays', String(expiresInDays));
    if (password) {
      formData.append('password', password);
    }
    for (const tag of tags) {
      formData.append('tags', tag);
    }

    return this.http.post<UploadFileResponse>(`${environment.apiUrl}/files`, formData);
  }
}
