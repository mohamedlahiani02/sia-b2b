package tn.sia.b2b.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Authentification
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_PENDING(HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN),

    // Client / Rattachement
    CUSTOMER_NOT_LINKED(HttpStatus.CONFLICT),
    CUSTOMER_REF_UNKNOWN(HttpStatus.CONFLICT),

    // Produit
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_INACTIVE(HttpStatus.CONFLICT),

    // Prix
    PRICE_UNRESOLVED(HttpStatus.CONFLICT),

    // Conditionnement
    PACKAGING_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY),

    // Stock
    STOCK_INSUFFICIENT(HttpStatus.CONFLICT),
    STOCK_DATA_STALE(HttpStatus.CONFLICT),

    // Panier
    CART_REVALIDATION_REQUIRED(HttpStatus.CONFLICT),

    // Commande
    ORDER_DUPLICATE_SUBMISSION(HttpStatus.OK),
    ORDER_INVALID_TRANSITION(HttpStatus.CONFLICT),
    ORDER_NOT_CANCELLABLE(HttpStatus.CONFLICT),

    // Ingestion
    INGEST_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED),
    INGEST_SOURCE_UNKNOWN(HttpStatus.FORBIDDEN),
    INGEST_ENVELOPE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY),
    INGEST_SCHEMA_UNSUPPORTED(HttpStatus.UNPROCESSABLE_ENTITY),
    INGEST_DUPLICATE(HttpStatus.ACCEPTED),
    INGEST_PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),

    // Outbox
    OUTBOX_ACK_UNKNOWN(HttpStatus.NOT_FOUND),

    // Général
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
