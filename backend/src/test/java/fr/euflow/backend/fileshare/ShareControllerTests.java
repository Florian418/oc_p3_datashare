package fr.euflow.backend.fileshare;

import fr.euflow.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
