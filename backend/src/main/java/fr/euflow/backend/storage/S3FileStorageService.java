package fr.euflow.backend.storage;

import java.io.InputStream;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Implémentation {@link FileStorageService} qui délègue à Garage via le SDK S3 — le seul
 * endroit du code qui connaît concrètement S3/Garage, le reste de l'application ne manipule
 * que l'interface.
 */
@Service
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.bucket();
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength));
    }

    @Override
    public InputStream retrieve(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.getObject(request);
    }

    @Override
    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }
}
