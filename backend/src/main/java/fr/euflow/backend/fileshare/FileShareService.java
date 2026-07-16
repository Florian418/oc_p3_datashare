package fr.euflow.backend.fileshare;

import fr.euflow.backend.storage.FileStorageService;
import fr.euflow.backend.user.User;
import fr.euflow.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(FileShareService.class);

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
        int resolvedExpiresInDays = resolveExpiresInDays(expiresInDays);
        Instant expiresAt = Instant.now().plus(resolvedExpiresInDays, ChronoUnit.DAYS);
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

        LOGGER.atInfo()
                .setMessage("file_uploaded")
                .addKeyValue("sizeBytes", fileShare.getSize())
                .addKeyValue("mimeType", mimeType)
                .addKeyValue("expiresInDays", resolvedExpiresInDays)
                .addKeyValue("passwordProtected", sharePasswordHash != null)
                .addKeyValue("authenticated", owner != null)
                .log();

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
                .map(this::toHistoryItem)
                .toList();
    }

    /**
     * Supprime un fichier déposé par l'utilisateur authentifié (US06).
     *
     * @param id identifiant interne du fichier à supprimer
     * @throws FileNotFoundException si le fichier n'existe pas ou n'appartient pas à l'appelant
     */
    public void delete(Long id) {
        FileShare fileShare = findOwnedFileShare(id);
        fileStorageService.delete(fileShare.getToken().toString());
        fileShareRepository.delete(fileShare);
    }

    /**
     * Détail d'un fichier déposé par l'utilisateur authentifié, tags inclus (US08).
     *
     * @param id identifiant interne du fichier
     * @return le détail du fichier
     * @throws FileNotFoundException si le fichier n'existe pas ou n'appartient pas à l'appelant
     */
    public FileHistoryItemResponse getDetail(Long id) {
        return toHistoryItem(findOwnedFileShare(id));
    }

    /**
     * Ajoute un tag à un fichier déposé par l'utilisateur authentifié (US08). Annotée
     * {@code @Transactional} pour que la collection {@code tags} reste gérée par le même
     * contexte de persistance de la lecture à l'écriture (nécessaire pour que
     * l'{@code orphanRemoval} de {@link #removeTag} fonctionne de façon fiable, donc posé ici
     * aussi par cohérence).
     *
     * @param id identifiant interne du fichier
     * @param label libellé du tag (déjà validé par {@link AddTagRequest} : non vide, 50
     *     caractères maximum)
     * @return les tags du fichier après ajout
     * @throws FileNotFoundException si le fichier n'existe pas ou n'appartient pas à l'appelant
     */
    @Transactional
    public List<TagResponse> addTag(Long id, String label) {
        FileShare fileShare = findOwnedFileShare(id);
        fileShare.addTag(label.strip());
        // flush explicite : Tag utilise GenerationType.IDENTITY, son id n'existe qu'après un
        // vrai INSERT — sans ça, toTagResponses() sérialiserait un id encore null.
        fileShareRepository.saveAndFlush(fileShare);
        return toTagResponses(fileShare);
    }

    /**
     * Retire un tag d'un fichier déposé par l'utilisateur authentifié (US08).
     *
     * @param id identifiant interne du fichier
     * @param tagId identifiant du tag à retirer
     * @return les tags du fichier après retrait
     * @throws FileNotFoundException si le fichier n'existe pas ou n'appartient pas à l'appelant
     * @throws TagNotFoundException si le tag n'appartient pas à ce fichier
     */
    @Transactional
    public List<TagResponse> removeTag(Long id, Long tagId) {
        FileShare fileShare = findOwnedFileShare(id);
        Tag tag = fileShare.getTags().stream()
                .filter(candidate -> candidate.getId().equals(tagId))
                .findFirst()
                .orElseThrow(TagNotFoundException::new);
        fileShare.getTags().remove(tag);
        return toTagResponses(fileShare);
    }

    private FileHistoryItemResponse toHistoryItem(FileShare fileShare) {
        return new FileHistoryItemResponse(
                fileShare.getId(),
                fileShare.getToken().toString(),
                fileShare.getName(),
                fileShare.getMime(),
                fileShare.getSize(),
                fileShare.getCreatedAt(),
                fileShare.getExpiresAt(),
                fileShare.getSharePasswordHash() != null,
                toTagResponses(fileShare));
    }

    private List<TagResponse> toTagResponses(FileShare fileShare) {
        return fileShare.getTags().stream().map(tag -> new TagResponse(tag.getId(), tag.getLabel())).toList();
    }

    /**
     * Résout un fichier appartenant à l'utilisateur authentifié. Un {@code id} inconnu et un
     * {@code id} appartenant à un autre utilisateur sont traités identiquement (404), pour ne
     * jamais confirmer l'existence d'un fichier qui n'est pas le sien.
     *
     * @param id identifiant interne du fichier
     * @throws FileNotFoundException si le fichier n'existe pas ou n'appartient pas à l'appelant
     */
    private FileShare findOwnedFileShare(Long id) {
        User owner = resolveAuthenticatedUser();
        return fileShareRepository.findById(id)
                .filter(candidate -> candidate.getUser() != null && candidate.getUser().getId().equals(owner.getId()))
                .orElseThrow(FileNotFoundException::new);
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
