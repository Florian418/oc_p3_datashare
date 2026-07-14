package fr.euflow.backend.auth;

import fr.euflow.backend.security.JwtService;
import fr.euflow.backend.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expose les endpoints d'authentification (US03/US04) : création de compte et connexion.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    /**
     * Authentifie un utilisateur par email/mot de passe et renvoie un JWT à utiliser en
     * {@code Authorization: Bearer} sur les endpoints protégés.
     *
     * @param request email + mot de passe en clair
     * @return 200 avec le token et sa date d'expiration
     * @throws InvalidCredentialsException si l'email est inconnu ou le mot de passe invalide
     *         (401) — la même erreur est renvoyée dans les deux cas, pour ne jamais révéler
     *         l'existence d'un compte via cet endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtService.GeneratedToken token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token.value(), token.expiresAt()));
    }
}
