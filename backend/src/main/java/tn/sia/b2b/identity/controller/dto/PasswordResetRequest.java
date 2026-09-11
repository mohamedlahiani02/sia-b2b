package tn.sia.b2b.identity.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Application du nouveau mot de passe via jeton de réinitialisation")
public record PasswordResetRequest(

        @NotBlank
        @Schema(description = "Jeton reçu par email")
        String token,

        @NotBlank
        @Size(min = 10, message = "Le mot de passe doit comporter au moins 10 caractères")
        @Schema(description = "Nouveau mot de passe (10 caractères minimum)")
        String password
) {}
