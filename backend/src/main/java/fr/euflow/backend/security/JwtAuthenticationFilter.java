package fr.euflow.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authentifie chaque requête via un JWT porté en {@code Authorization: Bearer <token>}.
 * Ne rejette jamais directement une requête : si le token est absent ou invalide, aucune
 * {@code Authentication} n'est posée dans le {@link SecurityContextHolder}, et c'est
 * {@link JwtAuthenticationEntryPoint} (déclenché plus loin par Spring Security, uniquement
 * si la route visée exige d'être authentifié) qui renvoie le 401.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Nom de l'attribut de requête utilisé pour transmettre le détail de l'échec à {@link JwtAuthenticationEntryPoint}. */
    public static final String JWT_ERROR_ATTRIBUTE = "jwt.error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                String email = jwtService.extractSubject(token);
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()));
            } catch (ExpiredJwtException e) {
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "Le jeton a expiré, reconnectez-vous.");
            } catch (JwtException | IllegalArgumentException e) {
                // JwtException = signature invalide ou token malformé ; IllegalArgumentException
                // = token vide ("Bearer " sans rien derrière) — catchés explicitement, jamais
                // laissés fuiter en 500.
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "Jeton invalide.");
            }
        }
        filterChain.doFilter(request, response);
    }
}
