package fr.euflow.backend.fileshare;

/**
 * Levée quand le mot de passe de protection d'un partage (US09) ne respecte pas la longueur
 * minimale — traduite en 400 par {@link FileShareExceptionHandler}.
 */
public class InvalidSharePasswordException extends RuntimeException {

    public InvalidSharePasswordException(int minLength) {
        super("Le mot de passe doit contenir au moins " + minLength + " caractères");
    }
}
