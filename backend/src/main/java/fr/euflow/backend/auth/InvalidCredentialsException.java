package fr.euflow.backend.auth;

/**
 * Levée quand l'email est inconnu ou le mot de passe invalide à la connexion — traduite
 * en 401 par {@link AuthExceptionHandler}. Volontairement la même exception dans les deux
 * cas, pour ne jamais révéler l'existence d'un compte via l'endpoint de connexion.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email ou mot de passe invalide");
    }
}
