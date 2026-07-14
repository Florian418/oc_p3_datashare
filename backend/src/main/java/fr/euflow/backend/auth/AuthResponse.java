package fr.euflow.backend.auth;

import java.time.Instant;

/**
 * Corps de la réponse {@code POST /api/v1/auth/login} en cas de succès.
 *
 * @param token JWT compact à utiliser en {@code Authorization: Bearer} sur les endpoints protégés
 * @param expiresAt date d'expiration du token
 */
public record AuthResponse(String token, Instant expiresAt) {
}
