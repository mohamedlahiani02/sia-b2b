package tn.sia.b2b.ingestion.envelope;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum SupportedEventType {
    PRODUCT_CREATED("product.created",             "sia.ingest.product.v1"),
    PRODUCT_UPDATED("product.updated",             "sia.ingest.product.v1"),
    PRODUCT_DEACTIVATED("product.deactivated",     "sia.ingest.product.v1"),
    STOCK_CHANGED("stock.changed",                 "sia.ingest.stock.v1"),
    STOCK_SNAPSHOT("stock.snapshot",               "sia.ingest.stock.v1"),
    PRICE_CHANGED("price.changed",                 "sia.ingest.price.v1"),
    CUSTOMER_CREATED("customer.created",           "sia.ingest.customer.v1"),
    CUSTOMER_UPDATED("customer.updated",           "sia.ingest.customer.v1"),
    ORDER_CONFIRMED("order.confirmed",             "sia.ingest.order-feedback.v1"),
    ORDER_PARTIALLY_CONFIRMED("order.partially_confirmed", "sia.ingest.order-feedback.v1"),
    ORDER_REJECTED("order.rejected",               "sia.ingest.order-feedback.v1");

    private final String type;
    private final String topic;

    SupportedEventType(String type, String topic) {
        this.type = type;
        this.topic = topic;
    }

    public String getType() { return type; }
    public String getTopic() { return topic; }

    public static Optional<SupportedEventType> fromType(String type) {
        return Arrays.stream(values())
            .filter(e -> e.type.equals(type))
            .findFirst();
    }

    public static final Set<Integer> SUPPORTED_SCHEMA_VERSIONS = Set.of(1);
}
