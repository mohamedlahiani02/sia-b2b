package tn.sia.b2b.ingestion.auth;

import org.springframework.stereotype.Component;
import tn.sia.b2b.shared.error.AppException;
import tn.sia.b2b.shared.error.ErrorCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Authentifie les sources d'ingestion via HMAC-SHA256.
 *
 * Le champ secret_hash contient la clé HMAC brute (hex ou UTF-8).
 * Contrairement aux mots de passe, les clés HMAC ne sont pas hachées Argon2
 * car elles doivent être reproductibles pour la vérification.
 */
@Component
public class SourceRegistry {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int MAX_CLOCK_SKEW_SECONDS = 300;

    private final IngestSourceRepository repository;

    public SourceRegistry(IngestSourceRepository repository) {
        this.repository = repository;
    }

    /**
     * Authentifie la source et retourne l'entité si tout est valide.
     * Lève AppException en cas d'échec (source inconnue, timestamp hors fenêtre, signature invalide).
     */
    public IngestSource authenticate(String sourceCode, String timestampHeader,
                                     String signatureHeader, byte[] body) {
        IngestSource source = repository.findBySourceCodeAndActiveTrue(sourceCode)
            .orElseThrow(() -> new AppException(ErrorCode.INGEST_SOURCE_UNKNOWN,
                "Source inconnue ou inactive : " + sourceCode));

        long timestamp = parseTimestamp(timestampHeader);
        checkClockSkew(timestamp);

        if (!computeAndCompare(source.getSecretHash(), sourceCode, timestampHeader, body, signatureHeader)) {
            throw new AppException(ErrorCode.INGEST_SIGNATURE_INVALID, "Signature HMAC invalide");
        }

        return source;
    }

    private boolean computeAndCompare(String secretKey, String source,
                                      String timestamp, byte[] body, String providedSig) {
        try {
            String message = source + "\n" + timestamp + "\n" + new String(body, StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            String computed = HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
            String provided = providedSig.startsWith("sha256=") ? providedSig.substring(7) : providedSig;
            return MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8),
                                         provided.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private long parseTimestamp(String header) {
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INGEST_SIGNATURE_INVALID, "X-Sia-Timestamp invalide");
        }
    }

    private void checkClockSkew(long timestamp) {
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > MAX_CLOCK_SKEW_SECONDS) {
            throw new AppException(ErrorCode.INGEST_SIGNATURE_INVALID,
                "Timestamp hors fenêtre de 5 minutes");
        }
    }
}
