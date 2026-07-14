package fr.euflow.backend.auth;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Cet email est déjà utilisé : " + email);
    }
}
