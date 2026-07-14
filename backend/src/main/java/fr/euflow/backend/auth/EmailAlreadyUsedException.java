package fr.euflow.backend.auth;

/**
 * Levée quand l'email est déjà utilisé à l'inscription — traduite en 409 par
 * {@link AuthExceptionHandler}.
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Cet email est déjà utilisé : " + email);
    }
}
