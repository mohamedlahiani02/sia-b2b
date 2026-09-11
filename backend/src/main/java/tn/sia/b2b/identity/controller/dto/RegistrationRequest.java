package tn.sia.b2b.identity.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Demande d'ouverture de compte professionnel")
public record RegistrationRequest(

        @NotBlank
        @Schema(description = "Raison sociale de l'entreprise", example = "Garage Central Tunis")
        String companyName,

        @NotBlank
        @Schema(description = "Identifiant fiscal", example = "1234567/A/M/000")
        String taxId,

        @NotBlank
        @Schema(description = "Nom du contact principal", example = "Karim Ben Ali")
        String contactName,

        @NotBlank
        @Email
        @Schema(description = "Email professionnel", example = "karim@garagecentral.tn")
        String email,

        @NotBlank
        @Schema(description = "Numéro de téléphone", example = "+216 71 234 567")
        String phone,

        @NotBlank
        @Size(min = 10, message = "Le mot de passe doit comporter au moins 10 caractères")
        @Schema(description = "Mot de passe (10 caractères minimum)")
        String password,

        @Schema(description = "Adresse postale de l'entreprise")
        Map<String, Object> address
) {}
