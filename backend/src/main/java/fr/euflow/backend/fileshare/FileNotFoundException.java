package fr.euflow.backend.fileshare;

/**
 * Levée quand {@code id} ne correspond à aucun fichier appartenant à l'utilisateur authentifié
 * (id inconnu ou fichier d'un autre propriétaire — volontairement pas de distinction, pour ne
 * pas confirmer l'existence d'un fichier qui n'est pas le sien) — traduite en 404 par
 * {@link FileShareExceptionHandler}.
 */
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException() {
        super("Fichier introuvable");
    }
}
