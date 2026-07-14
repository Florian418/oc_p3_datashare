package fr.euflow.backend.storage;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * Étend le healthcheck Actuator au stockage Garage — Actuator ne connaît nativement que le
 * {@code DataSource}, rien pour un {@link S3Client} maison. Vérifie l'accessibilité du
 * bucket configuré via un simple {@code HEAD}.
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final String bucket;

    public StorageHealthIndicator(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.bucket();
    }

    @Override
    public Health health() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
