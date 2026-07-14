package fr.euflow.backend.auth;

import fr.euflow.backend.security.JwtService;
import fr.euflow.backend.user.User;
import fr.euflow.backend.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Logique métier de l'authentification (US03/US04) : création de compte et connexion.
 * Ne connaît rien du HTTP (pas de code de statut ici) — seulement les règles métier.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()));
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // deux requêtes concurrentes avec le même email : la contrainte UNIQUE en base tranche,
            // le check existsByEmail() ci-dessus ne suffit pas seul à l'empêcher (race condition).
            throw new EmailAlreadyUsedException(request.email());
        }
    }

    /**
     * Vérifie les identifiants et émet un JWT en cas de succès.
     *
     * @param request email + mot de passe en clair
     * @return le token signé et sa date d'expiration
     * @throws InvalidCredentialsException si l'email est inconnu OU le mot de passe est
     *         invalide — volontairement la même exception dans les deux cas, pour ne jamais
     *         révéler l'existence d'un compte via cet endpoint (contrairement à
     *         {@link #register}, où le conflit d'email fait partie du contrat métier)
     */
    public JwtService.GeneratedToken login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(user -> passwordEncoder.matches(request.password(), user.getUserPasswordHash()))
                .map(user -> jwtService.generateToken(user.getEmail()))
                .orElseThrow(InvalidCredentialsException::new);
    }
}
