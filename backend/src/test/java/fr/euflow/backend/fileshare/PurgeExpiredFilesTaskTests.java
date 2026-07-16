package fr.euflow.backend.fileshare;

import fr.euflow.backend.storage.FileStorageService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurgeExpiredFilesTaskTests {

    private final FileShareRepository fileShareRepository = mock(FileShareRepository.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final PurgeExpiredFilesTask task = new PurgeExpiredFilesTask(fileShareRepository, fileStorageService);

    @Test
    void purgeExpiredFiles_deletesStorageContentAndRepositoryRowForEachExpiredShare() {
        FileShare expiredOne = new FileShare("a.png", "image/png", 100, Instant.now().minusSeconds(3600), null, null);
        FileShare expiredTwo = new FileShare("b.png", "image/png", 200, Instant.now().minusSeconds(60), null, null);
        when(fileShareRepository.findByExpiresAtBefore(any())).thenReturn(List.of(expiredOne, expiredTwo));

        task.purgeExpiredFiles();

        verify(fileStorageService).delete(expiredOne.getToken().toString());
        verify(fileStorageService).delete(expiredTwo.getToken().toString());
        verify(fileShareRepository).delete(expiredOne);
        verify(fileShareRepository).delete(expiredTwo);
    }

    @Test
    void purgeExpiredFiles_withNoExpiredShares_deletesNothing() {
        when(fileShareRepository.findByExpiresAtBefore(any())).thenReturn(List.of());

        task.purgeExpiredFiles();

        verify(fileStorageService, never()).delete(any());
        verify(fileShareRepository, never()).delete(any(FileShare.class));
    }
}
