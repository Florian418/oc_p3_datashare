package fr.euflow.backend.fileshare;

import fr.euflow.backend.storage.FileStorageService;
import fr.euflow.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
}
