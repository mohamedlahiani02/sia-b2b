package tn.sia.b2b.identity.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.sia.b2b.identity.controller.dto.AccountValidationRequest;
import tn.sia.b2b.identity.domain.User;
import tn.sia.b2b.identity.repository.UserRepository;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import java.util.Map;
import java.util.UUID;

@Service
public class AccountAdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLog;
    private final SessionService sessionService;
    private final JavaMailSender mailSender;

    public AccountAdminService(UserRepository userRepository, AuditLogService auditLog,
                               SessionService sessionService, JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.auditLog = auditLog;
        this.sessionService = sessionService;
        this.mailSender = mailSender;
    }

    @Transactional
    public void validateAccount(UUID userId, AccountValidationRequest req, UUID actorId, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Compte introuvable."));

        if (!user.isPending()) {
            throw new AppException(ErrorCode.ORDER_INVALID_TRANSITION,
                    "Ce compte n'est pas en attente de validation.");
        }

        String beforeStatus = user.getStatus().name();

        if ("APPROVE".equals(req.action())) {
            String ref = req.customerSourceRef();
            if (ref == null || ref.isBlank()) {
                throw new AppException(ErrorCode.CUSTOMER_REF_UNKNOWN,
                        "La référence client YME est obligatoire pour activer un compte.");
            }
            // TODO TBD-04 : vérifier que ref existe dans customer_projection
            // (sera implémenté en Lot 3, après accord client sur la structure YME)
            user.activate(ref);
            userRepository.save(user);
            sendActivationEmail(user.getEmail(), user.getContactName());

        } else if ("REJECT".equals(req.action())) {
            user.close();
            userRepository.save(user);
            sendRejectionEmail(user.getEmail(), user.getContactName(), req.reason());

        } else {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Action invalide. Valeurs acceptées : APPROVE, REJECT.");
        }

        auditLog.record(actorId, req.action().equals("APPROVE") ? "ACCOUNT_ACTIVATED" : "ACCOUNT_REJECTED",
                "users", userId.toString(),
                Map.of("before", beforeStatus, "after", user.getStatus().name(),
                       "reason", req.reason() != null ? req.reason() : ""),
                ip);
    }

    @Transactional
    public void suspendAccount(UUID userId, UUID actorId, String ip) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Compte introuvable."));

        String before = user.getStatus().name();
        user.suspend();
        userRepository.save(user);

        // Révocation immédiate de toutes les sessions actives
        sessionService.invalidateAllUserSessions(userId);

        auditLog.record(actorId, "ACCOUNT_SUSPENDED", "users", userId.toString(),
                Map.of("before", before, "after", "SUSPENDED"), ip);
    }

    private void sendActivationEmail(String email, String contactName) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("[SIA B2B] Votre compte est activé");
            msg.setText("Bonjour " + contactName + ",\n\nVotre compte SIA B2B a été activé. Vous pouvez maintenant vous connecter.");
            mailSender.send(msg);
        } catch (Exception ignored) {}
    }

    private void sendRejectionEmail(String email, String contactName, String reason) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("[SIA B2B] Votre demande de compte");
            msg.setText("Bonjour " + contactName + ",\n\nVotre demande d'ouverture de compte n'a pas pu être acceptée."
                    + (reason != null ? "\n\nMotif : " + reason : "")
                    + "\n\nContactez-nous pour plus d'informations.");
            mailSender.send(msg);
        } catch (Exception ignored) {}
    }
}
