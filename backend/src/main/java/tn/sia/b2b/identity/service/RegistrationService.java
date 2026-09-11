package tn.sia.b2b.identity.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.sia.b2b.identity.controller.dto.RegistrationRequest;
import tn.sia.b2b.identity.domain.User;
import tn.sia.b2b.identity.repository.UserRepository;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import java.util.Map;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLog;
    private final JavaMailSender mailSender;
    private final String adminEmail;

    public RegistrationService(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               AuditLogService auditLog,
                               JavaMailSender mailSender,
                               @Value("${app.admin-email}") String adminEmail) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLog = auditLog;
        this.mailSender = mailSender;
        this.adminEmail = adminEmail;
    }

    @Transactional
    public User register(RegistrationRequest req) {
        // Message uniforme : ne révèle pas si l'email est déjà utilisé
        if (userRepository.existsByEmail(req.email().toLowerCase().trim())) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS,
                    "Un compte avec cet email existe déjà ou la demande ne peut être traitée.");
        }

        String hash = passwordEncoder.encode(req.password());

        User user = User.createPending(
                UuidCreator.getTimeOrderedEpoch(),
                req.email(),
                hash,
                req.companyName(),
                req.taxId(),
                req.contactName(),
                req.phone(),
                req.address()
        );

        userRepository.save(user);

        // Notification back-office : audit_log + email SIA
        auditLog.record(null, "REGISTRATION_REQUESTED", "users", user.getId().toString(),
                Map.of("email", user.getEmail(), "company", user.getCompanyName()), null);

        notifyAdmin(user);

        return user;
    }

    private void notifyAdmin(User user) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(adminEmail);
            msg.setSubject("[SIA B2B] Nouvelle demande d'ouverture de compte");
            msg.setText("Nouvelle demande de : " + user.getCompanyName() + " (" + user.getEmail() + ")\n"
                    + "Contact : " + user.getContactName() + "\n"
                    + "Identifiant fiscal : " + user.getTaxId());
            mailSender.send(msg);
        } catch (Exception e) {
            // L'échec de l'email ne doit pas faire échouer l'inscription
        }
    }
}
