import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Auth } from './auth';
import { environment } from '../../environments/environment';

/**
 * Ajoute `Authorization: Bearer <token>` sur les appels vers l'API DataShare quand une session
 * est active — jamais sur des requêtes vers un autre domaine (pas de fuite du token).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const token = auth.token();

  if (token && req.url.startsWith(environment.apiUrl)) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(req);
};
