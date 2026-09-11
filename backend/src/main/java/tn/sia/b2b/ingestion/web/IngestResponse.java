package tn.sia.b2b.ingestion.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import tn.sia.b2b.ingestion.service.EventIngestionService.ElementResult;
import tn.sia.b2b.ingestion.service.EventIngestionService.IngestStatus;

import java.util.List;
import java.util.UUID;

public record IngestResponse(
    @JsonProperty("results") List<ElementResultView> results
) {
    public IngestResponse(List<ElementResult> raw) {
        this(raw.stream().map(ElementResultView::from).toList());
    }

    public record ElementResultView(
        @JsonProperty("eventId")   UUID eventId,
        @JsonProperty("status")    IngestStatus status,
        @JsonProperty("errorCode") String errorCode,
        @JsonProperty("error")     String error
    ) {
        static ElementResultView from(ElementResult r) {
            return new ElementResultView(r.eventId(), r.status(), r.errorCode(), r.error());
        }
    }
}
