package fr.euflow.backend.storage;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Fabrique le client S3 (SDK AWS) utilisé pour parler à Garage plutôt qu'à la vraie AWS —
 * un seul {@link S3Client}, construit une fois au démarrage, réutilisé par tout le code de
 * stockage.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3ClientConfig {

    /**
     * @param properties config Garage résolue depuis {@code datashare.storage.*}
     * @return le client S3 paramétré pour Garage : endpoint custom (sinon il irait taper
     *         {@code amazonaws.com}) et {@code forcePathStyle} (Garage n'a pas de DNS
     *         virtuel par bucket)
     */
    @Bean
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .forcePathStyle(true)
                // Garage ne supporte pas le mode de signature "streaming avec trailer de
                // checksum" devenu la valeur par défaut du SDK AWS récent (uploads en
                // InputStream) — sans ce réglage, tout PutObject échoue avec "Invalid payload
                // signature". WHEN_REQUIRED revient à l'ancien mode de signature (chunked SigV4
                // classique), toujours en flux, sans charger le fichier entier en mémoire.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
