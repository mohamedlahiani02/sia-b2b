-- Zone intégration : tables de réception et d'émission événementielles

-- Journal brut de tous les événements reçus du connecteur YME.
-- Append-only. Jamais modifié après RECEIVED.
CREATE TABLE event_inbox (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    event_id        UUID        NOT NULL,           -- clé idempotence (UNIQUE)
    event_type      TEXT        NOT NULL,
    schema_version  INTEGER     NOT NULL,
    source          TEXT        NOT NULL,
    sequence        BIGINT,                          -- compteur monotone par source (nullable : détection trous désactivée si absent)
    entity_ref      TEXT        NOT NULL,
    entity_version  BIGINT,
    occurred_at     TIMESTAMPTZ NOT NULL,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    payload         JSONB       NOT NULL,
    state           TEXT        NOT NULL DEFAULT 'RECEIVED', -- RECEIVED | PROCESSING | APPLIED | SUPERSEDED | RETRY | DEAD
    attempts        INTEGER     NOT NULL DEFAULT 0,
    last_error      TEXT,
    processed_at    TIMESTAMPTZ,

    CONSTRAINT event_inbox_pk PRIMARY KEY (id),
    CONSTRAINT event_inbox_event_id_uq UNIQUE (event_id)
);

CREATE INDEX event_inbox_state_received_idx ON event_inbox (state, received_at);
CREATE INDEX event_inbox_entity_version_idx ON event_inbox (entity_ref, entity_version);

-- Boîte d'émission des commandes vers le connecteur YME.
-- Remise au moins une fois : le connecteur doit être idempotent sur orderNumber.
CREATE TABLE outbox_message (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    message_type    TEXT        NOT NULL,
    order_id        UUID        NOT NULL,
    payload         JSONB       NOT NULL,
    state           TEXT        NOT NULL DEFAULT 'PENDING', -- PENDING | LEASED | ACKED | FAILED
    leased_until    TIMESTAMPTZ,
    delivery_count  INTEGER     NOT NULL DEFAULT 0,
    acked_at        TIMESTAMPTZ,

    CONSTRAINT outbox_message_pk PRIMARY KEY (id)
);

CREATE INDEX outbox_message_state_idx ON outbox_message (state, leased_until);

-- Registre des sources autorisées à pousser des événements (connecteurs YME).
CREATE TABLE ingest_source (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    source_code     TEXT        NOT NULL,
    secret_hash     TEXT        NOT NULL,            -- hash HMAC secret, rotatif
    allowed_ips     TEXT[],                          -- liste blanche IPs (NULL = toutes acceptées)
    last_sequence   BIGINT,
    last_seen_at    TIMESTAMPTZ,
    active          BOOLEAN     NOT NULL DEFAULT true,

    CONSTRAINT ingest_source_pk PRIMARY KEY (id),
    CONSTRAINT ingest_source_code_uq UNIQUE (source_code)
);

-- Agrégats de santé par fenêtre de 5 minutes — alimente les écrans de supervision.
CREATE TABLE sync_health (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    source_code         TEXT        NOT NULL,
    window_start        TIMESTAMPTZ NOT NULL,
    events_received     INTEGER     NOT NULL DEFAULT 0,
    events_applied      INTEGER     NOT NULL DEFAULT 0,
    events_dead         INTEGER     NOT NULL DEFAULT 0,
    max_lag_ms          BIGINT,

    CONSTRAINT sync_health_pk PRIMARY KEY (id),
    CONSTRAINT sync_health_source_window_uq UNIQUE (source_code, window_start)
);

-- Résultats de réconciliation quotidienne stock/projection.
CREATE TABLE reconciliation_run (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    started_at      TIMESTAMPTZ NOT NULL,
    finished_at     TIMESTAMPTZ,
    compared        INTEGER,
    mismatches      INTEGER,
    report_key      TEXT,                            -- clé objet S3/R2 du rapport détaillé

    CONSTRAINT reconciliation_run_pk PRIMARY KEY (id)
);
