package fr.euflow.backend.auth;

import fr.euflow.backend.user.User;

import java.time.Instant;

public record UserResponse(Long id, String email, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
