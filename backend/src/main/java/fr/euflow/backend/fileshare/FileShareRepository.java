package fr.euflow.backend.fileshare;

import fr.euflow.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux fichiers partagés ({@code file_shares}).
 */
public interface FileShareRepository extends JpaRepository<FileShare, Long> {

    /**
     * @param token identifiant public du partage
     * @return le fichier correspondant, s'il existe
     */
    Optional<FileShare> findByToken(UUID token);

    /**
     * @param user propriétaire des fichiers (US05, historique)
     * @return les fichiers du propriétaire, du plus récent au plus ancien
     */
    List<FileShare> findByUserOrderByCreatedAtDesc(User user);

    /**
     * @param now instant de référence
     * @return les partages dont l'expiration est dépassée (US10, purge automatique)
     */
    List<FileShare> findByExpiresAtBefore(Instant now);
}
