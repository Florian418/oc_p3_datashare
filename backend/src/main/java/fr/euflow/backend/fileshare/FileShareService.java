package fr.euflow.backend.fileshare;

import fr.euflow.backend.storage.FileStorageService;
import fr.euflow.backend.user.User;
import fr.euflow.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Dépose un fichier partagé (US01 avec compte, US07 anonyme — l'authentification est
 * optionnelle : c'est la présence d'un utilisateur authentifié dans le contexte de sécurité
 * qui fait la différence, pas deux chemins de code séparés).
 */
@Service
public class FileShareService {

    private static final long MAX_SIZE_BYTES = 1_073_741_824L; // 1 Go (US01)
    private static final int MIN_EXPIRES_IN_DAYS = 1;
    private static final int MAX_EXPIRES_IN_DAYS = 7;
    private static final int DEFAULT_EXPIRES_IN_DAYS = 7;
    private static final int MIN_SHARE_PASSWORD_LENGTH = 6; // US01/US09
    private static final int MAX_TAG_LENGTH = 50; // tags.label varchar(50)

    private final FileShareRepository fileShareRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FileTypeValidator fileTypeValidator;
    private final PasswordEncoder passwordEncoder;
    private final String frontendBaseUrl;

    public FileShareService(
            FileShareRepository fileShareRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            FileTypeValidator fileTypeValidator,
            PasswordEncoder passwordEncoder,
            @Value("${datashare.frontend.base-url}") String frontendBaseUrl) {
        this.fileShareRepository = fileShareRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.fileTypeValidator = fileTypeValidator;
        this.passwordEncoder = passwordEncoder;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * @param file fichier envoyé (multipart)
     * @param expiresInDays durée de validité en jours, {@code null} pour la valeur par défaut (7)
     * @param password mot de passe de protection, {@code null} si le partage est libre
     * @param tags libellés des tags associés, jamais {@code null} (liste vide si aucun)
     * @return le token public, le lien de téléchargement et la date d'expiration
     * @throws FileTooLargeException si le fichier dépasse 1 Go
     * @throws FileTypeNotAllowedException si le type réel du fichier n'est pas autorisé
     * @throws InvalidSharePasswordException si le mot de passe fait moins de 6 caractères
     */
    public UploadFileResponse upload(MultipartFile file, Integer expiresInDays, String password, List<String> tags) {
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new FileTooLargeException(MAX_SIZE_BYTES);
        }
        if (password != null && !password.isBlank() && password.length() < MIN_SHARE_PASSWORD_LENGTH) {
            throw new InvalidSharePasswordException(MIN_SHARE_PASSWORD_LENGTH);
        }

        String mimeType = detectMimeType(file);
        Instant expiresAt = Instant.now().plus(resolveExpiresInDays(expiresInDays), ChronoUnit.DAYS);
        String sharePasswordHash = (password == null || password.isBlank()) ? null : passwordEncoder.encode(password);
        User owner = resolveAuthenticatedUser();

        FileShare fileShare = new FileShare(file.getOriginalFilename(), mimeType, file.getSize(), expiresAt, sharePasswordHash, owner);
        for (String tag : tags) {
            if (!tag.isBlank()) {
                fileShare.addTag(tag.strip().substring(0, Math.min(tag.strip().length(), MAX_TAG_LENGTH)));
            }
        }

        storeContent(file, fileShare);
        fileShareRepository.save(fileShare);

        String downloadUrl = frontendBaseUrl + "/download/" + fileShare.getToken();
        return new UploadFileResponse(fileShare.getToken().toString(), downloadUrl, expiresAt);
    }

    /**
     * Liste les fichiers déposés par l'utilisateur authentifié (US05, historique). La route
     * exige déjà une authentification valide (voir {@code SecurityConfig}), donc l'utilisateur
     * résolu ici n'est jamais {@code null} en pratique.
     *
     * @return les fichiers du propriétaire, du plus récent au plus ancien
     */
    public List<FileHistoryItemResponse> listForCurrentUser() {
        User owner = resolveAuthenticatedUser();
        return fileShareRepository.findByUserOrderByCreatedAtDesc(owner).stream()
                .map(fileShare -> new FileHistoryItemResponse(
                        fileShare.getId(),
                        fileShare.getToken().toString(),
                        fileShare.getName(),
                        fileShare.getMime(),
                        fileShare.getSize(),
                        fileShare.getCreatedAt(),
                        fileShare.getExpiresAt(),
                        fileShare.getSharePasswordHash() != null))
                .toList();
    }

    /**
     * Supprime un fichier déposé par l'utilisateur authentifié (US06). Un {@code id} inconnu et
     * un {@code id} appartenant à un autre utilisateur sont traités identiquement (404), pour ne
     * jamais confirmer l'existence d'un fichier qui n'est pas le sien.
     *
     * @param id identifiant interne du fichier à supprimer
     * @throws FileNotFoundException si le fichier n'existe pas ou n'appartient pas à l'appelant
     */
    public void delete(Long id) {
        User owner = resolveAuthenticatedUser();
        FileShare fileShare = fileShareRepository.findById(id)
                .filter(candidate -> candidate.getUser() != null && candidate.getUser().getId().equals(owner.getId()))
                .orElseThrow(FileNotFoundException::new);

        fileStorageService.delete(fileShare.getToken().toString());
        fileShareRepository.delete(fileShare);
    }

    private String detectMimeType(MultipartFile file) {
        try {
            return fileTypeValidator.detectAndValidate(file.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier envoyé", e);
        }
    }

    private void storeContent(MultipartFile file, FileShare fileShare) {
        try {
            fileStorageService.store(fileShare.getToken().toString(), file.getInputStream(), file.getSize(), fileShare.getMime());
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier envoyé", e);
        }
    }

    private int resolveExpiresInDays(Integer requested) {
        if (requested == null) {
            return DEFAULT_EXPIRES_IN_DAYS;
        }
        if (requested < MIN_EXPIRES_IN_DAYS || requested > MAX_EXPIRES_IN_DAYS) {
            throw new InvalidExpirationException(MIN_EXPIRES_IN_DAYS, MAX_EXPIRES_IN_DAYS);
        }
        return requested;
    }

    private User resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
        if (isAnonymous) {
            return null;
        }
        String email = (String) authentication.getPrincipal();
        return userRepository.findByEmail(email).orElse(null);
    }
}
