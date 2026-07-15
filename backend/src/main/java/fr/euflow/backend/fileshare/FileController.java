package fr.euflow.backend.fileshare;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

/**
 * Expose les endpoints de dépôt de fichiers (US01 avec compte, US07 anonyme).
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileShareService fileShareService;

    public FileController(FileShareService fileShareService) {
        this.fileShareService = fileShareService;
    }

    /**
     * Dépose un fichier et génère son lien de partage. Authentification optionnelle : avec un
     * JWT valide, le fichier est lié au compte (US01, visible dans l'historique) ; sans, le
     * dépôt reste anonyme (US07).
     *
     * @param file fichier envoyé
     * @param expiresInDays durée de validité en jours (1-7), 7 par défaut si absent
     * @param password mot de passe de protection, optionnel (min. 6 caractères si renseigné)
     * @param tags tags associés, optionnels
     * @return 201 avec le token, le lien de téléchargement et la date d'expiration
     */
    @PostMapping
    public ResponseEntity<UploadFileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "expiresInDays", required = false) Integer expiresInDays,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "tags", required = false) List<String> tags) {
        UploadFileResponse response = fileShareService.upload(
                file, expiresInDays, password, tags == null ? Collections.emptyList() : tags);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Liste les fichiers déposés par l'utilisateur authentifié (US05). Route protégée par
     * défaut (voir {@code SecurityConfig}) : un JWT valide est requis.
     *
     * @return les fichiers du propriétaire, du plus récent au plus ancien
     */
    @GetMapping
    public List<FileHistoryItemResponse> list() {
        return fileShareService.listForCurrentUser();
    }
}
