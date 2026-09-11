package tn.sia.b2b.identity.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Demande d'ouverture de session")
public record LoginRequest(

        @NotBlank @Email
        @Schema(description = "Email du compte", example = "karim@garagecentral.tn")
        String email,

        @NotBlank
        @Schema(description = "Mot de passe")
        String password
) {}
