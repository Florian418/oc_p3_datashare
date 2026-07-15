package fr.euflow.backend.fileshare;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Les autres gestionnaires de {@link FileShareExceptionHandler} sont couverts indirectement via
 * de vraies requêtes {@code MockMvc} (voir {@code FileControllerTests}/{@code
 * ShareControllerTests}). Les deux cas ci-dessous ne le sont pas : ils nécessiteraient un
 * payload multipart de plusieurs centaines de Mo à 2 Go pour être déclenchés réellement.
 */
class FileShareExceptionHandlerTests {

    private final FileShareExceptionHandler handler = new FileShareExceptionHandler();

    @Test
    void handleFileTooLarge_returns413WithBusinessLimitMessage() {
        ProblemDetail problemDetail = handler.handleFileTooLarge(new FileTooLargeException(1_073_741_824L));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE.value(), problemDetail.getStatus());
        assertEquals("La taille du fichier dépasse la limite autorisée (1073741824 octets)", problemDetail.getDetail());
    }

    @Test
    void handleMaxUploadSizeExceeded_returns413WithGenericMessage() {
        ProblemDetail problemDetail = handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(2_147_483_648L));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE.value(), problemDetail.getStatus());
        assertEquals("Le fichier envoyé est trop volumineux", problemDetail.getDetail());
    }
}
