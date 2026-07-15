package fr.euflow.backend.fileshare;

import fr.euflow.backend.TestcontainersConfiguration;
import fr.euflow.backend.security.JwtService;
import fr.euflow.backend.storage.FileStorageService;
import fr.euflow.backend.user.User;
import fr.euflow.backend.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class FileControllerTests {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    // en-tête DOS/PE ("MZ") — utilisé pour vérifier que le serveur rejette bien un exécutable
    // même déclaré comme .txt/text-plain par le client (le contenu réel prime toujours).
    private static final byte[] PE_HEADER = {'M', 'Z', 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileShareRepository fileShareRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private FileStorageService fileStorageService;

    @AfterEach
    void cleanUp() {
        fileShareRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void upload_asAnonymous_returns201WithNoOwner() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.downloadUrl").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());

        var saved = fileShareRepository.findAll().getFirst();
        assertEquals(null, saved.getUser());
    }

    @Test
    void upload_withValidJwt_isLinkedToOwner() throws Exception {
        User owner = registerUser("owner@example.com", "s3cret!!");
        String token = jwtService.generateToken(owner.getEmail()).value();
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated());

        var saved = fileShareRepository.findAll().getFirst();
        assertEquals(owner.getEmail(), saved.getUser().getEmail());
    }

    @Test
    void upload_withoutExpiresInDays_defaultsToSevenDays() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isCreated());

        var saved = fileShareRepository.findAll().getFirst();
        Instant expected = Instant.now().plus(7, ChronoUnit.DAYS);
        assertTrue(Math.abs(saved.getExpiresAt().getEpochSecond() - expected.getEpochSecond()) < 60);
    }

    @Test
    void upload_withValidExpiresInDays_usesRequestedValue() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file).param("expiresInDays", "3"))
                .andExpect(status().isCreated());

        var saved = fileShareRepository.findAll().getFirst();
        Instant expected = Instant.now().plus(3, ChronoUnit.DAYS);
        assertTrue(Math.abs(saved.getExpiresAt().getEpochSecond() - expected.getEpochSecond()) < 60);
    }

    @Test
    void upload_withValidFile_actuallyStoresContentInGarage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isCreated());

        var saved = fileShareRepository.findAll().getFirst();
        try (var stored = fileStorageService.retrieve(saved.getToken().toString())) {
            assertArrayEquals(PNG_SIGNATURE, stored.readAllBytes());
        }
    }

    @Test
    void upload_withTags_persistsThem() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file)
                        .param("tags", "vacances", "famille"))
                .andExpect(status().isCreated());

        var saved = fileShareRepository.findAll().getFirst();
        assertEquals(2, saved.getTags().size());
    }

    @Test
    void upload_withExecutableContentRenamedAsText_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", PE_HEADER);

        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_withBlankPassword_isTreatedAsUnprotected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file).param("password", ""))
                .andExpect(status().isCreated());

        var saved = fileShareRepository.findAll().getFirst();
        assertEquals(null, saved.getSharePasswordHash());
    }

    @Test
    void upload_withTooShortPassword_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file)
                        .param("password", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_withExpiresInDaysOutOfRange_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);

        mockMvc.perform(multipart("/api/v1/files").file(file)
                        .param("expiresInDays", "8"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_forOwner_returnsOnlyTheirFilesMostRecentFirst() throws Exception {
        User owner = registerUser("owner@example.com", "s3cret!!");
        String jwt = jwtService.generateToken(owner.getEmail()).value();
        String ownerJwtHeader = "Bearer " + jwt;
        MockMultipartFile firstFile = new MockMultipartFile("file", "first.png", "image/png", PNG_SIGNATURE);
        MockMultipartFile secondFile = new MockMultipartFile("file", "second.png", "image/png", PNG_SIGNATURE);
        mockMvc.perform(multipart("/api/v1/files").file(firstFile).header(HttpHeaders.AUTHORIZATION, ownerJwtHeader))
                .andExpect(status().isCreated());
        mockMvc.perform(multipart("/api/v1/files").file(secondFile).header(HttpHeaders.AUTHORIZATION, ownerJwtHeader))
                .andExpect(status().isCreated());
        MockMultipartFile anonymousFile = new MockMultipartFile("file", "anonymous.png", "image/png", PNG_SIGNATURE);
        mockMvc.perform(multipart("/api/v1/files").file(anonymousFile))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/files").header(HttpHeaders.AUTHORIZATION, ownerJwtHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("second.png"))
                .andExpect(jsonPath("$[1].name").value("first.png"))
                .andExpect(jsonPath("$[0].mime").value("image/png"))
                .andExpect(jsonPath("$[0].passwordProtected").value(false));
    }

    @Test
    void list_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/files"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_forOwner_doesNotIncludeAnotherUsersFiles() throws Exception {
        User owner = registerUser("owner@example.com", "s3cret!!");
        String ownerJwt = "Bearer " + jwtService.generateToken(owner.getEmail()).value();
        MockMultipartFile ownFile = new MockMultipartFile("file", "mine.png", "image/png", PNG_SIGNATURE);
        mockMvc.perform(multipart("/api/v1/files").file(ownFile).header(HttpHeaders.AUTHORIZATION, ownerJwt))
                .andExpect(status().isCreated());

        User other = registerUser("other@example.com", "s3cret!!");
        String otherJwt = "Bearer " + jwtService.generateToken(other.getEmail()).value();
        MockMultipartFile otherFile = new MockMultipartFile("file", "not-mine.png", "image/png", PNG_SIGNATURE);
        mockMvc.perform(multipart("/api/v1/files").file(otherFile).header(HttpHeaders.AUTHORIZATION, otherJwt))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/files").header(HttpHeaders.AUTHORIZATION, ownerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("mine.png"));
    }

    @Test
    void delete_asOwner_removesFile() throws Exception {
        User owner = registerUser("owner@example.com", "s3cret!!");
        String jwt = "Bearer " + jwtService.generateToken(owner.getEmail()).value();
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);
        mockMvc.perform(multipart("/api/v1/files").file(file).header(HttpHeaders.AUTHORIZATION, jwt))
                .andExpect(status().isCreated());
        var uploaded = fileShareRepository.findAll().getFirst();
        Long fileId = uploaded.getId();
        String token = uploaded.getToken().toString();

        mockMvc.perform(delete("/api/v1/files/{id}", fileId).header(HttpHeaders.AUTHORIZATION, jwt))
                .andExpect(status().isNoContent());

        assertTrue(fileShareRepository.findById(fileId).isEmpty());
        // pas seulement la ligne en base : le contenu réel doit aussi disparaître de Garage,
        // sinon c'est une fuite de stockage (fichier orphelin jamais nettoyé).
        assertThrows(Exception.class, () -> fileStorageService.retrieve(token).readAllBytes());
    }

    @Test
    void delete_asNonOwner_returns404() throws Exception {
        User owner = registerUser("owner@example.com", "s3cret!!");
        String ownerJwt = "Bearer " + jwtService.generateToken(owner.getEmail()).value();
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);
        mockMvc.perform(multipart("/api/v1/files").file(file).header(HttpHeaders.AUTHORIZATION, ownerJwt))
                .andExpect(status().isCreated());
        Long fileId = fileShareRepository.findAll().getFirst().getId();

        User intruder = registerUser("intruder@example.com", "s3cret!!");
        String intruderJwt = "Bearer " + jwtService.generateToken(intruder.getEmail()).value();

        mockMvc.perform(delete("/api/v1/files/{id}", fileId).header(HttpHeaders.AUTHORIZATION, intruderJwt))
                .andExpect(status().isNotFound());

        assertTrue(fileShareRepository.findById(fileId).isPresent());
    }

    @Test
    void delete_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{id}", 1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void delete_withUnknownId_returns404() throws Exception {
        User owner = registerUser("owner@example.com", "s3cret!!");
        String jwt = "Bearer " + jwtService.generateToken(owner.getEmail()).value();

        mockMvc.perform(delete("/api/v1/files/{id}", 999_999).header(HttpHeaders.AUTHORIZATION, jwt))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_ofAnonymousFile_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_SIGNATURE);
        mockMvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isCreated());
        Long fileId = fileShareRepository.findAll().getFirst().getId();

        User authenticatedUser = registerUser("owner@example.com", "s3cret!!");
        String jwt = "Bearer " + jwtService.generateToken(authenticatedUser.getEmail()).value();

        mockMvc.perform(delete("/api/v1/files/{id}", fileId).header(HttpHeaders.AUTHORIZATION, jwt))
                .andExpect(status().isNotFound());

        assertTrue(fileShareRepository.findById(fileId).isPresent());
    }

    private User registerUser(String email, String password) {
        User user = new User(email, passwordEncoder.encode(password));
        return userRepository.save(user);
    }
}
