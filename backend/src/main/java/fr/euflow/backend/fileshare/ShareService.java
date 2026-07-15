package fr.euflow.backend.fileshare;

import fr.euflow.backend.security.JwtService;
import fr.euflow.backend.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Accès public à un partage par son token (US02) — vue "shares", distincte de la vue privée
 * "files" mais adossée à la même entité {@link FileShare}.
 */
@Service
public class ShareService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareService.class);

    // courte durée volontaire (US02) : suffisante pour enchaîner authenticate → download,
    // jamais un vrai risque même si le token finit dans un log (il ne compromet pas le mot de
    // passe réel, contrairement à un identifiant à durée de vie longue)
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofSeconds(90);

    private final FileShareRepository fileShareRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public ShareService(
            FileShareRepository fileShareRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService) {
        this.fileShareRepository = fileShareRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
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
     * Vérifie le mot de passe d'un partage protégé et émet un token d'accès éphémère (courte
     * durée de vie, pas nécessairement à usage unique) — étape 1 du téléchargement en 2 temps
     * (US02/US09).
     *
     * @param rawToken token tel que reçu dans l'URL
     * @param password mot de passe fourni, ignoré si le partage n'est pas protégé
     * @return le token d'accès et sa durée de validité
     * @throws ShareNotFoundException si le token est malformé ou ne correspond à aucun fichier
     * @throws ShareExpiredException si le fichier a dépassé sa date d'expiration
     * @throws SharePasswordMismatchException si le mot de passe ne correspond pas
     */
    public ShareAccessResponse authenticate(String rawToken, String password) {
        FileShare fileShare = resolveValidShare(rawToken);
        String hash = fileShare.getSharePasswordHash();

        if (hash != null && !passwordEncoder.matches(password == null ? "" : password, hash)) {
            throw new SharePasswordMismatchException();
        }

        JwtService.GeneratedToken accessToken = jwtService.generateToken(fileShare.getToken().toString(), ACCESS_TOKEN_TTL);
        return new ShareAccessResponse(accessToken.value(), ACCESS_TOKEN_TTL.toSeconds());
    }

    /**
     * Étape 2 du téléchargement (US02/US09) : renvoie le contenu réel du fichier, en exigeant un
     * token d'accès valide (obtenu via {@link #authenticate}) si le partage est protégé.
     *
     * @param rawToken token tel que reçu dans l'URL
     * @param accessToken token d'accès éphémère, requis seulement si le partage est protégé
     * @return le flux de contenu et les métadonnées nécessaires à la réponse HTTP
     * @throws ShareNotFoundException si le token est malformé ou ne correspond à aucun fichier
     * @throws ShareExpiredException si le fichier a dépassé sa date d'expiration
     * @throws InvalidAccessTokenException si le partage est protégé et que le token d'accès est
     *         manquant, invalide, expiré, ou émis pour un autre partage
     */
    public ShareDownload download(String rawToken, String accessToken) {
        FileShare fileShare = resolveValidShare(rawToken);

        if (fileShare.getSharePasswordHash() != null) {
            requireValidAccessToken(fileShare, accessToken);
        }

        LOGGER.atInfo()
                .setMessage("file_downloaded")
                .addKeyValue("sizeBytes", fileShare.getSize())
                .addKeyValue("mimeType", fileShare.getMime())
                .addKeyValue("passwordProtected", fileShare.getSharePasswordHash() != null)
                .log();

        return new ShareDownload(
                fileStorageService.retrieve(fileShare.getToken().toString()),
                fileShare.getName(),
                fileShare.getMime(),
                fileShare.getSize());
    }

    private void requireValidAccessToken(FileShare fileShare, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new InvalidAccessTokenException();
        }

        String subject;
        try {
            subject = jwtService.extractSubject(accessToken);
        } catch (RuntimeException e) {
            throw new InvalidAccessTokenException();
        }

        if (!subject.equals(fileShare.getToken().toString())) {
            throw new InvalidAccessTokenException();
        }
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
