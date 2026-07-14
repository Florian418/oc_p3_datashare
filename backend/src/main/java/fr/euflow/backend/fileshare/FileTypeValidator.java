package fr.euflow.backend.fileshare;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * Détecte le type réel d'un fichier à partir de son contenu (signature binaire, "magic bytes")
 * et le vérifie contre une liste blanche — jamais l'extension du nom de fichier ni le
 * {@code Content-Type} envoyé par le client, tous deux falsifiables (ex. un exécutable renommé
 * en {@code .txt}). Utilise uniquement {@link Tika#detect}, pas les parseurs de contenu
 * (module {@code tika-parsers}, non utilisé ici) : la détection ne lit que l'en-tête du
 * fichier, elle n'ouvre ni ne décompresse jamais son contenu.
 */
@Component
public class FileTypeValidator {

    /**
     * Liste blanche des types MIME acceptés — images, audio, vidéo, documents bureautiques et
     * archives. Le SVG est volontairement exclu des images : contrairement à un PNG/JPEG (pixels
     * bruts), un SVG est du XML pouvant embarquer du JavaScript (risque XSS). Le contenu des
     * archives n'est pas inspecté (seul le conteneur est vérifié) : limitation documentée.
     */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            // images
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/tiff",
            // audio
            "audio/mpeg", "audio/wav", "audio/x-wav", "audio/ogg", "audio/flac", "audio/aac", "audio/mp4",
            // vidéo
            "video/mp4", "video/mpeg", "video/quicktime", "video/webm", "video/x-msvideo", "video/ogg",
            // documents
            "application/pdf", "text/plain", "application/rtf",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text", "application/vnd.oasis.opendocument.spreadsheet",
            // archives
            "application/zip", "application/x-7z-compressed", "application/vnd.rar",
            "application/x-tar", "application/gzip", "application/x-gzip"
    );

    private final Tika tika = new Tika();

    /**
     * @param content flux positionné au début du fichier à analyser
     * @return le type MIME réel détecté
     * @throws FileTypeNotAllowedException si ce type n'est pas dans la liste blanche
     */
    public String detectAndValidate(InputStream content) {
        String detectedType;
        try {
            detectedType = tika.detect(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire le fichier pour en détecter le type", e);
        }

        if (!ALLOWED_MIME_TYPES.contains(detectedType)) {
            throw new FileTypeNotAllowedException(detectedType);
        }
        return detectedType;
    }
}
