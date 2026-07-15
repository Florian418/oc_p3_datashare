package fr.euflow.backend.fileshare;

import fr.euflow.backend.storage.StorageUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduit les exceptions métier du package {@code fileshare} en réponses HTTP normalisées
 * (RFC 7807 / {@link ProblemDetail}).
 */
@RestControllerAdvice
public class FileShareExceptionHandler {

    @ExceptionHandler(FileTooLargeException.class)
    public ProblemDetail handleFileTooLarge(FileTooLargeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage());
    }

    /**
     * @return 413 quand le fichier dépasse le plafond technique du serveur applicatif
     *         ({@code spring.servlet.multipart.max-file-size}) — rejeté avant même d'atteindre
     *         {@link FileShareService}, distinct de {@link FileTooLargeException} (la vraie
     *         limite métier, 1 Go)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, "Le fichier envoyé est trop volumineux");
    }

    @ExceptionHandler({FileTypeNotAllowedException.class, InvalidSharePasswordException.class, InvalidExpirationException.class})
    public ProblemDetail handleInvalidUpload(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({ShareNotFoundException.class, FileNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ShareExpiredException.class)
    public ProblemDetail handleShareExpired(ShareExpiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler({SharePasswordMismatchException.class, InvalidAccessTokenException.class})
    public ProblemDetail handleShareAccessDenied(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(StorageUnavailableException.class)
    public ProblemDetail handleStorageUnavailable(StorageUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }
}
