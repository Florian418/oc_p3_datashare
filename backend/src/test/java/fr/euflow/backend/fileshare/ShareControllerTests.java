package fr.euflow.backend.fileshare;

import fr.euflow.backend.TestcontainersConfiguration;
import fr.euflow.backend.storage.FileStorageService;
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

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ShareControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileShareRepository fileShareRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void cleanUp() {
        fileShareRepository.deleteAll();
    }

    @Test
    void getMetadata_forFreeShare_returns200WithoutPasswordProtection() throws Exception {
        FileShare fileShare = new FileShare(
                "photo.png", "image/png", 1024, Instant.now().plus(3, ChronoUnit.DAYS), null, null);
        fileShareRepository.save(fileShare);

        mockMvc.perform(get("/api/v1/shares/{token}", fileShare.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("photo.png"))
                .andExpect(jsonPath("$.mime").value("image/png"))
                .andExpect(jsonPath("$.size").value(1024))
                .andExpect(jsonPath("$.passwordProtected").value(false));
    }

    @Test
    void getMetadata_forProtectedShare_returnsPasswordProtectedTrue() throws Exception {
        FileShare fileShare = new FileShare(
                "secret.pdf", "application/pdf", 2048, Instant.now().plus(3, ChronoUnit.DAYS), "hashed", null);
        fileShareRepository.save(fileShare);

        mockMvc.perform(get("/api/v1/shares/{token}", fileShare.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordProtected").value(true));
    }

    @Test
    void getMetadata_forExpiredShare_returns410() throws Exception {
        FileShare fileShare = new FileShare(
                "old.png", "image/png", 1024, Instant.now().minus(1, ChronoUnit.DAYS), null, null);
        fileShareRepository.save(fileShare);

        mockMvc.perform(get("/api/v1/shares/{token}", fileShare.getToken()))
                .andExpect(status().isGone());
    }

    @Test
    void getMetadata_forUnknownToken_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/shares/{token}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMetadata_forMalformedToken_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/shares/{token}", "not-a-uuid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void authenticate_withCorrectPassword_returnsAccessToken() throws Exception {
        FileShare fileShare = aProtectedShare("s3cret!!");

        mockMvc.perform(post("/api/v1/shares/{token}/authenticate", fileShare.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticateShareRequest("s3cret!!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(90));
    }

    @Test
    void authenticate_withWrongPassword_returns401() throws Exception {
        FileShare fileShare = aProtectedShare("s3cret!!");

        mockMvc.perform(post("/api/v1/shares/{token}/authenticate", fileShare.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticateShareRequest("wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticate_forExpiredShare_returns410() throws Exception {
        FileShare fileShare = new FileShare(
                "old.png", "image/png", 1024, Instant.now().minus(1, ChronoUnit.DAYS), passwordEncoder.encode("s3cret!!"), null);
        fileShareRepository.save(fileShare);

        mockMvc.perform(post("/api/v1/shares/{token}/authenticate", fileShare.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticateShareRequest("s3cret!!"))))
                .andExpect(status().isGone());
    }

    @Test
    void download_forFreeShare_returnsFileContent() throws Exception {
        byte[] content = "hello datashare".getBytes();
        FileShare fileShare = new FileShare(
                "hello.txt", "text/plain", content.length, Instant.now().plus(3, ChronoUnit.DAYS), null, null);
        fileShareRepository.save(fileShare);
        fileStorageService.store(fileShare.getToken().toString(), new ByteArrayInputStream(content), content.length, "text/plain");

        var result = mockMvc.perform(get("/api/v1/shares/{token}/download", fileShare.getToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''hello.txt"))
                .andReturn();

        assertArrayEquals(content, result.getResponse().getContentAsByteArray());
    }

    @Test
    void download_forProtectedShareWithoutAccessToken_returns401() throws Exception {
        FileShare fileShare = aProtectedShare("s3cret!!");

        mockMvc.perform(get("/api/v1/shares/{token}/download", fileShare.getToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void download_forProtectedShareWithValidAccessToken_returnsFileContent() throws Exception {
        byte[] content = "protected content".getBytes();
        FileShare fileShare = new FileShare(
                "secret.txt", "text/plain", content.length, Instant.now().plus(3, ChronoUnit.DAYS),
                passwordEncoder.encode("s3cret!!"), null);
        fileShareRepository.save(fileShare);
        fileStorageService.store(fileShare.getToken().toString(), new ByteArrayInputStream(content), content.length, "text/plain");

        var authResponse = mockMvc.perform(post("/api/v1/shares/{token}/authenticate", fileShare.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticateShareRequest("s3cret!!"))))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(authResponse.getResponse().getContentAsString()).get("accessToken").asString();

        var result = mockMvc.perform(get("/api/v1/shares/{token}/download", fileShare.getToken())
                        .param("access_token", accessToken))
                .andExpect(status().isOk())
                .andReturn();

        assertArrayEquals(content, result.getResponse().getContentAsByteArray());
    }

    @Test
    void download_withAccessTokenIssuedForAnotherShare_returns401() throws Exception {
        FileShare shareA = aProtectedShare("s3cret!!");
        FileShare shareB = aProtectedShare("s3cret!!");

        var authResponse = mockMvc.perform(post("/api/v1/shares/{token}/authenticate", shareA.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticateShareRequest("s3cret!!"))))
                .andExpect(status().isOk())
                .andReturn();
        String accessTokenForShareA = objectMapper.readTree(authResponse.getResponse().getContentAsString()).get("accessToken").asString();

        mockMvc.perform(get("/api/v1/shares/{token}/download", shareB.getToken())
                        .param("access_token", accessTokenForShareA))
                .andExpect(status().isUnauthorized());
    }

    private FileShare aProtectedShare(String password) {
        FileShare fileShare = new FileShare(
                "secret.pdf", "application/pdf", 2048, Instant.now().plus(3, ChronoUnit.DAYS), passwordEncoder.encode(password), null);
        return fileShareRepository.save(fileShare);
    }
}
