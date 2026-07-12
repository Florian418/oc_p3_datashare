package fr.euflow.backend.storage;

import java.io.InputStream;

public interface FileStorageService {

    void store(String key, InputStream content, long contentLength, String contentType);

    InputStream retrieve(String key);

    void delete(String key);
}
