package fr.euflow.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationEntryPointTests {

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(JsonMapper.builder().build());

    @Test
    void withoutJwtErrorAttribute_writesGenericMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("no credentials"));

        assertEquals(401, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
        assertTrue(response.getContentAsString().contains("Authentification requise"));
    }

    @Test
    void withJwtErrorAttribute_writesSpecificMessage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE, "Le jeton a expiré, reconnectez-vous.");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("expired"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("expiré"));
    }
}
