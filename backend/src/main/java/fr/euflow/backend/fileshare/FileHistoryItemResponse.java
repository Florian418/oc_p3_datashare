package fr.euflow.backend.fileshare;

import java.time.Instant;
import java.util.List;

/**
 * Un élément de l'historique {@code GET /api/v1/files} (US05), aussi utilisé comme détail d'un
 * fichier ({@code GET /api/v1/files/{id}}, US08) — jamais le mot de passe lui-même, seulement
 * s'il y en a un ; {@code id} sert de référence pour une suppression ({@code DELETE
 * /api/v1/files/{id}}) ou une gestion des tags ({@code POST}/{@code DELETE
 * /api/v1/files/{id}/tags/...}).
 *
 * @param id identifiant interne du fichier
 * @param token identifiant public du partage, utilisé dans le lien de téléchargement
 * @param name nom original du fichier
 * @param mime type MIME réel détecté à l'envoi, utilisé côté front pour choisir l'icône
 * @param size taille en octets
 * @param createdAt date d'envoi
 * @param expiresAt date d'expiration
 * @param passwordProtected {@code true} si un mot de passe protège le téléchargement
 * @param tags tags associés (US08), {@code id} inclus pour permettre un retrait ciblé
 */
public record FileHistoryItemResponse(
        Long id,
        String token,
        String name,
        String mime,
        long size,
        Instant createdAt,
        Instant expiresAt,
        boolean passwordProtected,
        List<TagResponse> tags) {
}
