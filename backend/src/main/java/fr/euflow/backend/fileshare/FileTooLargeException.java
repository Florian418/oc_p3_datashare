package fr.euflow.backend.fileshare;

/**
 * Levée quand le fichier envoyé dépasse la taille maximale autorisée — traduite en 413 par
 * {@link FileShareExceptionHandler}.
 */
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(long maxSizeBytes) {
        super("La taille du fichier dépasse la limite autorisée (" + maxSizeBytes + " octets)");
    }
}
