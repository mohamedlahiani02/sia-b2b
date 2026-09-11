package tn.sia.b2b.ingestion.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.sia.b2b.ingestion.auth.IngestSource;
import tn.sia.b2b.ingestion.auth.SourceRegistry;
import tn.sia.b2b.ingestion.envelope.EventEnvelope;
import tn.sia.b2b.ingestion.service.EventIngestionService;
import tn.sia.b2b.ingestion.service.EventIngestionService.ElementResult;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/ingest/v1")
public class IngestionController {

    private static final int MAX_BODY_BYTES = 10 * 1024 * 1024; // 10 MB (aussi limité par nginx)
    private static final int MAX_BATCH_SIZE = 500;

    private final SourceRegistry sourceRegistry;
    private final EventIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    public IngestionController(SourceRegistry sourceRegistry,
                               EventIngestionService ingestionService,
                               ObjectMapper objectMapper) {
        this.sourceRegistry = sourceRegistry;
        this.ingestionService = ingestionService;
        this.objectMapper = objectMapper;
    }

    /**
     * E1-01 — Passerelle d'ingestion signée HMAC-SHA256.
     *
     * Headers requis :
     *   X-Sia-Source    : code de la source (ex: "yme-prod")
     *   X-Sia-Timestamp : epoch unix en secondes
     *   X-Sia-Signature : sha256={hex} — HMAC du corps signé avec la clé secrète
     *
     * Corps : tableau JSON d'EventEnvelope, max 500 éléments, max 10 Mo.
     *
     * Réponse 207 Multi-Status : résultat par élément (ACCEPTED / DUPLICATE / REJECTED).
     */
    @PostMapping(value = "/events",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngestResponse> ingest(
        @RequestHeader("X-Sia-Source") String source,
        @RequestHeader("X-Sia-Timestamp") String timestamp,
        @RequestHeader("X-Sia-Signature") String signature,
        @RequestBody String rawBody
    ) {
        // Taille maximale
        if (rawBody.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_BYTES) {
            throw new AppException(ErrorCode.INGEST_PAYLOAD_TOO_LARGE, "Corps > 10 Mo");
        }

        // E1-02 — Authentification HMAC
        byte[] bodyBytes = rawBody.getBytes(StandardCharsets.UTF_8);
        IngestSource ingestSource = sourceRegistry.authenticate(source, timestamp, signature, bodyBytes);

        // Déserialisation du tableau d'enveloppes
        List<EventEnvelope> envelopes;
        try {
            envelopes = objectMapper.readValue(rawBody, new TypeReference<>() {});
        } catch (Exception e) {
            throw new AppException(ErrorCode.INGEST_ENVELOPE_INVALID, "JSON invalide : " + e.getMessage());
        }

        if (envelopes.size() > MAX_BATCH_SIZE) {
            throw new AppException(ErrorCode.INGEST_PAYLOAD_TOO_LARGE,
                "Batch limité à " + MAX_BATCH_SIZE + " éléments");
        }

        // E1-03 / E1-04 — Ingestion élément par élément (résultat indépendant)
        List<ElementResult> results = new ArrayList<>(envelopes.size());
        for (EventEnvelope envelope : envelopes) {
            ElementResult result;
            try {
                result = ingestionService.ingest(envelope, ingestSource);
            } catch (AppException e) {
                result = ElementResult.rejected(
                    envelope.eventId(),
                    e.getCode(),
                    e.getMessage()
                );
            } catch (Exception e) {
                result = ElementResult.rejected(
                    envelope.eventId(),
                    ErrorCode.INTERNAL_ERROR,
                    "Erreur inattendue"
                );
            }
            results.add(result);
        }

        return ResponseEntity.status(207).body(IngestResponse.from(results));
    }
}
