package fr.euflow.backend.fileshare;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Expose l'accès public à un fichier partagé par son token (US02).
 */
@RestController
@RequestMapping("/api/v1/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * Renvoie les métadonnées d'un fichier partagé, visibles avant de le télécharger.
     *
     * @param token token public du partage
     * @return 200 avec le nom, le type, la taille, la date d'expiration et si un mot de passe est requis
     * @throws ShareNotFoundException si le token est inconnu ou malformé (404)
     * @throws ShareExpiredException si le fichier a expiré (410)
     */
    @GetMapping("/{token}")
    public ShareMetadataResponse getMetadata(@PathVariable String token) {
        return shareService.getMetadata(token);
    }

    /**
     * Vérifie le mot de passe d'un partage protégé et renvoie un token d'accès éphémère à
     * utiliser sur {@link #download} — étape 1 du téléchargement en 2 temps (US02/US09).
     *
     * @param token token public du partage
     * @param request mot de passe fourni
     * @return 200 avec le token d'accès et sa durée de validité
     * @throws ShareNotFoundException si le token est inconnu ou malformé (404)
     * @throws ShareExpiredException si le fichier a expiré (410)
     * @throws SharePasswordMismatchException si le mot de passe ne correspond pas (401)
     */
    @PostMapping("/{token}/authenticate")
    public ShareAccessResponse authenticate(@PathVariable String token, @RequestBody AuthenticateShareRequest request) {
        return shareService.authenticate(token, request.password());
    }

    /**
     * Télécharge le contenu réel du fichier — étape 2 (US02/US09). Le nom de fichier est fourni
     * sous les deux formes recommandées par la RFC 6266 : {@code filename} (repli ASCII, pour
     * les navigateurs qui ignorent la forme étendue — WebKit/Safari notamment, testé en e2e) et
     * {@code filename*} (UTF-8, pour rester correct avec des caractères non-ASCII).
     *
     * @param token token public du partage
     * @param accessToken token d'accès éphémère, requis seulement si le partage est protégé
     * @return 200, flux binaire avec {@code Content-Disposition: attachment} (jamais affiché
     *         inline par le navigateur, même pour un contenu qu'il saurait rendre)
     * @throws ShareNotFoundException si le token est inconnu ou malformé (404)
     * @throws ShareExpiredException si le fichier a expiré (410)
     * @throws InvalidAccessTokenException si le partage est protégé et le token d'accès
     *         manquant/invalide/expiré (401)
     */
    @GetMapping("/{token}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String token,
            @RequestParam(value = "access_token", required = false) String accessToken) {
        ShareDownload download = shareService.download(token, accessToken);
        String asciiFallbackFilename = download.filename().replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "'");
        String encodedFilename = URLEncoder.encode(download.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + asciiFallbackFilename + "\"; filename*=UTF-8''" + encodedFilename;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(new InputStreamResource(download.content()));
    }
}
