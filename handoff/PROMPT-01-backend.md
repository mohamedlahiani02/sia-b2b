# Prompt de démarrage — à coller dans Claude Code

> Collez le bloc ci-dessous tel quel comme premier message, depuis `C:\Users\haith\OneDrive\Bureau\B2B-2026` avec `CLAUDE.md` à la racine et les documents dans `docs\`.

---

Lis d'abord `CLAUDE.md`, puis `docs/Backend Spec.html` (sections 1 à 5 et 7 à 10), `docs/API Contracts.html` (sections 1 et 2) et `docs/Product Backlog.html` (sections 1, 3 et 4). Ne code rien avant d'avoir lu ces documents.

Nous construisons la plateforme B2B SIA. Ordre imposé : **backend entièrement d'abord**, jusqu'à une API documentée dans Swagger et testée ; le frontend Next.js viendra ensuite, généré contre cette spécification OpenAPI.

Contrainte non négociable : les modèles de données Produit, Stock, Tarif et Client, ainsi que le format de commande attendu par l'ERP du client, **ne sont pas connus** (points TBD-01 à TBD-06). Tu n'inventes aucun champ métier sur ces entités. Les tables de projection reçoivent uniquement leurs colonnes techniques et un `payload_raw jsonb`. Si une tâche exige ces structures, tu t'arrêtes et tu me le dis.

Pour cette première session, livre le **lot 1 (socle)** du backlog, stories E0-01 à E0-06 et E3-01 à E3-06 :

1. Projet **Maven** (`pom.xml`, parent `spring-boot-starter-parent 3.x`), Java 21, Spring Boot 3.x, packages `tn.sia.b2b` conformes à la spec §4, avec un test ArchUnit qui échoue si un contrôleur touche un repository ou si `ingestion` importe un module métier. Pas de Kotlin, ni dans le code ni dans les scripts de build.
2. `docker-compose.yml` : `postgres:16`, `kafka` en mode KRaft, `redis`, `nginx`, plus le service applicatif. Volumes nommés, variables d'environnement, aucun secret dans le dépôt.
3. Migrations Flyway créant les tables des zones « plateforme » et « intégration » de la spec §7.2 et §7.3. Les tables de projection reçoivent **uniquement** les colonnes techniques de §7.1. `ddl-auto: validate`.
4. Gestion d'erreur globale : enveloppe unique `{timestamp, traceId, code, message, details[]}`, énumération complète des codes du catalogue §10.2, `traceId` propagé dans les journaux et dans la réponse.
5. Authentification et comptes : inscription professionnelle en `PENDING`, validation par SIA avec rattachement client obligatoire, session cookie `HttpOnly` avec jeton opaque en Redis, Argon2id, rôles `ADMIN`/`OPERATOR`/`VIEWER`, `audit_log` sur toute action admin, cloisonnement strict des données par rattachement client.
6. springdoc-openapi : Swagger UI sur `/swagger-ui`, spécification sur `/v3/api-docs`. Chaque endpoint livré est documenté avec ses schémas de requête, de réponse **et d'erreur**. Les DTO inconnus sont déclarés comme objets libres avec `TBD-xx` écrit dans leur description.
7. Tests : unitaires sur le domaine, intégration sur PostgreSQL réel via Testcontainers. Chaque critère d'acceptation des stories ci-dessus est couvert.
8. CI GitHub Actions : compilation, tests, analyse statique, image Docker.

Travaille story par story. Pour chacune : annonce ce que tu vas faire, implémente, lance les tests, puis commit avec l'identifiant de la story dans le message (`feat(identity): E3-02 validation et rattachement client`). Arrête-toi après chaque story pour que je valide.

Commence par me proposer le plan de la session et la liste des fichiers que tu vas créer, sans coder, et signale tout point où les documents te paraissent ambigus.

---

## Prompt de la session suivante (lot 2 — flux événementiel)

Lis `docs/Backend Spec.html` §5 en entier et `docs/API Contracts.html` §11 et §12, puis livre le **lot 2** du backlog : stories E1-01 à E1-08 et E10-01 à E10-03.

Attendu : passerelle `POST /ingest/v1/events` signée HMAC-SHA256 avec résultat par élément, validation d'enveloppe sans validation de payload, idempotence par unicité SQL sur `event_id`, journal `event_inbox` append-only, relais `SKIP LOCKED` vers Kafka avec `acks=all`, consommateurs `@KafkaListener` avec offset manuel, contrôle d'ordre par `entityVersion`, topics de reprise et DLT, détection de trous de séquence, réception de snapshot par tranches, endpoints de supervision et rejeu.

Tests d'intégration exigés avec Kafka réel (Testcontainers) : rejeu de 10 000 événements sans effet en double, arrivée en désordre convergeant vers le même état, courtier arrêté puis redémarré sans perte ni refus de réception, message empoisonné basculant en DLT sans bloquer sa partition.

---

## Prompt de la phase frontend (après Swagger complet)

Ne démarre pas si `/v3/api-docs` ne couvre pas encore le catalogue, le panier et les commandes.

Génère d'abord le client TypeScript depuis l'OpenAPI du backend (`openapi-typescript` + `openapi-fetch`) : aucun type d'API écrit à la main, aucune URL en dur.

Puis construis le frontend Next.js (App Router, TypeScript strict, Tailwind) en **mobile-first** et **par composants**, fidèle aux maquettes de `docs/`. Lis le fichier HTML de l'écran avant de le construire, et reprends les tokens de `docs/styles/tokens.css` telles quelles, en oklch, portées en variables CSS et en thème Tailwind. La structure de composants de `docs/components/*.jsx` est celle à suivre.

Ordre : `Login` → `Home` → `Search` → fiche produit → `Cart` → `Checkout` → `OrderConfirmed` → `Account` → `Admin`. Un écran par session, avec ses états de chargement, vide, erreur et donnée périmée, et le respect des règles d'affichage listées dans `CLAUDE.md` (âge de la donnée, prix sur demande, revalidation explicite, confirmation partielle).
