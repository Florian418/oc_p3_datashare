package fr.euflow.backend.fileshare;

import fr.euflow.backend.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Purge automatique des fichiers expirés (US10) — tâche planifiée quotidienne : supprime le
 * blob de stockage via {@link FileStorageService#delete(String)} puis la ligne {@code
 * file_shares} correspondante, pour chaque partage dont {@code expiresAt} est dépassé.
 */
@Component
public class PurgeExpiredFilesTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurgeExpiredFilesTask.class);

    private final FileShareRepository fileShareRepository;
    private final FileStorageService fileStorageService;

    public PurgeExpiredFilesTask(FileShareRepository fileShareRepository, FileStorageService fileStorageService) {
        this.fileShareRepository = fileShareRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * S'exécute une fois par jour à 3h du matin (heure serveur).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredFiles() {
        List<FileShare> expired = fileShareRepository.findByExpiresAtBefore(Instant.now());
        for (FileShare fileShare : expired) {
            fileStorageService.delete(fileShare.getToken().toString());
            fileShareRepository.delete(fileShare);
        }

        LOGGER.atInfo()
                .setMessage("expired_files_purged")
                .addKeyValue("count", expired.size())
                .log();
    }
}
