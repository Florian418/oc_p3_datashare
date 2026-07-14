package fr.euflow.backend.fileshare;

/**
 * Levée quand le mot de passe fourni ne correspond pas à celui d'un partage protégé (US02/US09)
 * — traduite en 401 par {@link FileShareExceptionHandler}.
 */
public class SharePasswordMismatchException extends RuntimeException {

    public SharePasswordMismatchException() {
        super("Mot de passe invalide");
    }
}
