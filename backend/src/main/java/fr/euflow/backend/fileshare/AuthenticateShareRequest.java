package fr.euflow.backend.fileshare;

/**
 * Corps de la requête {@code POST /api/v1/shares/{token}/authenticate}.
 *
 * @param password mot de passe en clair, requis seulement si le partage est protégé
 */
public record AuthenticateShareRequest(String password) {
}
