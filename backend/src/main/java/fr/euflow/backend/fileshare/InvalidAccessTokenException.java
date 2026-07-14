package fr.euflow.backend.fileshare;

/**
 * Levée quand le token d'accès éphémère (obtenu via {@code /authenticate}) est absent, invalide,
 * expiré, ou ne correspond pas au partage demandé — traduite en 401 par
 * {@link FileShareExceptionHandler}. Volontairement pas de distinction entre ces cas (contrairement
 * au JWT de connexion, US02 n'exige pas ce niveau de détail).
 */
public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("Jeton d'accès manquant, invalide ou expiré");
    }
}
