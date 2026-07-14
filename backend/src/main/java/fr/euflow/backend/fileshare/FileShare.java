package fr.euflow.backend.fileshare;

import fr.euflow.backend.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA mappée sur la table {@code file_shares} (US01/US02/US07/US09) — un fichier
 * partagé, avec ou sans propriétaire ({@link #user} nul pour un upload anonyme, US07).
 */
@Entity
@Table(name = "file_shares")
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mime;

    @Column(nullable = false)
    private long size;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "share_password_hash")
    private String sharePasswordHash;

    @Column(nullable = false, unique = true)
    private UUID token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "fileShare", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    protected FileShare() {
        // JPA
    }

    public FileShare(String name, String mime, long size, Instant expiresAt, String sharePasswordHash, User user) {
        this.name = name;
        this.mime = mime;
        this.size = size;
        this.expiresAt = expiresAt;
        this.sharePasswordHash = sharePasswordHash;
        this.user = user;
        this.token = UUID.randomUUID();
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = Instant.now();
    }

    /**
     * @param label contenu du tag (jamais {@code null}/vide, vérifié par l'appelant)
     */
    public void addTag(String label) {
        tags.add(new Tag(label, this));
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMime() {
        return mime;
    }

    public long getSize() {
        return size;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getSharePasswordHash() {
        return sharePasswordHash;
    }

    public UUID getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public List<Tag> getTags() {
        return tags;
    }
}
