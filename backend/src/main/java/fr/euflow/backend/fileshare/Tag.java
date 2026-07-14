package fr.euflow.backend.fileshare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entité JPA mappée sur la table {@code tags} — un mot-clé libre attaché à un {@link FileShare}.
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @ManyToOne
    @JoinColumn(name = "file_share_id", nullable = false)
    private FileShare fileShare;

    protected Tag() {
        // JPA
    }

    public Tag(String label, FileShare fileShare) {
        this.label = label;
        this.fileShare = fileShare;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public FileShare getFileShare() {
        return fileShare;
    }
}
