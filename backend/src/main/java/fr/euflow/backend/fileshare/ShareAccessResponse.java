package fr.euflow.backend.fileshare;

/**
 * Corps de la réponse {@code POST /api/v1/shares/{token}/authenticate} en cas de succès.
 *
 * @param accessToken token éphémère à transmettre en {@code ?access_token=} sur le téléchargement
 * @param expiresIn durée de validité du token, en secondes
 */
public record ShareAccessResponse(String accessToken, long expiresIn) {
}
