-- Décision TBD-06 actée : pas de connecteur automatique vers YME.
-- Les commandes sont saisies manuellement par les opérateurs SIA dans YME.
-- Cette migration adapte outbox_message et orders à ce nouveau modèle.

-- ─── outbox_message : file de saisie manuelle ────────────────────────────────
--
-- Anciens états (modèle connecteur, abandonné) : PENDING | LEASED | ACKED | FAILED
-- Nouveaux états (modèle saisie manuelle)      : PENDING_ENTRY | ENTERED | CONFIRMED_YME | FAILED
--
-- Les colonnes leased_until / delivery_count / acked_at sont conservées
-- pour compatibilité mais ne sont plus utilisées dans le nouveau modèle.
-- Elles seront supprimées par une migration de nettoyage après recette.

ALTER TABLE outbox_message
    ADD COLUMN entered_by      UUID        REFERENCES users(id),
    ADD COLUMN entered_at      TIMESTAMPTZ,
    ADD COLUMN erp_document_ref TEXT,                       -- ref pièce YME saisie par l'opérateur
    ADD COLUMN sla_target_at   TIMESTAMPTZ;                 -- délai de saisie attendu (order.submitted_at + ENTRY_SLA_HOURS)

COMMENT ON COLUMN outbox_message.state IS
    'PENDING_ENTRY : commande validée, en attente de saisie dans YME par un opérateur SIA.
     ENTERED : saisie effectuée, erp_document_ref renseigné, en attente de confirmation YME.
     CONFIRMED_YME : événement order.confirmed reçu de YME via event_inbox.
     FAILED : SLA dépassé ou erreur manuelle signalée — visible dans file alertes back-office.
     (Anciens états LEASED/ACKED obsolètes — modèle connecteur abandonné.)';

COMMENT ON COLUMN outbox_message.entered_by IS 'UUID opérateur SIA ayant effectué la saisie dans YME.';
COMMENT ON COLUMN outbox_message.erp_document_ref IS 'Référence du document créé dans YME lors de la saisie manuelle.';
COMMENT ON COLUMN outbox_message.sla_target_at IS 'Échéance de saisie. Dépasser cette date déclenche une alerte back-office.';

-- Index pour la file de saisie (vue opérateur : commandes PENDING_ENTRY triées par SLA)
CREATE INDEX outbox_message_manual_queue_idx
    ON outbox_message (state, sla_target_at)
    WHERE state IN ('PENDING_ENTRY', 'FAILED');

-- Index pour le suivi par opérateur
CREATE INDEX outbox_message_entered_by_idx
    ON outbox_message (entered_by, entered_at DESC)
    WHERE entered_by IS NOT NULL;


-- ─── orders : machine à états étendue ────────────────────────────────────────
--
-- Anciens états : DRAFT | SUBMITTED | VALIDATED | CONFIRMED | PARTIALLY_CONFIRMED | REJECTED | CANCELLED
-- Nouveaux états ajoutés :
--   PENDING_ENTRY  — commande validée, outbox_message créé, en attente de saisie opérateur
--   ENTERED_YME    — saisie effectuée dans YME, en attente de confirmation par événement

COMMENT ON COLUMN orders.state IS
    'Machine à états :
     DRAFT             → commande en cours de constitution (panier soumis mais non revalidé)
     SUBMITTED         → client a soumis, revalidation stock/prix en cours
     VALIDATED         → revalidation réussie, commande ferme côté plateforme
     PENDING_ENTRY     → outbox_message créé, en attente de saisie manuelle dans YME par opérateur SIA
     ENTERED_YME       → opérateur a saisi dans YME et renseigné erp_document_ref, en attente événement YME
     CONFIRMED         → order.confirmed (total) reçu de YME via event_inbox
     PARTIALLY_CONFIRMED → order.confirmed (partiel) reçu de YME
     REJECTED          → order.rejected reçu de YME
     CANCELLED         → annulation avant ENTERED_YME uniquement (après : contact SIA)';

-- Colonne pour l'horodatage de la transition ENTERED_YME (traçabilité SLA)
ALTER TABLE orders
    ADD COLUMN entered_at TIMESTAMPTZ;  -- renseigné quand state passe à ENTERED_YME

COMMENT ON COLUMN orders.entered_at IS 'Horodatage de la saisie manuelle dans YME. Permet de calculer le délai effectif vs SLA.';


-- ─── order_state_log : la machine à états est un journal non modifiable ───────
-- Aucune modification nécessaire : toutes les transitions (y compris les nouvelles)
-- y seront tracées automatiquement par OrderService.
