package fr.euflow.backend.fileshare;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Requête d'ajout d'un tag à un fichier ({@code POST /api/v1/files/{id}/tags}, US08).
 *
 * @param label libellé du tag (non vide, 50 caractères maximum — cf. {@code tags.label
 *     varchar(50)})
 */
public record AddTagRequest(@NotBlank @Size(max = 50) String label) {
}
