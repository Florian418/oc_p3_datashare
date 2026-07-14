package fr.euflow.backend.auth;

import fr.euflow.backend.TestcontainersConfiguration;
import fr.euflow.backend.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_withValidPayload_createsUserAndReturns201() throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest("alice@example.com", "s3cret!!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        var saved = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("s3cret!!", saved.getUserPasswordHash()));
    }

    @Test
    void register_withAlreadyUsedEmail_returns409() throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest("bob@example.com", "s3cret!!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest("not-an-email", "s3cret!!"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withTooShortPassword_returns400() throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest("carol@example.com", "abc"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidCredentials_returns200AndToken() throws Exception {
        registerUser("dave@example.com", "s3cret!!");
        String payload = objectMapper.writeValueAsString(new LoginRequest("dave@example.com", "s3cret!!"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        registerUser("erin@example.com", "s3cret!!");
        String payload = objectMapper.writeValueAsString(new LoginRequest("erin@example.com", "wrong-password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        String payload = objectMapper.writeValueAsString(new LoginRequest("unknown@example.com", "s3cret!!"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withInvalidEmail_returns400() throws Exception {
        String payload = objectMapper.writeValueAsString(new LoginRequest("not-an-email", "s3cret!!"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    private void registerUser(String email, String password) throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest(email, password));
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }
}
