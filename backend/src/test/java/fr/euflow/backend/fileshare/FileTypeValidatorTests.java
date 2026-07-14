package fr.euflow.backend.fileshare;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileTypeValidatorTests {

    private final FileTypeValidator validator = new FileTypeValidator();

    @Test
    void detectAndValidate_withPngContent_returnsImagePng() {
        byte[] pngSignature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        String detected = validator.detectAndValidate(new ByteArrayInputStream(pngSignature));

        assertEquals("image/png", detected);
    }

    @Test
    void detectAndValidate_withPlainTextContent_returnsTextPlain() {
        byte[] content = "Hello, DataShare!".getBytes();

        String detected = validator.detectAndValidate(new ByteArrayInputStream(content));

        assertEquals("text/plain", detected);
    }

    @Test
    void detectAndValidate_withExecutableContentRenamedAsText_throwsFileTypeNotAllowed() {
        // en-tête DOS/PE ("MZ") d'un exécutable Windows — le contenu réel est vérifié, pas le
        // nom de fichier ni le Content-Type déclaré par le client (cas typique : un .exe
        // renommé en .txt pour contourner un contrôle basé sur l'extension).
        byte[] peHeader = {'M', 'Z', 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF};

        assertThrows(FileTypeNotAllowedException.class,
                () -> validator.detectAndValidate(new ByteArrayInputStream(peHeader)));
    }
}
