package fr.euflow.backend.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du stockage Garage, liée aux clés {@code datashare.storage.*} (elles-mêmes
 * résolues depuis les variables d'environnement {@code GARAGE_*}, cf. {@code .env}).
 *
 * @param endpoint URL de l'API S3 de Garage ({@code http://localhost:3900} en dev)
 * @param region région S3 exigée par le SDK, ici arbitraire ({@code garage}, cf. garage.toml)
 * @param bucket bucket où sont rangés tous les fichiers uploadés
 * @param accessKey identifiant de la clé d'accès Garage (équivalent {@code AWS_ACCESS_KEY_ID})
 * @param secretKey secret associé à la clé d'accès (équivalent {@code AWS_SECRET_ACCESS_KEY})
 */
@ConfigurationProperties(prefix = "datashare.storage")
public record StorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey
) {
}
