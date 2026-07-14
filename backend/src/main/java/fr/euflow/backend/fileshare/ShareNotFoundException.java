package fr.euflow.backend.fileshare;

/**
 * Levée quand le token d'un partage ne correspond à aucun fichier (inconnu ou malformé —
 * volontairement pas de distinction, un token est censé être opaque et non prédictible) —
 * traduite en 404 par {@link FileShareExceptionHandler}.
 */
public class ShareNotFoundException extends RuntimeException {

    public ShareNotFoundException() {
        super("Lien de téléchargement introuvable");
    }
}
