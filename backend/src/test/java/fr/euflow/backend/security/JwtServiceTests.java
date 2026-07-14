package fr.euflow.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTests {

    private static final String SECRET = Base64.getEncoder().encodeToString("test-secret-at-least-32-bytes-long".getBytes());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));
    }

    @Test
    void generateToken_producesAValidSignedTokenWithTheRightClaims() {
        JwtService.GeneratedToken token = jwtService.generateToken("alice@example.com");

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token.value()).getPayload();

        assertEquals("alice@example.com", claims.getSubject());
        assertEquals(token.expiresAt(), claims.getExpiration().toInstant());
        assertTrue(claims.getExpiration().toInstant().isAfter(Instant.now()));
    }

    @Test
    void generateToken_withCustomTtl_usesThatDurationInsteadOfTheDefault() {
        JwtService.GeneratedToken token = jwtService.generateToken("share-token", Duration.ofSeconds(90));

        Instant expected = Instant.now().plusSeconds(90);
        assertTrue(Duration.between(token.expiresAt(), expected).abs().getSeconds() < 2);
        assertEquals("share-token", jwtService.extractSubject(token.value()));
    }
}
