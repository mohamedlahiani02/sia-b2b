package tn.sia.b2b.identity.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Demande de validation ou de refus d'un compte professionnel par SIA")
public record AccountValidationRequest(

        @NotBlank
        @Pattern(regexp = "APPROVE|REJECT")
        @Schema(description = "Action à effectuer", allowableValues = {"APPROVE", "REJECT"})
        String action,

        @Schema(description = "Référence client dans YME (obligatoire pour APPROVE). TBD-04 : vérification d'existence dans customer_projection en Lot 3.")
        String customerSourceRef,

        @Schema(description = "Motif du refus (obligatoire pour REJECT)", example = "Activité non éligible")
        String reason
) {}
