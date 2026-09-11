#!/bin/bash
# Création déclarative des topics Kafka au premier démarrage.
# 6 partitions par topic d'ingestion (ordre garanti par entité via clé de partition).
# Rétention 7 jours (604800000 ms) ; au-delà, rejeu depuis event_inbox (90 j).

KAFKA="kafka:9092"

create_topic() {
  local name=$1
  local partitions=${2:-6}
  local retention=${3:-604800000}

  kafka-topics \
    --bootstrap-server "$KAFKA" \
    --create \
    --if-not-exists \
    --topic "$name" \
    --partitions "$partitions" \
    --replication-factor 1 \
    --config retention.ms="$retention" \
    --config min.insync.replicas=1
  echo "Topic prêt : $name"
}

echo "=== Création des topics SIA B2B ==="

# Topics d'ingestion YME → plateforme
create_topic "sia.ingest.product.v1"
create_topic "sia.ingest.stock.v1"
create_topic "sia.ingest.price.v1"
create_topic "sia.ingest.customer.v1"
create_topic "sia.ingest.order-feedback.v1"

# Topics internes plateforme
create_topic "sia.internal.order.v1" 6
create_topic "sia.internal.projection.v1" 6

# Topics de reprise (retry + DLT) — 1 partition suffit pour la supervision
create_topic "sia.ingest.product.v1.retry" 1
create_topic "sia.ingest.stock.v1.retry" 1
create_topic "sia.ingest.price.v1.retry" 1
create_topic "sia.ingest.customer.v1.retry" 1
create_topic "sia.ingest.order-feedback.v1.retry" 1

create_topic "sia.ingest.product.v1.dlt" 1 2592000000    # 30 jours pour DLT
create_topic "sia.ingest.stock.v1.dlt" 1 2592000000
create_topic "sia.ingest.price.v1.dlt" 1 2592000000
create_topic "sia.ingest.customer.v1.dlt" 1 2592000000
create_topic "sia.ingest.order-feedback.v1.dlt" 1 2592000000

echo "=== Topics créés ==="
kafka-topics --bootstrap-server "$KAFKA" --list
