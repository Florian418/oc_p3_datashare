package fr.euflow.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "datashare.storage")
public record StorageProperties(
        String endpoint, // URL de l'API S3 de Garage (http://localhost:3900 en dev)
        String region, // région S3 exigée par le SDK, ici arbitraire ("garage", cf. garage.toml)
        String bucket, // bucket où sont rangés tous les fichiers uploadés (datashare-files)
        String accessKey, // identifiant de la clé d'accès Garage (équivalent AWS_ACCESS_KEY_ID)
        String secretKey // secret associé à la clé d'accès (équivalent AWS_SECRET_ACCESS_KEY)
) {
}
