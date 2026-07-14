package fr.euflow.backend.fileshare;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
