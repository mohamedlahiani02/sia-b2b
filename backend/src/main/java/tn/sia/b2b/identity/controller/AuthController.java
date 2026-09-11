package tn.sia.b2b.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.sia.b2b.identity.controller.dto.*;
import tn.sia.b2b.identity.domain.User;
import tn.sia.b2b.identity.service.AuthService;
import tn.sia.b2b.identity.service.RegistrationService;
import tn.sia.b2b.identity.service.SessionService;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;
import tn.sia.b2b.shared.error.ErrorResponse;
import tn.sia.b2b.identity.service.RateLimitService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Inscription, connexion, session, réinitialisation mot de passe")
public class AuthController {

    private static final String SESSION_COOKIE = "sia_session";

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;

    public AuthController(RegistrationService registrationService, AuthService authService,
                          SessionService sessionService, RateLimitService rateLimitService) {
        this.registrationService = registrationService;
        this.authService = authService;
        this.sessionService = sessionService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Demande d'ouverture de compte professionnel")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Demande enregistrée"),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé"),
        @ApiResponse(responseCode = "422", description = "Champs invalides"),
        @ApiResponse(responseCode = "429", description = "Trop de demandes depuis cette IP")
    })
    public Map<String, String> register(@Valid @RequestBody RegistrationRequest req,
                                        HttpServletRequest request) {
        String ip = resolveClientIp(request);
        if (!rateLimitService.isRegisterAllowed(ip)) {
            throw new AppException(ErrorCode.RATE_LIMITED,
                    "Trop de demandes depuis cette adresse. Réessayez dans 1 heure.");
        }
        registrationService.register(req);
        return Map.of(
            "status", "PENDING",
            "message", "Demande enregistrée. Validation par SIA sous 24 h ouvrées."
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Ouverture de session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session ouverte, cookie posé"),
        @ApiResponse(responseCode = "401", description = "Identifiants invalides"),
        @ApiResponse(responseCode = "403", description = "Compte en attente ou suspendu"),
        @ApiResponse(responseCode = "429", description = "Trop de tentatives")
    })
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req,
                                                      HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(req.email(), req.password());

        Cookie cookie = new Cookie(SESSION_COOKIE, result.sessionToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge((int) (8 * 3600));
        response.addCookie(cookie);

        User user = result.user();
        return ResponseEntity.ok(Map.of(
            "user", Map.of(
                "id", user.getId().toString(),
                "contactName", user.getContactName(),
                "companyName", user.getCompanyName(),
                "role", user.getRole().name(),
                "customerLinked", user.isLinkedToCustomer()
            )
        ));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Révocation de session côté serveur")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractSessionToken(request);
        authService.logout(token);

        Cookie cookie = new Cookie(SESSION_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    @GetMapping("/me")
    @Operation(summary = "Session courante")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session active"),
        @ApiResponse(responseCode = "401", description = "Session absente ou expirée")
    })
    public Map<String, Object> me(HttpServletRequest request) {
        String token = extractSessionToken(request);
        if (token == null) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Session absente ou expirée.");
        }
        SessionService.SessionData session = sessionService.getSession(token);
        if (session == null) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Session absente ou expirée.");
        }
        return Map.of(
            "user", Map.of(
                "id", session.userId().toString(),
                "role", session.role(),
                "customerLinked", session.customerSourceRef() != null
            )
        );
    }

    @PostMapping("/password/reset-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envoi du lien de réinitialisation de mot de passe",
               description = "Réponse identique que le compte existe ou non.")
    public void resetRequest(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email != null && !email.isBlank()) {
            authService.requestPasswordReset(email);
        }
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Application du nouveau mot de passe via jeton")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mot de passe modifié, sessions révoquées"),
        @ApiResponse(responseCode = "401", description = "Jeton invalide ou expiré")
    })
    public void reset(@Valid @RequestBody PasswordResetRequest req) {
        authService.resetPassword(req.token(), req.password());
    }

    private String extractSessionToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (SESSION_COOKIE.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
