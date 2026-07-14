package fr.euflow.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entité JPA mappée sur la table {@code users} (compte utilisateur, US03/US04).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "user_password_hash", nullable = false)
    private String userPasswordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
        // JPA
    }

    public User(String email, String userPasswordHash) {
        this.email = email;
        this.userPasswordHash = userPasswordHash;
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUserPasswordHash() {
        return userPasswordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
