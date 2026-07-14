package fr.euflow.backend.fileshare;

import java.time.Instant;

/**
 * Corps de la réponse {@code GET /api/v1/shares/{token}} — métadonnées visibles avant
 * téléchargement (US02), jamais le mot de passe lui-même.
 *
 * @param name nom original du fichier
 * @param mime type MIME réel détecté à l'upload
 * @param size taille en octets
 * @param expiresAt date d'expiration
 * @param passwordProtected {@code true} si un mot de passe est requis pour télécharger
 */
public record ShareMetadataResponse(String name, String mime, long size, Instant expiresAt, boolean passwordProtected) {
}
