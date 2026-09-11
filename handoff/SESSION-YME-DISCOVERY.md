# Session de découverte YME — à injecter comme premier message

> Ce fichier est conçu pour être collé tel quel dans une nouvelle session Claude Code
> ouverte sur l'ordinateur contenant le serveur local YME de SIA.
> Objectif : collecter toutes les informations bloquantes pour la plateforme B2B SIA.

---

## Contexte du projet

On construit une plateforme de commande B2B pour SIA, distributeur de pièces automobiles.
Le backend est en Java 21 / Spring Boot 3 / PostgreSQL / Kafka.
Le logiciel de gestion existant s'appelle **YME** (ERP legacy du client).

La plateforme ne modifie jamais YME. Elle en tient une projection en lecture.
Les commandes B2B sont saisies manuellement dans YME par les opérateurs SIA.
YME émet des événements (stock, confirmations) que la plateforme consomme.

**Ta mission dans cette session : explorer YME et documenter tout ce qui est listé ci-dessous.**
Ne génère pas de code de production. Génère uniquement un rapport structuré.

---

## Ce qu'on a besoin de savoir — liste exhaustive

### 1. Identification du client (TBD-04)

- Quel est l'identifiant unique d'un client/compte dans YME ?
  (numéro de compte, code tiers, matricule fiscal, autre ?)
- Est-ce un entier, une chaîne, un code alphanumérique ? Donne un exemple réel anonymisé.
- ~~Y a-t-il une notion de "tarif client" ou "catégorie tarifaire" rattachée au compte ?~~ **✅ NON — confirmé.**
- ~~Ce code est-il stable dans le temps ou peut-il changer ?~~ **✅ STABLE — confirmé.**
- Quels autres champs identifient un client ? (raison sociale, adresse, encours)

### 2. Catalogue produit (TBD-01)

- Quel est l'identifiant unique d'un article/produit dans YME ?
  (référence, code article, EAN, autre ?)
- Quels champs décrivent un produit ? (libellé, famille, sous-famille, marque, unité)
- ~~Y a-t-il une notion de "conditionnement" (quantité minimale de commande, multiple) ?~~ **✅ NON — confirmé.**
- ~~Y a-t-il des images produit dans YME ?~~ **✅ NON — confirmé. Placeholder générique côté frontend.**
- Un produit peut-il être désactivé/retiré du catalogue ? Comment YME le signale-t-il ?

### 3. Stock (TBD-02)

- Comment YME représente-t-il le stock ? (quantité disponible, quantité réservée, stock théorique)
- Y a-t-il plusieurs dépôts/entrepôts ? Si oui, le stock est-il par dépôt ou global ?
- Quelle est l'unité de stock (pièces, kg, litres) ? Est-elle toujours entière ?
- Comment YME notifie-t-il une variation de stock ?
  (événement, batch nocturne, fichier, API, autre ?)
- À quelle fréquence le stock change-t-il en pratique ? (temps réel, toutes les X minutes, journalier)
- Y a-t-il une notion de "stock minimum d'alerte" dans YME ?

### 4. Tarification (TBD-03 + TBD-09 + TBD-10)

- Comment YME stocke-t-il le prix d'un article ?
  (prix catalogue unique par article, ou grille par famille ?)
- ~~Y a-t-il une catégorie tarifaire rattachée au compte client ?~~ **✅ NON — confirmé. Prix porté par l'article.**
- ~~Y a-t-il plusieurs niveaux de remise qui se cumulent ?~~ **✅ SIMPLIFIÉ — pas de multi-niveaux côté client. Confirmer si remise par article/famille existe.**
- La devise est-elle toujours le dinar tunisien (TND) ? Le prix a-t-il toujours 3 décimales ?
- Les prix TTC existent-ils dans YME ou tout est HT ?
- Comment YME notifie-t-il un changement de prix ?

### 5. Commandes — format et cycle de vie (TBD-05)

- Quel est le format exact d'une commande dans YME ?
  (en-tête + lignes ? quels champs obligatoires ?)
- Quels champs identifient une ligne de commande ? (référence article, quantité, prix unitaire, remise)
- Après saisie manuelle d'une commande dans YME :
  - YME génère-t-il une référence de pièce (n° de commande YME) ? Quel format ?
  - YME confirme-t-il les quantités immédiatement ou après traitement ?
  - YME peut-il confirmer partiellement (une ligne oui, une ligne non) ?
  - YME peut-il rejeter une commande entière ? Avec quel motif ?
- Quels sont les statuts possibles d'une commande dans YME ?
- YME émet-il des événements quand le statut d'une commande change ?
  (commande accusée, confirmée, expédiée, livrée — quel canal : fichier, API, bus, autre ?)

### 6. Capacités techniques de YME (crucial — priorité absolue)

> **Contexte important :** YME est confirmé comme un logiciel **desktop Windows legacy des années 90**.
> Il n'a donc probablement pas d'API REST ni de bus d'événements natif.
> L'objectif ici est de trouver comment lire ses données depuis l'extérieur.

#### 6.1 Base de données (question n°1)

YME stocke ses données dans une base. Il faut identifier laquelle :

- Cherche dans le dossier d'installation de YME (souvent `C:\YME\`, `C:\Program Files\YME\`, ou sur un lecteur réseau partagé) :
  - Des fichiers `.mdb` ou `.accdb` → **Microsoft Access**
  - Des fichiers `.db` ou `.gdb` → **InterBase / Firebird**
  - Des fichiers `.fdb` → **Firebird**
  - Des fichiers `.mdf` + `.ldf` → **SQL Server**
  - Des fichiers `.dbf` → **dBase / FoxPro / Clipper**
  - Des fichiers `.db4`, `.cdx` → **dBase IV**
  - Un service Windows nommé `MSSQL*`, `Firebird*`, `MySQL*` → base serveur locale

- Ouvre le **Gestionnaire de tâches** → onglet Services : y a-t-il un service de base de données actif ?
- Cherche dans les fichiers de config YME (`.ini`, `.cfg`, `.xml`) une chaîne de connexion.

Si tu trouves le type de base, tente de te connecter **en lecture seule** :
- SQL Server : ouvre **SQL Server Management Studio** (SSMS) → liste toutes les tables
- Access : ouvre le fichier `.mdb` avec Access ou DBeaver
- Firebird : utilise **FlameRobin** ou **DBeaver** avec le driver Firebird
- dBase/DBF : ouvre avec **DBFView** ou DBeaver

**Liste toutes les tables trouvées. Pour chaque table dont le nom évoque un produit, stock, client, commande, tarif : copie la structure (colonnes + types) et 3-5 lignes d'exemple anonymisées.**

#### 6.2 Fichiers d'export (si pas de base accessible)

- YME génère-t-il des fichiers automatiquement ? (CSV, TXT, XML, EDI)
  - Cherche dans `C:\`, `D:\`, les lecteurs réseau partagés des fichiers avec des noms comme `stock*.csv`, `articles*.txt`, `export*.xml`
  - Y a-t-il une option "Exporter" dans le menu de YME ?
  - Y a-t-il un dossier partagé sur le réseau local que YME alimente ?
- Si oui : quel format ? Quelle fréquence de génération ? Quel chemin ?

#### 6.3 Informations sur le logiciel

- Quel nom exact et quelle version affiche l'écran "À propos" de YME ?
- Y a-t-il un dossier de documentation ou un manuel dans le dossier d'installation ?
- Sur quel poste tourne YME : poste unique ou serveur central avec clients légers ?
- Y a-t-il un administrateur ou un prestataire qui maintient YME ? (peut avoir la doc technique)

### 7. Volume et performance

- Combien d'articles/références actifs dans YME ? (ordre de grandeur)
- Combien de clients professionnels actifs ?
- Combien de commandes par jour en moyenne ?
- Quelle est la fréquence de mise à jour du stock en pratique ?

---

## Ce que tu dois produire

Un rapport Markdown structuré avec les sections suivantes :

```markdown
# Rapport de découverte YME — [date]

## 1. Identification client
[réponses + exemples anonymisés]

## 2. Catalogue produit
[réponses + exemple de fiche article anonymisée]

## 3. Stock
[réponses + exemple de représentation stock]

## 4. Tarification
[réponses + exemple de calcul de prix pour un client]

## 5. Cycle de vie commande
[réponses + exemple de commande anonymisée + liste des statuts]

## 6. Capacités techniques
[réponses + liste exhaustive des APIs / endpoints / fichiers disponibles]

## 7. Volume
[chiffres]

## 8. Points encore flous
[ce que tu n'as pas pu déterminer et pourquoi]

## 9. Recommandations d'intégration
[canal recommandé pour chaque flux :
 - Envoi produits/stock vers plateforme → ?
 - Retour confirmations commandes → ?
 - Identifiant client → format exact pour customerSourceRef]
```

---

## Informations sur la plateforme B2B (pour contexte)

**Ce qu'on envoie vers YME (les opérateurs saisissent manuellement) :**
```
Commande :
  - Numéro de commande plateforme (ex: ORD-2026-00042)
  - Référence client YME (customerSourceRef)
  - Lignes : référence article YME + quantité + prix unitaire
```

**Ce qu'on reçoit de YME (via événements, API, fichier — à déterminer) :**
```
Produits    : créations, modifications, désactivations
Stock       : variations de quantité disponible
Prix        : changements de tarif par client ou par article
Commandes   : accusé de réception + confirmation + statut logistique
```

**Format de nos événements entrants (ce que la plateforme sait consommer) :**
```json
{
  "eventId":      "uuid-v4",
  "eventType":    "stock.changed",
  "schemaVersion": 1,
  "source":       "yme-prod",
  "occurredAt":   "2026-09-12T08:00:00Z",
  "entityRef":    "REF-ARTICLE-YME",
  "entityVersion": 42,
  "payload":      { ... }    ← c'est ce payload qu'on cherche à documenter
}
```

**Tables de projection actuelles (colonnes techniques seules — à remplir après cette session) :**
- `product_projection` — TBD-01
- `stock_projection` — TBD-02
- `price_projection` — TBD-03
- `customer_projection` — TBD-04

---

## Si YME a une base de données accessible

Connecte-toi en lecture seule et liste :
1. Toutes les tables
2. Pour chaque table liée aux produits, stock, tarifs, clients, commandes :
   - Nom des colonnes + type
   - Quelques lignes d'exemple (anonymisées)
3. Les vues ou procédures stockées existantes liées à ces objets

**Ne modifie rien. Lecture seule uniquement.**

---

## Si YME a une API

Pour chaque endpoint découvert :
1. Méthode HTTP + URL
2. Paramètres / corps de requête
3. Exemple de réponse (anonymisé)
4. Code d'authentification nécessaire

---

## Priorité de découverte

En cas de temps limité, traiter dans cet ordre :

1. 🔴 **Capacités techniques** (§6) — débloque tout le reste
2. 🔴 **Identification client** (§1) — débloque E3-02 en production
3. 🔴 **Stock** (§2) — débloque Lot 3
4. 🟡 **Produit** (§2) — débloque Lot 3
5. 🟡 **Tarification** (§4) — débloque Lot 3
6. 🟡 **Cycle de vie commande** (§5) — débloque Lot 5
7. 🟢 **Volume** (§7) — utile pour le dimensionnement
