package fr.euflow.backend.storage;

/**
 * Levée quand le stockage backend (Garage) est injoignable ou renvoie une erreur — traduite en
 * 503 côté HTTP, pour distinguer une vraie panne d'infrastructure d'une erreur métier classique.
 */
public class StorageUnavailableException extends RuntimeException {

    public StorageUnavailableException(Throwable cause) {
        super("Le service de stockage est momentanément indisponible", cause);
    }
}
