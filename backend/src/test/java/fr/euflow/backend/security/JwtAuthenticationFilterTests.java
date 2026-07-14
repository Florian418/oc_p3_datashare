package fr.euflow.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTests {

    private static final String SECRET = Base64.getEncoder().encodeToString("test-secret-at-least-32-bytes-long".getBytes());

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsAuthenticationAndContinuesChain() throws Exception {
        String token = jwtService.generateToken("alice@example.com").value();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        assertEquals("alice@example.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(request, response);
    }

    @Test
    void expiredToken_leavesSecurityContextEmptyAndSetsExpiredMessage() throws Exception {
        JwtService expiredTokenService = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-10)));
        String token = expiredTokenService.generateToken("bob@example.com").value();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(request).setAttribute(eq(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE), contains("expiré"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void malformedToken_leavesSecurityContextEmptyAndSetsInvalidMessage() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-real-token");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(request).setAttribute(eq(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE), contains("invalide"));
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenSignedWithAnotherKey_isTreatedAsInvalid() throws Exception {
        String otherSecret = Base64.getEncoder().encodeToString("a-completely-different-secret-32b".getBytes());
        JwtService otherJwtService = new JwtService(new JwtProperties(otherSecret, Duration.ofHours(1)));
        String token = otherJwtService.generateToken("mallory@example.com").value();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(request).setAttribute(eq(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE), contains("invalide"));
    }

    @Test
    void noAuthorizationHeader_leavesSecurityContextEmptyAndSetsNoAttribute() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(request, never()).setAttribute(eq(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE), any());
        verify(chain).doFilter(request, response);
    }
}
