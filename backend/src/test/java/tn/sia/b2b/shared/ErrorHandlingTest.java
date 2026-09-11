package tn.sia.b2b.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tn.sia.b2b.shared.error.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void chaque_code_erreur_a_un_statut_http_associe(ErrorCode code) {
        assertThat(code.getHttpStatus()).isNotNull();
    }

    @Test
    void les_codes_catalogue_sont_tous_presents() {
        // Vérifie que les codes de la spec §10.2 existent dans l'enum
        assertThat(ErrorCode.AUTH_INVALID_CREDENTIALS).isNotNull();
        assertThat(ErrorCode.AUTH_ACCOUNT_PENDING).isNotNull();
        assertThat(ErrorCode.AUTH_ACCOUNT_SUSPENDED).isNotNull();
        assertThat(ErrorCode.CUSTOMER_NOT_LINKED).isNotNull();
        assertThat(ErrorCode.PRODUCT_NOT_FOUND).isNotNull();
        assertThat(ErrorCode.PRODUCT_INACTIVE).isNotNull();
        assertThat(ErrorCode.PRICE_UNRESOLVED).isNotNull();
        assertThat(ErrorCode.PACKAGING_VIOLATION).isNotNull();
        assertThat(ErrorCode.STOCK_INSUFFICIENT).isNotNull();
        assertThat(ErrorCode.STOCK_DATA_STALE).isNotNull();
        assertThat(ErrorCode.CART_REVALIDATION_REQUIRED).isNotNull();
        assertThat(ErrorCode.ORDER_DUPLICATE_SUBMISSION).isNotNull();
        assertThat(ErrorCode.ORDER_INVALID_TRANSITION).isNotNull();
        assertThat(ErrorCode.ORDER_NOT_CANCELLABLE).isNotNull();
        assertThat(ErrorCode.INGEST_SIGNATURE_INVALID).isNotNull();
        assertThat(ErrorCode.INGEST_SOURCE_UNKNOWN).isNotNull();
        assertThat(ErrorCode.INGEST_ENVELOPE_INVALID).isNotNull();
        assertThat(ErrorCode.INGEST_SCHEMA_UNSUPPORTED).isNotNull();
        assertThat(ErrorCode.INGEST_DUPLICATE).isNotNull();
        assertThat(ErrorCode.INGEST_PAYLOAD_TOO_LARGE).isNotNull();
        assertThat(ErrorCode.OUTBOX_ACK_UNKNOWN).isNotNull();
        assertThat(ErrorCode.RATE_LIMITED).isNotNull();
        assertThat(ErrorCode.INTERNAL_ERROR).isNotNull();
    }
}
