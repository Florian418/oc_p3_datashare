package fr.euflow.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * Émet et vérifie des JWT signés (HMAC) pour authentifier les appels aux endpoints protégés.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.secret()));
        this.expiration = properties.expiration();
    }

    /**
     * Génère un JWT signé pour le sujet donné, valide pour la durée configurée
     * ({@code datashare.jwt.expiration}).
     *
     * @param subject identifiant du porteur du token (l'email de l'utilisateur connecté)
     * @return le token compact ({@code header.payload.signature}) et sa date d'expiration
     */
    public GeneratedToken generateToken(String subject) {
        Instant now = Instant.now();
        // tronqué à la seconde : le claim JWT "exp" n'encode que des secondes (RFC 7519,
        // NumericDate) — sans ça, expiresAt() renverrait une valeur plus précise que ce que
        // le token contient réellement.
        Instant expiresAt = now.plus(expiration).truncatedTo(ChronoUnit.SECONDS);
        String value = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new GeneratedToken(value, expiresAt);
    }

    /**
     * Vérifie la signature et l'expiration d'un token, et en extrait le sujet.
     *
     * @param token le token JWT compact à vérifier
     * @return le sujet encodé dans le token (l'email de l'utilisateur)
     * @throws io.jsonwebtoken.ExpiredJwtException si le token est expiré (signature par
     *         ailleurs valide) — catchée séparément par l'appelant, qui distingue ce cas
     *         des autres échecs de validation
     * @throws io.jsonwebtoken.JwtException si la signature est invalide ou le token malformé
     * @throws IllegalArgumentException si le token est vide ou {@code null}
     */
    public String extractSubject(String token) {
        JwtParser parser = Jwts.parser()
                .verifyWith(key)
                .build();

        // vérifie signature + expiration, lève une exception si l'une des deux est invalide
        Jws<Claims> signedToken = parser.parseSignedClaims(token);
        Claims claims = signedToken.getPayload();

        return claims.getSubject();
    }

    /**
     * @param value token JWT compact ({@code header.payload.signature})
     * @param expiresAt date d'expiration exacte encodée dans le token (tronquée à la
     *         seconde, comme le claim {@code exp} lui-même — RFC 7519)
     */
    public record GeneratedToken(String value, Instant expiresAt) {
    }
}
