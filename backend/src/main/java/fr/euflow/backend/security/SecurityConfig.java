package fr.euflow.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration transverse de sécurité : API 100% stateless (JWT Bearer, jamais de cookie
 * de session), routes publiques vs authentifiées, CORS pour le frontend Angular, et
 * l'encodeur de mot de passe partagé par tout le module auth.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * @param jwtAuthenticationFilter pose l'{@code Authentication} dans le contexte de
     *         sécurité à partir du JWT Bearer, sans jamais rejeter directement une requête
     * @param jwtAuthenticationEntryPoint renvoie le 401 (RFC 7807) quand une route protégée
     *         est appelée sans authentification valide
     * @param corsConfigurationSource origine(s) autorisée(s) à appeler l'API depuis un navigateur
     * @return la chaîne de filtres de sécurité : CSRF désactivé et session stateless (pas de
     *         cookie, cohérent avec une API 100% JWT Bearer), routes techniques et
     *         {@code /api/v1/auth/**} publiques, le reste authentifié par défaut
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        // API stateless (JWT Bearer, jamais de cookie de session) : le CSRF protège les flux
        // basés sur les cookies, il n'a pas de sens ici et bloquerait les POST sans jeton dédié.
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // authentification optionnelle (US01 avec compte / US07 anonyme) : la
                        // route reste publique, FileShareService lit lui-même le contexte de
                        // sécurité pour savoir si un utilisateur est authentifié
                        .requestMatchers(HttpMethod.POST, "/api/v1/files").permitAll()
                        // US02 : accès public par token, jamais de JWT requis
                        .requestMatchers("/api/v1/shares/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * @param allowedOrigin origine du frontend autorisée à appeler l'API depuis un navigateur
     *         (résolue depuis {@code datashare.cors.allowed-origin}, {@code localhost:4200} en
     *         dev par défaut)
     * @return la configuration CORS appliquée à toutes les routes {@code /api/v1/**} — méthodes
     *         REST standard, en-têtes {@code Content-Type}/{@code Authorization} autorisés
     *         (ce dernier n'est pas un en-tête CORS "safelisted" par défaut)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${datashare.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }

    /**
     * @return l'encodeur BCrypt utilisé pour hacher/vérifier les mots de passe (register/login)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
