package fr.euflow.backend.fileshare;

/**
 * Levée quand la durée d'expiration demandée sort de la plage autorisée (US01/US10) —
 * traduite en 400 par {@link FileShareExceptionHandler}.
 */
public class InvalidExpirationException extends RuntimeException {

    public InvalidExpirationException(int minDays, int maxDays) {
        super("La durée d'expiration doit être comprise entre " + minDays + " et " + maxDays + " jours");
    }
}
