package tn.sia.b2b.identity.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.sia.b2b.identity.domain.PasswordResetToken;
import tn.sia.b2b.identity.domain.User;
import tn.sia.b2b.identity.repository.PasswordResetTokenRepository;
import tn.sia.b2b.identity.repository.UserRepository;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;
    private final JavaMailSender mailSender;
    private final String appBaseUrl;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       SessionService sessionService,
                       RateLimitService rateLimitService,
                       JavaMailSender mailSender,
                       @Value("${app.base-url:http://localhost:8080}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.rateLimitService = rateLimitService;
        this.mailSender = mailSender;
        this.appBaseUrl = appBaseUrl;
    }

    @Transactional
    public LoginResult login(String email, String password) {
        if (!rateLimitService.isLoginAllowed(email)) {
            throw new AppException(ErrorCode.RATE_LIMITED,
                    "Trop de tentatives. Réessayez dans 15 minutes.");
        }

        Optional<User> opt = userRepository.findByEmail(email.toLowerCase().trim());

        // Vérification du hash même si le compte n'existe pas (protection timing)
        boolean valid = opt.map(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElse(false);

        if (!valid) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Identifiants invalides.");
        }

        User user = opt.get();
        rateLimitService.resetLoginAttempts(email);

        if (user.isPending()) {
            throw new AppException(ErrorCode.AUTH_ACCOUNT_PENDING,
                    "Votre compte est en attente de validation par SIA.");
        }
        if (user.isSuspended()) {
            throw new AppException(ErrorCode.AUTH_ACCOUNT_SUSPENDED,
                    "Votre compte a été suspendu. Contactez SIA.");
        }

        String token = sessionService.createSession(user.getId(), user.getRole().name(),
                user.getCustomerSourceRef());

        return new LoginResult(token, user);
    }

    public void logout(String token) {
        if (token != null) {
            sessionService.invalidateSession(token);
        }
    }

    @Transactional
    public void requestPasswordReset(String email) {
        // Réponse identique, compte existant ou non
        Optional<User> opt = userRepository.findByEmail(email.toLowerCase().trim());
        if (opt.isEmpty()) return;

        User user = opt.get();
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        resetTokenRepository.save(PasswordResetToken.create(
                UuidCreator.getTimeOrderedEpoch(), user.getId(), tokenHash, expiresAt));

        sendResetEmail(user.getEmail(), rawToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = sha256(rawToken);
        PasswordResetToken token = resetTokenRepository.findByTokenHash(tokenHash)
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS,
                        "Lien de réinitialisation invalide ou expiré."));

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, "Utilisateur introuvable."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.markUsed();
        resetTokenRepository.save(token);

        sessionService.invalidateAllUserSessions(user.getId());
    }

    private void sendResetEmail(String email, String rawToken) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("[SIA B2B] Réinitialisation de votre mot de passe");
            msg.setText("Cliquez sur le lien suivant pour réinitialiser votre mot de passe (valable 1 heure) :\n"
                    + appBaseUrl + "/connexion/reinitialiser?token=" + rawToken);
            mailSender.send(msg);
        } catch (Exception e) {
            // Échec silencieux : la réponse est identique dans tous les cas
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }

    public record LoginResult(String sessionToken, User user) {}
}
