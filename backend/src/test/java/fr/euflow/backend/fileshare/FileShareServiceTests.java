package fr.euflow.backend.fileshare;

import fr.euflow.backend.storage.FileStorageService;
import fr.euflow.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires purs (mocks, sans contexte Spring) sur les cas qu'il serait trop coûteux de
 * couvrir via une vraie requête HTTP — en particulier le rejet d'un fichier dépassant 1 Go, qui
 * nécessiterait un vrai payload de cette taille dans un test {@code MockMvc}.
 */
class FileShareServiceTests {

    private static final long MAX_SIZE_BYTES = 1_073_741_824L; // 1 Go

    private final FileShareRepository fileShareRepository = mock(FileShareRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final FileTypeValidator fileTypeValidator = mock(FileTypeValidator.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final FileShareService service = new FileShareService(
            fileShareRepository, userRepository, fileStorageService, fileTypeValidator, passwordEncoder, "http://localhost:4200");

    @Test
    void upload_withFileExceedingMaxSize_throwsFileTooLargeExceptionWithoutTouchingStorage() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(MAX_SIZE_BYTES + 1);

        assertThrows(FileTooLargeException.class, () -> service.upload(file, null, null, List.of()));

        // la vérification de taille doit court-circuiter avant toute lecture du contenu réel
        // (détection du type, stockage) — pas seulement lever la bonne exception.
        verifyNoInteractions(fileTypeValidator);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void upload_withFileAtExactSizeLimit_isAccepted() {
        MultipartFile file = validFile(MAX_SIZE_BYTES);

        assertDoesNotThrow(() -> service.upload(file, null, null, List.of()));
    }

    @Test
    void upload_withPasswordAtMinimumLength_isAccepted() {
        MultipartFile file = validFile(1024);

        assertDoesNotThrow(() -> service.upload(file, null, "abcdef", List.of()));
    }

    @Test
    void upload_withExpiresInDaysAtBoundaries_isAccepted() {
        assertDoesNotThrow(() -> service.upload(validFile(1024), 1, null, List.of()));
        assertDoesNotThrow(() -> service.upload(validFile(1024), 7, null, List.of()));
    }

    @Test
    void upload_withValidFile_storesContentViaFileStorageService() {
        MultipartFile file = validFile(1024);

        service.upload(file, null, null, List.of());

        verify(fileStorageService).store(anyString(), any(), anyLong(), anyString());
    }

    private MultipartFile validFile(long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(size);
        when(file.getOriginalFilename()).thenReturn("photo.png");
        try {
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        when(fileTypeValidator.detectAndValidate(any())).thenReturn("image/png");
        return file;
    }
}
