package fr.euflow.backend.auth;

import fr.euflow.backend.user.User;

import java.time.Instant;

/**
 * Corps de la réponse {@code POST /api/v1/auth/register} en cas de succès — ne contient
 * jamais le hash du mot de passe.
 *
 * @param id identifiant généré du compte
 * @param email adresse email du compte
 * @param createdAt date de création du compte
 */
public record UserResponse(Long id, String email, Instant createdAt) {

    /**
     * @param user l'entité JPA persistée
     * @return la projection publique de {@code user}, sans le hash du mot de passe
     */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
