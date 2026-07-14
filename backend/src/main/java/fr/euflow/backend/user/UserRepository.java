package fr.euflow.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Accès aux comptes utilisateurs ({@code users}). Les méthodes dérivées ci-dessous sont
 * implémentées automatiquement par Spring Data JPA à partir de leur nom.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * @param email adresse email à vérifier
     * @return {@code true} si un compte existe déjà avec cet email
     */
    boolean existsByEmail(String email);

    /**
     * @param email adresse email recherchée
     * @return le compte correspondant, s'il existe
     */
    Optional<User> findByEmail(String email);
}
