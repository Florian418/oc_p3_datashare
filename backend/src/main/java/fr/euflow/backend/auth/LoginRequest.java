package fr.euflow.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corps de la requête {@code POST /api/v1/auth/login}.
 *
 * @param email adresse email du compte
 * @param password mot de passe en clair (jamais stocké tel quel)
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
