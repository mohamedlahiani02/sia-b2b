# SIA B2B — Passage en développement avec Claude Code

Ce dossier contient tout ce qu'il faut pour démarrer l'implémentation réelle : la spécification backend, les contrats API, le backlog, et les maquettes HTML.

**Les fichiers HTML de `design/` sont des références visuelles, pas du code de production.** Ils montrent l'intention (mise en page, couleurs, typographie, comportements). Le travail consiste à les **reconstruire** en Next.js selon les patterns du projet, pas à les copier.

---

## 1. Installation sur Windows (PowerShell)

```powershell
# a. Node.js 20+ (si absent)
winget install OpenJS.NodeJS.LTS
# fermer puis réouvrir PowerShell
node -v   # doit afficher v20.x ou plus

# b. Claude Code
npm install -g @anthropic-ai/claude-code
claude --version

# c. Git (si absent)
winget install Git.Git
```

Si `claude` refuse de démarrer sous PowerShell natif, passez par WSL :

```powershell
wsl --install -d Ubuntu
# puis, dans Ubuntu :
#   sudo apt update && sudo apt install -y nodejs npm
#   npm install -g @anthropic-ai/claude-code
```

## 2. Création du dépôt

Le dossier de travail est déjà en place :

```
C:\Users\haith\OneDrive\Bureau\B2B-2026\
```

```powershell
cd "C:\Users\haith\OneDrive\Bureau\B2B-2026"
git init

# Copier les documents et maquettes dans docs\
mkdir docs
Copy-Item -Recurse ".\handoff\design\*" .\docs\

# Copier le contexte projet à la racine
Copy-Item ".\handoff\CLAUDE.md" .\CLAUDE.md

git add -A
git commit -m "docs: spécification, contrats API, backlog, maquettes"
```

Arborescence cible après ces étapes :

```
C:\Users\haith\OneDrive\Bureau\B2B-2026\
├── CLAUDE.md              ← contexte permanent, lu automatiquement
├── docs\
│   ├── Backend Spec.html      ← architecture, événements, règles, erreurs
│   ├── API Contracts.html     ← tous les endpoints
│   ├── Product Backlog.html   ← epics et user stories
│   ├── Home.html, Search.html, Cart.html, …   ← maquettes
│   ├── styles\tokens.css      ← design tokens, valeurs exactes
│   └── components\*.jsx       ← composants de la maquette
├── backend\               ← Spring Boot, Maven, Java 21
└── frontend\              ← à créer ensuite (Next.js)
```

## 3. Lancement

```powershell
cd "C:\Users\haith\OneDrive\Bureau\B2B-2026"
claude
```

Puis collez le contenu de **`PROMPT-01-backend.md`** comme premier message.

## 4. Ordre de travail imposé

1. **Backend d'abord**, jusqu'à Swagger complet et testé. Lots 1 et 2 du backlog (socle + flux Kafka) : réalisables immédiatement.
2. **Frontend ensuite**, généré contre le client TypeScript produit depuis l'OpenAPI du backend.

Ne démarrez pas le frontend avant que `/swagger-ui` expose les endpoints du catalogue, du panier et des commandes : sinon le front est écrit contre des types inventés.

## 5. Point de blocage à connaître

Les modèles **Produit, Stock, Tarif, Client** et le **format de commande** ne sont pas spécifiés — ils doivent venir du logiciel de gestion du client (points TBD-01 à TBD-06 de la spécification, §14). Claude Code doit s'arrêter et poser la question plutôt que d'inventer des champs. C'est écrit dans `CLAUDE.md` comme règle absolue.

## 6. Conseils d'usage de Claude Code

- Une story du backlog = une session = un commit. Référencez son identifiant (`E1-03`) dans le prompt et dans le message de commit.
- `/init` au premier lancement s'il propose d'enrichir `CLAUDE.md` : acceptez, il ajoutera les commandes de build réelles.
- Demandez-lui de lire le document avant de coder : « lis docs/Backend Spec.html §5 avant d'implémenter ».
- Faites-lui écrire les tests en même temps que le code : les critères d'acceptation du backlog sont déjà rédigés pour ça.
