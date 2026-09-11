-- Zone projection : tables de reflet en lecture des données YME.
--
-- IMPORTANT : Ces tables ne contiennent QUE les colonnes techniques.
-- Les colonnes métier (champs produit, stock, tarif, client) seront ajoutées
-- par une migration ultérieure, après accord du client sur les structures YME.
--
--   product_projection  : TBD-01 — structure payload produit à définir avec le client
--   stock_projection    : TBD-02 — structure payload stock à définir avec le client
--   price_projection    : TBD-03 — structure payload tarif à définir avec le client
--   customer_projection : TBD-04 — structure payload client à définir avec le client

CREATE TABLE product_projection (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    source_ref          TEXT        NOT NULL,       -- identifiant naturel dans YME (SKU, code article)
    source_version      BIGINT      NOT NULL DEFAULT 0,
    last_event_id       UUID,                       -- event_inbox.event_id ayant produit l'état courant
    projected_at        TIMESTAMPTZ,
    source_occurred_at  TIMESTAMPTZ,
    payload_raw         JSONB,                      -- payload d'origine conservé intégralement (TBD-01)
    status              TEXT        NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | INACTIVE | ORPHANED

    CONSTRAINT product_projection_pk PRIMARY KEY (id),
    CONSTRAINT product_projection_source_ref_uq UNIQUE (source_ref)
);

CREATE TABLE stock_projection (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    source_ref          TEXT        NOT NULL,       -- identifiant naturel dans YME (SKU + dépôt si multi-dépôt)
    source_version      BIGINT      NOT NULL DEFAULT 0,
    last_event_id       UUID,
    projected_at        TIMESTAMPTZ,
    source_occurred_at  TIMESTAMPTZ,
    payload_raw         JSONB,                      -- payload d'origine conservé intégralement (TBD-02)
    status              TEXT        NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT stock_projection_pk PRIMARY KEY (id),
    CONSTRAINT stock_projection_source_ref_uq UNIQUE (source_ref)
);

CREATE TABLE price_projection (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    source_ref          TEXT        NOT NULL,       -- identifiant naturel dans YME (ref tarif + client)
    source_version      BIGINT      NOT NULL DEFAULT 0,
    last_event_id       UUID,
    projected_at        TIMESTAMPTZ,
    source_occurred_at  TIMESTAMPTZ,
    payload_raw         JSONB,                      -- payload d'origine conservé intégralement (TBD-03)
    status              TEXT        NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT price_projection_pk PRIMARY KEY (id),
    CONSTRAINT price_projection_source_ref_uq UNIQUE (source_ref)
);

CREATE TABLE customer_projection (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    source_ref          TEXT        NOT NULL,       -- identifiant client dans YME
    source_version      BIGINT      NOT NULL DEFAULT 0,
    last_event_id       UUID,
    projected_at        TIMESTAMPTZ,
    source_occurred_at  TIMESTAMPTZ,
    payload_raw         JSONB,                      -- payload d'origine conservé intégralement (TBD-04)
    status              TEXT        NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT customer_projection_pk PRIMARY KEY (id),
    CONSTRAINT customer_projection_source_ref_uq UNIQUE (source_ref)
);
