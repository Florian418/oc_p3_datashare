package fr.euflow.backend.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires purs (mock du {@link S3Client}, sans conteneur Garage réel) sur la traduction
 * d'une panne de stockage en {@link StorageUnavailableException} — le scénario "Garage
 * injoignable" n'est pas reproductible via un test d'intégration classique.
 */
class S3FileStorageServiceTests {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3FileStorageService service =
            new S3FileStorageService(s3Client, new StorageProperties(null, null, "bucket", null, null));

    @Test
    void store_whenS3ClientFails_throwsStorageUnavailableException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkException.builder().message("connection refused").build());

        assertThrows(StorageUnavailableException.class,
                () -> service.store("token", new ByteArrayInputStream(new byte[]{1}), 1, "image/png"));
    }

    @Test
    void retrieve_whenS3ClientFails_throwsStorageUnavailableException() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(SdkException.builder().message("connection refused").build());

        assertThrows(StorageUnavailableException.class, () -> service.retrieve("token"));
    }

    @Test
    void delete_whenS3ClientFails_throwsStorageUnavailableException() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkException.builder().message("connection refused").build());

        assertThrows(StorageUnavailableException.class, () -> service.delete("token"));
    }
}
