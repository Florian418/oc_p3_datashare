package fr.euflow.backend.fileshare;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès aux fichiers partagés ({@code file_shares}).
 */
public interface FileShareRepository extends JpaRepository<FileShare, Long> {
}
