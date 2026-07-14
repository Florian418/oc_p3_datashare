package fr.euflow.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration de la signature JWT, liée aux clés {@code datashare.jwt.*} (elles-mêmes
 * résolues depuis les variables d'environnement {@code JWT_*}, cf. {@code .env}).
 *
 * @param secret clé HMAC en Base64, signe et vérifie les JWT (jamais commise)
 * @param expiration durée de validité d'un token émis à la connexion
 */
@ConfigurationProperties(prefix = "datashare.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
