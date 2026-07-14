package fr.euflow.backend.fileshare;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
