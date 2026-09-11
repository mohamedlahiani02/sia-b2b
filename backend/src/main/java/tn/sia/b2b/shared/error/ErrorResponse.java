package tn.sia.b2b.shared.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Enveloppe d'erreur uniforme retournée par tous les endpoints")
public record ErrorResponse(

        @Schema(description = "Horodatage UTC de l'erreur", example = "2026-09-11T08:14:22Z")
        Instant timestamp,

        @Schema(description = "Identifiant de trace permettant de retrouver la requête dans les journaux", example = "4bf92f3577b34da6a3ce929d0e0e4736")
        String traceId,

        @Schema(description = "Code d'erreur stable", example = "AUTH_INVALID_CREDENTIALS")
        String code,

        @Schema(description = "Message lisible", example = "Identifiants invalides")
        String message,

        @Schema(description = "Détails complémentaires, notamment liste des champs en erreur")
        List<String> details
) {

    public static ErrorResponse of(String traceId, ErrorCode code, String message, List<String> details) {
        return new ErrorResponse(Instant.now(), traceId, code.name(), message, details != null ? details : List.of());
    }

    public static ErrorResponse of(String traceId, ErrorCode code, String message) {
        return of(traceId, code, message, List.of());
    }
}
