package fr.euflow.backend.fileshare;

import java.time.Instant;

/**
 * Un élément de l'historique {@code GET /api/v1/files} (US05) — jamais le mot de passe
 * lui-même, seulement s'il y en a un ; {@code id} sert de référence pour une future
 * suppression ({@code DELETE /api/v1/files/{id}}).
 *
 * @param id identifiant interne du fichier
 * @param token identifiant public du partage, utilisé dans le lien de téléchargement
 * @param name nom original du fichier
 * @param size taille en octets
 * @param createdAt date d'envoi
 * @param expiresAt date d'expiration
 * @param passwordProtected {@code true} si un mot de passe protège le téléchargement
 */
public record FileHistoryItemResponse(
        Long id, String token, String name, long size, Instant createdAt, Instant expiresAt, boolean passwordProtected) {
}
