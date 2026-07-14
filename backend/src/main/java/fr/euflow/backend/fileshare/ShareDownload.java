package fr.euflow.backend.fileshare;

import java.io.InputStream;

/**
 * Contenu et métadonnées nécessaires à {@link ShareController} pour construire la réponse HTTP
 * de téléchargement (flux binaire, jamais chargé entièrement en mémoire).
 *
 * @param content flux du contenu du fichier
 * @param filename nom original du fichier
 * @param mimeType type MIME réel détecté à l'upload
 * @param size taille en octets
 */
public record ShareDownload(InputStream content, String filename, String mimeType, long size) {
}
