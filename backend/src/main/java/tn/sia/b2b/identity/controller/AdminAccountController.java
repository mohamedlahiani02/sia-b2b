package tn.sia.b2b.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.sia.b2b.identity.controller.dto.AccountValidationRequest;
import tn.sia.b2b.identity.domain.User;
import tn.sia.b2b.identity.repository.UserRepository;
import tn.sia.b2b.identity.service.AccountAdminService;
import tn.sia.b2b.identity.service.SessionService;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@Tag(name = "Administration — Comptes", description = "Validation et gestion des comptes professionnels")
public class AdminAccountController {

    private final AccountAdminService accountAdminService;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    public AdminAccountController(AccountAdminService accountAdminService,
                                   UserRepository userRepository,
                                   SessionService sessionService) {
        this.accountAdminService = accountAdminService;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Liste des comptes en attente de validation")
    public List<UserSummary> listPending() {
        return userRepository.findAllByStatus(tn.sia.b2b.identity.domain.UserStatus.PENDING)
                .stream().map(UserSummary::from).toList();
    }

    @PostMapping("/{userId}/validate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Validation ou refus d'un compte professionnel",
               description = "APPROVE active le compte et y rattache la référence client YME. " +
                             "REJECT ferme la demande avec motif. " +
                             "Note TBD-04 : vérification d'existence de customerSourceRef dans customer_projection à implémenter en Lot 3.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Action effectuée"),
        @ApiResponse(responseCode = "404", description = "Compte introuvable"),
        @ApiResponse(responseCode = "409", description = "Compte non en attente ou référence inconnue")
    })
    public void validate(@PathVariable UUID userId,
                         @Valid @RequestBody AccountValidationRequest req,
                         HttpServletRequest request) {
        UUID actorId = resolveActorId(request);
        accountAdminService.validateAccount(userId, req, actorId, request.getRemoteAddr());
    }

    @PostMapping("/{userId}/suspend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspension d'un compte — révoque toutes les sessions actives immédiatement")
    public void suspend(@PathVariable UUID userId, HttpServletRequest request) {
        UUID actorId = resolveActorId(request);
        accountAdminService.suspendAccount(userId, actorId, request.getRemoteAddr());
    }

    private UUID resolveActorId(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var c : request.getCookies()) {
            if ("sia_session".equals(c.getName())) {
                SessionService.SessionData s = sessionService.getSession(c.getValue());
                return s != null ? s.userId() : null;
            }
        }
        return null;
    }

    public record UserSummary(UUID id, String email, String companyName, String contactName,
                              String status, String createdAt) {
        static UserSummary from(User u) {
            return new UserSummary(u.getId(), u.getEmail(), u.getCompanyName(),
                    u.getContactName(), u.getStatus().name(), u.getCreatedAt().toString());
        }
    }
}
