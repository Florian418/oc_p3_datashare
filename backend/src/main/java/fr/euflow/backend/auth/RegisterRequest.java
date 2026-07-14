package fr.euflow.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de la requête {@code POST /api/v1/auth/register}.
 *
 * @param email adresse email du futur compte (doit être unique)
 * @param password mot de passe en clair, au moins 6 caractères (jamais stocké tel quel)
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password) {
}
