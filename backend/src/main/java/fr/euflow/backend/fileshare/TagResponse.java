package fr.euflow.backend.fileshare;

/**
 * Un tag exposé au client (US08) — {@code id} est nécessaire pour cibler un retrait
 * ({@code DELETE /api/v1/files/{id}/tags/{tagId}}).
 *
 * @param id identifiant interne du tag
 * @param label libellé du tag
 */
public record TagResponse(Long id, String label) {
}
