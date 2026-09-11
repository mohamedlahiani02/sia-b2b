package tn.sia.b2b.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tn.sia.b2b.ingestion.auth.IngestSourceRepository;

@Component
public class SequenceGapDetector {

    private static final Logger log = LoggerFactory.getLogger(SequenceGapDetector.class);

    private final IngestSourceRepository sourceRepository;

    public SequenceGapDetector(IngestSourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    /**
     * Détecte un trou de séquence pour une source donnée.
     * Non bloquant : l'événement est accepté même si un trou est détecté.
     * Le trou est loggué et comptabilisé dans sync_health pour alerter les opérateurs.
     *
     * @return true si un trou a été détecté
     */
    public boolean checkAndLog(String sourceCode, Long lastKnownSequence, Long incomingSequence) {
        if (incomingSequence == null || lastKnownSequence == null) {
            return false;
        }

        long expected = lastKnownSequence + 1;
        if (incomingSequence > expected) {
            long gap = incomingSequence - expected;
            log.warn("[SEQUENCE-GAP] source={} lastKnown={} incoming={} gap={}",
                sourceCode, lastKnownSequence, incomingSequence, gap);
            return true;
        }

        if (incomingSequence < lastKnownSequence) {
            log.debug("[SEQUENCE-REORDER] source={} lastKnown={} incoming={} (retransmission probable)",
                sourceCode, lastKnownSequence, incomingSequence);
        }

        return false;
    }
}
