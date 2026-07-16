package fr.euflow.backend.fileshare;

/**
 * Levée quand {@code tagId} ne correspond à aucun tag du fichier ciblé (US08) — traduite en 404
 * par {@link FileShareExceptionHandler}.
 */
public class TagNotFoundException extends RuntimeException {

    public TagNotFoundException() {
        super("Tag introuvable");
    }
}
