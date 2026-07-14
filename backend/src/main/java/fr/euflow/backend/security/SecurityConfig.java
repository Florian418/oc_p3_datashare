package fr.euflow.backend.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration transverse de sécurité : API 100% stateless (JWT Bearer, jamais de cookie
 * de session), routes publiques vs authentifiées, et l'encodeur de mot de passe partagé par
 * tout le module auth.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * @return la chaîne de filtres de sécurité : CSRF désactivé et session stateless (pas de
     *         cookie, cohérent avec une API 100% JWT Bearer), routes techniques et
     *         {@code /api/v1/auth/**} publiques, le reste authentifié par défaut
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // API stateless (JWT Bearer à venir, jamais de cookie de session) : le CSRF protège les flux
        // basés sur les cookies, il n'a pas de sens ici et bloquerait les POST sans jeton dédié.
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * @return l'encodeur BCrypt utilisé pour hacher/vérifier les mots de passe (register/login)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
