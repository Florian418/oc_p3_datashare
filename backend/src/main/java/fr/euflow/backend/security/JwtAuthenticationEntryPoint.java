package fr.euflow.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Renvoie un 401 au format RFC 7807 ({@link ProblemDetail}) pour toute route protégée
 * appelée sans authentification valide — déclenché par Spring Security dès qu'aucune
 * {@code Authentication} n'est présente pour une route qui l'exige. Seul endroit qui
 * connaît le format de cette réponse, que la cause soit un token absent, expiré ou invalide
 * (cf. {@link JwtAuthenticationFilter#JWT_ERROR_ATTRIBUTE}).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String DEFAULT_DETAIL = "Authentification requise.";

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        Object detail = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, detail != null ? detail.toString() : DEFAULT_DETAIL);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
