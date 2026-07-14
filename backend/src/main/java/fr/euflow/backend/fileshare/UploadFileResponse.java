package fr.euflow.backend.fileshare;

import java.time.Instant;

/**
 * Corps de la réponse {@code POST /api/v1/files} en cas de succès.
 *
 * @param token identifiant public du partage, utilisé dans le lien de téléchargement
 * @param downloadUrl lien de téléchargement complet, prêt à être partagé
 * @param expiresAt date d'expiration du fichier
 */
public record UploadFileResponse(String token, String downloadUrl, Instant expiresAt) {
}
