package fr.euflow.backend.fileshare;

/**
 * Levée quand un partage existe mais que sa date d'expiration est dépassée (US10 — la tâche de
 * purge quotidienne n'a pas forcément encore supprimé la ligne en base) — traduite en 410 par
 * {@link FileShareExceptionHandler}.
 */
public class ShareExpiredException extends RuntimeException {

    public ShareExpiredException() {
        super("Ce lien de téléchargement a expiré");
    }
}
