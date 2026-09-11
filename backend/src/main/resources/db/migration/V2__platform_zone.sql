-- Zone plateforme : tables propriété de la plateforme B2B SIA

-- Comptes utilisateurs (clients professionnels + opérateurs SIA).
CREATE TABLE users (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    email                   TEXT        NOT NULL,
    password_hash           TEXT        NOT NULL,           -- Argon2id
    role                    TEXT        NOT NULL DEFAULT 'VIEWER', -- ADMIN | OPERATOR | VIEWER
    status                  TEXT        NOT NULL DEFAULT 'PENDING', -- PENDING | ACTIVE | SUSPENDED | CLOSED
    company_name            TEXT        NOT NULL,
    tax_id                  TEXT        NOT NULL,
    contact_name            TEXT        NOT NULL,
    phone                   TEXT        NOT NULL,
    address                 JSONB,
    customer_source_ref     TEXT,                           -- ref client dans YME (renseignée à l'activation)
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT users_pk PRIMARY KEY (id),
    CONSTRAINT users_email_uq UNIQUE (email)
);

CREATE INDEX users_status_idx ON users (status);
CREATE INDEX users_customer_source_ref_idx ON users (customer_source_ref);

-- Jetons de réinitialisation de mot de passe (usage unique, durée 1h).
CREATE TABLE password_reset_token (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id),
    token_hash      TEXT        NOT NULL,            -- SHA-256 du jeton envoyé par email
    expires_at      TIMESTAMPTZ NOT NULL,
    used            BOOLEAN     NOT NULL DEFAULT false,

    CONSTRAINT password_reset_token_pk PRIMARY KEY (id)
);

CREATE INDEX password_reset_token_hash_idx ON password_reset_token (token_hash);

-- Panier persistant (un actif par utilisateur).
CREATE TABLE cart (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id),
    status          TEXT        NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | CHECKED_OUT | ABANDONED
    version         INTEGER     NOT NULL DEFAULT 0,         -- verrou optimiste
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT cart_pk PRIMARY KEY (id)
);

CREATE UNIQUE INDEX cart_user_active_uq ON cart (user_id) WHERE status = 'ACTIVE';

-- Lignes du panier.
CREATE TABLE cart_line (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    cart_id                 UUID        NOT NULL REFERENCES cart(id),
    product_source_ref      TEXT        NOT NULL,
    quantity                INTEGER     NOT NULL CHECK (quantity > 0),
    unit_price_resolved     NUMERIC(14,3),                  -- prix figé à l'ajout (NULL si non résolu)
    price_resolved_at       TIMESTAMPTZ,
    availability_flag       TEXT,                           -- OK | INSUFFICIENT | STALE | UNKNOWN

    CONSTRAINT cart_line_pk PRIMARY KEY (id),
    CONSTRAINT cart_line_product_uq UNIQUE (cart_id, product_source_ref)
);

-- Commandes.
CREATE TABLE orders (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    order_number        TEXT        NOT NULL,               -- séquence plateforme lisible (ex: ORD-2026-00042)
    user_id             UUID        NOT NULL REFERENCES users(id),
    customer_source_ref TEXT        NOT NULL,
    state               TEXT        NOT NULL DEFAULT 'DRAFT', -- DRAFT | SUBMITTED | VALIDATED | CONFIRMED | PARTIALLY_CONFIRMED | REJECTED | CANCELLED
    submitted_at        TIMESTAMPTZ,
    total_ht            NUMERIC(14,3),
    total_ttc           NUMERIC(14,3),
    erp_document_ref    TEXT,                               -- ref pièce YME (renseignée à l'accusé)
    idempotency_key     TEXT,
    version             INTEGER     NOT NULL DEFAULT 0,

    CONSTRAINT orders_pk PRIMARY KEY (id),
    CONSTRAINT orders_number_uq UNIQUE (order_number),
    CONSTRAINT orders_idempotency_uq UNIQUE (idempotency_key)
);

CREATE INDEX orders_user_idx ON orders (user_id, submitted_at DESC);
CREATE INDEX orders_state_idx ON orders (state);

-- Lignes de commande (snapshot au moment de la soumission).
CREATE TABLE order_line (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    order_id                UUID        NOT NULL REFERENCES orders(id),
    product_source_ref      TEXT        NOT NULL,
    label_snapshot          TEXT        NOT NULL,           -- libellé copié à la soumission
    quantity_ordered        INTEGER     NOT NULL CHECK (quantity_ordered > 0),
    quantity_confirmed      INTEGER,                        -- renseigné par YME
    unit_price              NUMERIC(14,3) NOT NULL,
    line_total              NUMERIC(14,3) NOT NULL,

    CONSTRAINT order_line_pk PRIMARY KEY (id)
);

-- Journal d'états des commandes (append-only, non modifiable).
CREATE TABLE order_state_log (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    order_id        UUID        NOT NULL REFERENCES orders(id),
    from_state      TEXT,
    to_state        TEXT        NOT NULL,
    actor           TEXT,                                   -- userId ou 'SYSTEM'
    reason          TEXT,
    event_id        UUID,                                   -- event_inbox.event_id si transition déclenchée par événement YME
    at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT order_state_log_pk PRIMARY KEY (id)
);

CREATE INDEX order_state_log_order_idx ON order_state_log (order_id, at);

-- Réservations de stock locales (n'engage pas YME, réduit la dispo affichée).
CREATE TABLE stock_reservation (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    order_id                UUID        NOT NULL REFERENCES orders(id),
    product_source_ref      TEXT        NOT NULL,
    quantity                INTEGER     NOT NULL CHECK (quantity > 0),
    state                   TEXT        NOT NULL DEFAULT 'HELD', -- HELD | CONSUMED | RELEASED | EXPIRED
    expires_at              TIMESTAMPTZ,

    CONSTRAINT stock_reservation_pk PRIMARY KEY (id)
);

-- Historique des variations de stock projeté.
CREATE TABLE stock_movement (
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    product_source_ref      TEXT        NOT NULL,
    delta                   INTEGER     NOT NULL,
    quantity_after          INTEGER     NOT NULL,
    cause                   TEXT        NOT NULL,           -- INGESTION | RESERVATION | RELEASE | RECONCILIATION
    event_id                UUID,
    at                      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT stock_movement_pk PRIMARY KEY (id)
);

CREATE INDEX stock_movement_ref_idx ON stock_movement (product_source_ref, at DESC);

-- Promotions gérées côté plateforme (hors tarification YME).
CREATE TABLE promotion (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    scope           TEXT        NOT NULL,   -- CUSTOMER | FAMILY | PRODUCT
    target_ref      TEXT        NOT NULL,
    kind            TEXT        NOT NULL,   -- PERCENTAGE | FIXED
    value           NUMERIC(14,3) NOT NULL,
    starts_at       TIMESTAMPTZ,
    ends_at         TIMESTAMPTZ,
    active          BOOLEAN     NOT NULL DEFAULT true,

    CONSTRAINT promotion_pk PRIMARY KEY (id)
);

-- Sections de la page d'accueil gérées par le back-office.
CREATE TABLE content_block (
    id          UUID    NOT NULL DEFAULT gen_random_uuid(),
    kind        TEXT    NOT NULL,
    position    INTEGER NOT NULL,
    payload     JSONB   NOT NULL,
    published   BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT content_block_pk PRIMARY KEY (id)
);

-- Métadonnées des médias (binaires stockés dans S3/R2).
CREATE TABLE media (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    object_key  TEXT        NOT NULL,
    kind        TEXT        NOT NULL,       -- PRODUCT_IMAGE | PDF | BANNER
    target_ref  TEXT,
    width       INTEGER,
    height      INTEGER,
    bytes       BIGINT,

    CONSTRAINT media_pk PRIMARY KEY (id),
    CONSTRAINT media_object_key_uq UNIQUE (object_key)
);

-- Journal d'audit de toutes les actions admin (append-only).
CREATE TABLE audit_log (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    actor_id    UUID        REFERENCES users(id),
    action      TEXT        NOT NULL,
    entity      TEXT        NOT NULL,
    entity_id   TEXT,
    diff        JSONB,                      -- {before: {...}, after: {...}}
    ip          TEXT,
    at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT audit_log_pk PRIMARY KEY (id)
);

CREATE INDEX audit_log_actor_idx ON audit_log (actor_id, at DESC);
CREATE INDEX audit_log_entity_idx ON audit_log (entity, entity_id);
