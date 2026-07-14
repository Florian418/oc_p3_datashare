package fr.euflow.backend.storage;

import java.io.InputStream;

/**
 * Contrat métier de stockage de fichiers ("port", au sens de l'architecture hexagonale) :
 * stocker/récupérer/supprimer un fichier via une clé, sans jamais dire *comment* ni *où*.
 * Le code métier (upload, téléchargement...) ne dépend que de cette interface, jamais d'un
 * SDK concret — permet de changer de fournisseur (Garage, Cloudflare R2, AWS S3...) sans
 * toucher au code métier.
 */
public interface FileStorageService {

    /**
     * @param key identifiant unique du fichier dans le stockage
     * @param content flux du contenu à écrire
     * @param contentLength taille en octets du contenu (requise par le SDK S3)
     * @param contentType type MIME du fichier
     */
    void store(String key, InputStream content, long contentLength, String contentType);

    /**
     * @param key identifiant unique du fichier dans le stockage
     * @return un flux sur le contenu du fichier
     */
    InputStream retrieve(String key);

    /**
     * @param key identifiant unique du fichier dans le stockage
     */
    void delete(String key);
}
