# SIA B2B — Contexte projet

Plateforme de commande B2B pour SIA, distributeur de pièces automobiles. Deux surfaces : un site de commande pour les clients professionnels (garages, revendeurs) et un back-office pour SIA.

**Langue du projet : français.** Code, identifiants et noms techniques en anglais ; commentaires, messages utilisateur, libellés d'interface et documentation en français.

## Documents de référence — à lire avant de coder

| Fichier | Contenu |
|---|---|
| `docs/Backend Spec.html` | Architecture, modèle événementiel, modèle de données, règles métier (RM-xx), catalogue d'erreurs, points ouverts (TBD-xx) |
| `docs/API Contracts.html` | Tous les endpoints : requêtes, réponses, codes d'erreur, conventions |
| `docs/Product Backlog.html` | 13 epics, 68 user stories avec critères d'acceptation, séquencement en 6 lots |
| `docs/*.html` (Home, Search, Cart, Checkout, Account, Login, Admin, OrderConfirmed) | Maquettes de référence |
| `docs/styles/tokens.css` | Design tokens — valeurs exactes, à ne pas réinterpréter |

Ces documents sont la source de vérité. En cas de contradiction entre une intuition et un document, le document gagne. Si un document est muet sur un point, demandez — ne comblez pas.

---

## Règle absolue : ne rien inventer des données du client

Le logiciel de gestion existant du client (ERP legacy, désigné **YME** dans les documents) est **l'unique source de vérité** pour les produits, le stock, les tarifs et les clients. La plateforme ne fait qu'en tenir une projection en lecture.

Les structures exactes de ces données **ne sont pas connues** : ce sont les points TBD-01 à TBD-06 de la spécification (§14). Elles seront fournies par le client.

**En conséquence, interdiction de :**
- inventer des champs de produit, de stock, de tarif ou de client ;
- inventer le format de commande attendu par l'ERP ;
- écrire une entité JPA `Product` ou `Stock` avec des colonnes métier devinées ;
- créer un jeu de données de démonstration présenté comme représentatif.

**À la place :** les tables de projection sont créées avec leurs seules colonnes techniques (`source_ref`, `source_version`, `last_event_id`, `projected_at`, `source_occurred_at`, `payload_raw jsonb`, `status`), le payload est conservé en `jsonb`, et les colonnes typées seront extraites par une migration ultérieure quand le client aura répondu. Si une tâche exige ces structures, **arrêtez-vous et dites-le** au lieu de produire du code à refaire.

Les mêmes règles valent pour : la devise et les décimales (TBD-09), le cumul des remises (TBD-10), le seuil de donnée périmée (TBD-07), les images produit (TBD-08), les seuils d'alerte (TBD-13).

---

## Architecture — les six principes à ne jamais violer

1. **YME est la source de vérité.** Aucune donnée maîtresse n'est créée côté plateforme. Aucun endpoint d'écriture sur les projections produit/stock (règle RM-03) — à vérifier par test d'architecture.
2. **Découplage total.** Aucun appel synchrone vers l'ERP sur le chemin d'une requête utilisateur. ERP arrêté = le site continue à servir le catalogue et à enregistrer des commandes.
3. **YME est intouchable — saisie manuelle par les opérateurs SIA.** Il n'existe aucun connecteur automatique entre la plateforme et YME. Les commandes validées apparaissent dans une file de saisie du back-office ; un opérateur SIA les saisit manuellement dans YME. YME émet ensuite ses événements habituels (stock, confirmation) vers la plateforme via le bus entrant. Rien n'entre dans le réseau du client à notre initiative.
4. **Tout événement est rejouable.** Chaque message reçu est conservé brut, identifié, horodaté. Un incident se répare par rejeu, jamais par correction manuelle en base.
5. **Le stock projeté est une information, pas un engagement.** L'engagement ferme vient de l'ERP à la confirmation.
6. **Contrat explicite et versionné.** Un schéma inconnu est rejeté et signalé, jamais interprété au mieux.

## Chaîne événementielle

```
connecteur client ─HTTP signé─► passerelle ─► event_inbox (Postgres) ─relais─► topic Kafka ─► consommateurs
                                     │                                                    ├─► projection produit
                              202 immédiat                                                ├─► projection stock
                                                                                          └─► supervision

commande validée ─► outbox_message (PENDING_ENTRY)
                         │
                    back-office admin : file de saisie (SLA configurable)
                         │
                    opérateur SIA saisit manuellement dans YME + renseigne ref doc YME
                         │
                    outbox_message (ENTERED) ◄─ horodaté, tracé avec acteur
                         │
                    YME traite normalement ─► émet order.confirmed / stock.changed
                         │
                    event_inbox ─► projection ─► commande mise à jour (CONFIRMED / REJECTED)
```

- **Kafka** (mode KRaft) porte tout le transport interne. Clé de partition = identifiant d'entité, ce qui garantit l'ordre par référence. `enable.auto.commit=false`, offset validé après traitement réussi. Topics de reprise + DLT, 5 tentatives en délai exponentiel.
- **La passerelle HTTP n'appelle pas Kafka** : elle écrit en base et répond `202`. Un relais (`SKIP LOCKED`, `acks=all`) publie ensuite. Courtier indisponible = réception toujours acceptée.
- **L'outbox est une file de saisie manuelle**, pas un endpoint HTTP pour connecteur. La table `outbox_message` expose les commandes à saisir dans YME. L'opérateur marque "saisi" via le back-office, ce qui horodate et trace l'action dans `audit_log`. TBD-06 (connecteur) : **résolu — pas de connecteur, saisie manuelle.**
- Idempotence par unicité SQL sur `event_inbox.event_id`. Ordre par comparaison de `entityVersion`. Rétention Kafka 7 j, `event_inbox` 90 j — un rejeu long republie depuis la base, jamais depuis le courtier.

## Machine à états des commandes

```
DRAFT ──► SUBMITTED ──► VALIDATED ──► PENDING_ENTRY ──► ENTERED_YME ──► CONFIRMED
                                                │                    └──► PARTIALLY_CONFIRMED
                                                │                    └──► REJECTED
                                           CANCELLED (avant ENTERED_YME seulement)
```

| État | Déclencheur | Acteur |
|---|---|---|
| `DRAFT` | Création du panier soumis | Système |
| `SUBMITTED` | Client soumet la commande | Client B2B |
| `VALIDATED` | Revalidation stock/prix réussie | Système |
| `PENDING_ENTRY` | Commande prête à saisir dans YME | Système (automatique après VALIDATED) |
| `ENTERED_YME` | Opérateur SIA a saisi dans YME + ref doc YME renseignée | Opérateur SIA (back-office) |
| `CONFIRMED` | YME émet `order.confirmed` (total) | Événement entrant |
| `PARTIALLY_CONFIRMED` | YME émet `order.confirmed` (partiel) | Événement entrant |
| `REJECTED` | YME émet `order.rejected` | Événement entrant |
| `CANCELLED` | Annulation avant `ENTERED_YME` | Client B2B ou opérateur |

Règles :
- On ne peut pas annuler une commande `ENTERED_YME` ou après — YME a déjà reçu la saisie.
- Le SLA de saisie est configurable (`ENTRY_SLA_HOURS`, défaut 24 h ouvrées). Une alerte back-office se déclenche si `outbox_message.sla_target_at` est dépassé.
- Le client B2B voit l'état `PENDING_ENTRY` comme "En cours de traitement" (pas de détail opérationnel interne).

---

## Phase 1 — Backend (à faire en premier, intégralement)

### Stack imposée
- Java 21, Spring Boot 3.x, **Maven** (`pom.xml`, parent `spring-boot-starter-parent 3.x`)
- PostgreSQL 16, Flyway (`ddl-auto: validate`, jamais autre chose)
- Apache Kafka (KRaft), Spring for Apache Kafka
- Redis (sessions, cache, pub/sub temps réel)
- S3 / Cloudflare R2 pour les médias et archives
- Docker Compose : `app`, `postgres`, `kafka`, `redis`, `nginx` — cible VPS Linux unique
- **springdoc-openapi** : Swagger UI sur `/swagger-ui`, spécification sur `/v3/api-docs`

### Couches — frontières non négociables
```
Controller      validation de forme, appel d'un service, mapping DTO. Aucun repository, aucune règle métier.
Service         @Transactional, orchestration, publication d'événements, autorisation fine
Domain          entités riches, machines à états, objets valeur (Money, Sku, Quantity), sans dépendance Spring
Persistence     JPA pour les écritures, SQL natif/JdbcTemplate pour les lectures catalogue et agrégats
Integration     passerelle, relais Kafka, consommateurs, outbox, client S3, mailer
```
Un test d'architecture (ArchUnit) doit échouer si un contrôleur touche un repository, si `ingestion` importe un module métier, ou si la colonne `state` d'une commande est écrite ailleurs que dans `OrderService`.

### Packages
```
tn.sia.b2b
├── config/ shared/{domain,error,event}
├── identity/  ingestion/{web,auth,envelope,store}
├── projection/{handler,consumer,relay,model}
└── catalog/ pricing/ cart/ ordering/ outbox/ content/ ops/
```

### Conventions
- **Identifiants** : UUID v7 pour nos entités ; les identifiants ERP sont conservés tels quels dans `source_ref`, jamais transformés.
- **Montants** : `NUMERIC(14,3)` en base, objet valeur `Money`, chaîne décimale en JSON (`"128.500"`). Jamais de `double`.
- **Dates** : `timestamptz`, `Instant`, UTC partout. Conversion Africa/Tunis à l'affichage seulement.
- **Transactions** : une par commande applicative ; les effets externes (mail, Kafka, S3) après commit.
- **Erreurs** : enveloppe unique `{timestamp, traceId, code, message, details[]}`, codes stables pris dans le catalogue de la spec §10.2. Aucune trace technique, aucun nom de classe, aucun SQL dans une réponse.
- **Idempotency-Key** obligatoire sur `POST /orders` et toute action admin non réversible.
- **Pagination** par curseur sur le catalogue et les journaux.
- **Migrations** : une migration appliquée n'est jamais modifiée.

### Swagger — exigence de couverture
Chaque endpoint publié doit apparaître dans `/v3/api-docs` avec : description, paramètres, schéma de requête, schéma de réponse **et schémas d'erreur avec leur `code`**. Annotez les DTO (`@Schema`) avec un exemple. Un endpoint sans documentation OpenAPI est considéré incomplet. Les DTO encore inconnus (produit, stock, commande ERP) sont déclarés comme objets libres et **marqués `TBD-xx` dans leur description** — visible, pas silencieux.

### Ordre de réalisation (lots du backlog)
| Lot | Contenu | Bloqué ? |
|---|---|---|
| 1 | E0 socle + E3-01…06 comptes et rôles | ✅ Livré |
| 2 | E1 ingestion + Kafka + E10-01…03 supervision | Non |
| 3 | E2 projections, E4 catalogue, E5 prix | **Oui — TBD-01 à 04, 09, 10** (réunion client) |
| 4 | E6 panier, E7 commande, E9-01…04 back-office + **file de saisie manuelle** | Non |
| 5 | E8 retours ERP (order.confirmed/rejected via events entrants) | **Partiellement — TBD-05** · TBD-06 résolu |
| 6 | E10-04…06 réconciliation, E11 contenu, E12 recette | Partiellement |

Une story = un commit, avec son identifiant dans le message : `feat(ingestion): E1-03 idempotence par eventId`.

### Definition of done
Critères d'acceptation de la story couverts par des tests automatisés ; tests d'intégration sur PostgreSQL et Kafka réels (Testcontainers) pour tout ce qui touche base ou bus ; codes d'erreur conformes au catalogue ; métriques et journalisation en place ; OpenAPI à jour ; migration Flyway livrée avec le code.

---

## Phase 2 — Frontend (seulement après Swagger complet)

### Stack imposée
- Next.js (App Router), TypeScript strict, React Server Components là où c'est pertinent
- Tailwind CSS, tokens issus de `docs/styles/tokens.css`
- TanStack Query pour les données côté client, Server Actions pour les mutations simples
- **Client API généré depuis l'OpenAPI du backend** (`openapi-typescript` / `openapi-fetch`) — aucun type d'API écrit à la main, aucune URL en dur

### Exigences de construction
- **Mobile-first.** Écrire la version mobile d'abord, puis élargir aux points de rupture. Le back-office admin est l'exception : desktop-first assumé, mais utilisable en tablette.
- **Composants, pas des pages HTML.** Chaque élément récurrent des maquettes devient un composant réutilisable et typé. Zéro duplication de markup entre deux écrans.
- **Dynamique.** Aucune donnée codée en dur : tout vient de l'API. Les états de chargement, vide, erreur et « donnée périmée » sont des états de premier ordre, pas des oublis.
- **Fidélité à la maquette.** Les fichiers `docs/*.html` sont la référence visuelle : lisez-les et reproduisez mise en page, échelles typographiques, espacements, rayons, ombres, densité et comportements de survol. Reproduisez, ne réinterprétez pas. Ne remplacez pas la palette par des couleurs Tailwind par défaut.
- **Accessibilité** : cibles tactiles ≥ 44 px, contraste du texte ≥ 4.5:1, focus visible, navigation clavier dans le back-office.

### Design tokens — valeurs exactes de `tokens.css`
```
Marque      --sia-red #C8102E → oklch(0.56 0.19 25) ; hover oklch(0.50 0.19 25)
            soft oklch(0.95 0.04 25) ; border oklch(0.85 0.08 25)
Neutres     bg oklch(0.985 0.003 60) · surface #fff · surface-2 oklch(0.975 0.004 60)
  (chauds)  surface-3 oklch(0.955 0.005 60) · border oklch(0.90 0.005 60)
            border-strong oklch(0.82 0.006 60)
Encre       ink oklch(0.20 0.01 250) · ink-2 oklch(0.38 …) · ink-3 oklch(0.55 …) · ink-4 oklch(0.70 …)
Statuts     ok oklch(0.62 0.13 155) · warn oklch(0.72 0.14 75)
            danger oklch(0.58 0.19 25) · info oklch(0.55 0.13 240) (+ variantes soft)
Typo        Inter (400/500/600/700/800) ; mono JetBrains Mono ; base 14px, line-height 1.5
Rayons      sm 6px · base 10px · lg 14px · xl 20px
Layout      max 1440px · header 68px · barre véhicule 56px
Ombres      sm / base / lg — reprendre les valeurs du fichier telles quelles
```
Portez ces tokens en variables CSS **et** en thème Tailwind. Les valeurs sont en oklch : conservez-les, ne les convertissez pas en hex.

### Écrans à reconstruire
| Maquette | Route | Rôle |
|---|---|---|
| `Login.html` | `/connexion` | Connexion + demande d'ouverture de compte |
| `Home.html` | `/` | Accueil, sections pilotées par le back-office |
| `Search.html` | `/recherche` | Recherche et liste filtrée, prix et disponibilité par ligne |
| (fiche produit) | `/produit/[ref]` | Fiche par référence ERP — construire d'après le vocabulaire de `Search.html` et `components/Product.jsx` |
| `Cart.html` | `/panier` | Panier, écarts signalés par ligne |
| `Checkout.html` | `/commande` | Revalidation puis soumission |
| `OrderConfirmed.html` | `/commande/[numero]` | Confirmation et suivi |
| `Account.html` | `/compte` | Profil, adresses, historique |
| `Admin.html` | `/admin/**` | Back-office : file de commandes, clients, supervision des flux |

Lisez chaque fichier avant de construire l'écran correspondant. `components/Chrome.jsx`, `Icon.jsx`, `Product.jsx`, `Admin.jsx` donnent la structure de composants déjà pensée dans la maquette : suivez-la.

### Règles fonctionnelles côté interface
- Aucun prix affiché sans session authentifiée et rattachement client (RM-02). Sans rattachement : navigation possible, prix et commande refusés.
- Toute donnée issue d'une projection est affichée **avec son âge** (`asOf`, `freshness`). Au-delà du seuil de retard, un avertissement explicite remplace la quantité (RM-11).
- Prix non résolu → « prix sur demande », ajout au panier désactivé (RM-04).
- La revalidation du panier est une étape visible : les écarts (prix changé, stock réduit, produit retiré) sont présentés et doivent être acceptés explicitement avant soumission (RM-07). Jamais d'ajustement automatique et silencieux.
- Une confirmation partielle est un état normal, affiché ligne par ligne (RM-10).
- La chronologie d'une commande affichée au client est celle du journal d'états : le client voit la même vérité que le support.

---

## Ce qui n'est pas dans le périmètre
Toute modification de l'ERP · **le connecteur côté client (TBD-06 : résolu — pas de connecteur, saisie manuelle par les opérateurs SIA)** · comptabilité et facturation légale · paiement en ligne (TBD-11).

## Commandes
```bash
# Backend
mvn spring-boot:run                        # démarrage dev
mvn test                                   # tests unitaires + intégration (Testcontainers)
mvn package -DskipTests                    # build JAR

# Infrastructure
docker compose up -d                       # démarrer postgres, kafka, redis, nginx
docker compose down -v                     # arrêter et supprimer volumes

# Frontend (phase 2)
npm run dev                                # démarrage Next.js
npx openapi-typescript /v3/api-docs -o src/api/schema.d.ts   # génération client TypeScript
```

## Décisions techniques actées
- **Build** : Maven, pas Gradle. Aucun Kotlin nulle part (ni code ni scripts build).
- **Répertoire projet** : `C:\Users\haith\OneDrive\Bureau\B2B-2026\` — sous-dossiers `backend\` et `frontend\`.
- **Notification back-office** (E3-01) : `audit_log` + email à `SIA_ADMIN_EMAIL` (variable d'environnement).
- **E3-02 placeholder** : validation `customerSourceRef` accepte toute ref non vide en Lot 1 ; contrainte réelle en Lot 3 après accord client sur TBD-04.
- **Lots 3 et 5** : démarrage conditionnel à la réunion client (TBD-01 à TBD-06).
- **TBD-06 résolu — pas de connecteur, saisie manuelle :** Le client ne fait pas confiance à une intégration automatique avec YME et considère son legacy comme intouchable. Les commandes validées apparaissent dans la file de saisie du back-office (`outbox_message`). Un opérateur SIA les saisit manuellement dans YME et enregistre la ref doc YME dans l'application. YME émet ensuite ses événements habituels (`order.confirmed`, `stock.changed`) qui reviennent via le bus d'ingestion existant. Aucun endpoint `/ingest/v1/outbox` exposé. SLA de saisie configurable via `ENTRY_SLA_HOURS` (défaut 24 h ouvrées). Machine à états commande : `VALIDATED → PENDING_ENTRY → ENTERED_YME → CONFIRMED/REJECTED` (voir section dédiée ci-dessus).
