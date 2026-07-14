package fr.euflow.backend.auth;

import fr.euflow.backend.user.User;
import fr.euflow.backend.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}
