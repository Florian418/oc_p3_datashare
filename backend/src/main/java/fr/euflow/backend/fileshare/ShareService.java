package fr.euflow.backend.fileshare;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Accès public à un partage par son token (US02) — vue "shares", distincte de la vue privée
 * "files" mais adossée à la même entité {@link FileShare}.
 */
@Service
public class ShareService {

    private final FileShareRepository fileShareRepository;

    public ShareService(FileShareRepository fileShareRepository) {
        this.fileShareRepository = fileShareRepository;
    }

    /**
     * @param rawToken token tel que reçu dans l'URL (pas encore validé comme un UUID)
     * @return les métadonnées visibles avant téléchargement
     * @throws ShareNotFoundException si le token est malformé ou ne correspond à aucun fichier
     * @throws ShareExpiredException si le fichier a dépassé sa date d'expiration
     */
    public ShareMetadataResponse getMetadata(String rawToken) {
        FileShare fileShare = resolveValidShare(rawToken);
        return new ShareMetadataResponse(
                fileShare.getName(),
                fileShare.getMime(),
                fileShare.getSize(),
                fileShare.getExpiresAt(),
                fileShare.getSharePasswordHash() != null);
    }

    /**
     * @param rawToken token tel que reçu dans l'URL
     * @return le fichier partagé correspondant, garanti non expiré
     * @throws ShareNotFoundException si le token est malformé ou ne correspond à aucun fichier
     * @throws ShareExpiredException si le fichier a dépassé sa date d'expiration
     */
    FileShare resolveValidShare(String rawToken) {
        UUID token = parseToken(rawToken);
        FileShare fileShare = fileShareRepository.findByToken(token).orElseThrow(ShareNotFoundException::new);

        if (fileShare.getExpiresAt().isBefore(Instant.now())) {
            throw new ShareExpiredException();
        }
        return fileShare;
    }

    private UUID parseToken(String rawToken) {
        try {
            return UUID.fromString(rawToken);
        } catch (IllegalArgumentException e) {
            throw new ShareNotFoundException();
        }
    }
}
