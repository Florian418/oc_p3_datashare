package fr.euflow.backend.fileshare;

/**
 * Levée quand le type réel du fichier (détecté par son contenu, pas son extension) n'est pas
 * dans la liste blanche des types autorisés — traduite en 400 par {@link FileShareExceptionHandler}.
 */
public class FileTypeNotAllowedException extends RuntimeException {

    public FileTypeNotAllowedException(String detectedMimeType) {
        super("Type de fichier non autorisé : " + detectedMimeType);
    }
}
