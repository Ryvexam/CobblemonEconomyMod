# Spécification — Agent local de développement Minecraft

**Date :** 2026-09-19  
**Statut :** Proposition validée en conversation, à relire avant implémentation  
**Périmètre :** outil local, sans cloud ni hébergement

## 1. Objectif

Construire un outil local qui permet à OpenCode de créer et maintenir des mods
Minecraft plus rapidement et plus proprement, avec une boucle reproductible :

```text
inspecter → modifier → compiler → lancer un test → lire les logs → corriger
```

Le projet actuel `CobblemonEconomyMod` sert de premier projet réel de
validation. L’outil doit également pouvoir initialiser de nouveaux projets
Fabric compatibles avec les versions prises en charge.

La qualité recherchée vient de l’exécution réelle des builds et tests, pas d’une
simple génération de code par le modèle.

## 2. Contexte existant

- Loader actuel : Fabric.
- Minecraft actuel : 1.21.1.
- Java actuel : 21.
- Build : Gradle 8.10.2 avec Fabric Loom.
- Projet existant : mod serveur Cobblemon Economy.
- Documentation agent déjà présente : `agent.md`, `skills.md` et
  `knowledge-cobblemoneconomy.md`.
- Skill globale disponible : `minecraft-modding`.

Le premier build de référence a dépassé dix minutes sans atteindre une sortie
de compilation exploitable. Le runner devra donc exposer clairement la phase
bloquante (résolution Gradle, configuration Loom, compilation ou lancement)
plutôt que masquer un timeout derrière une erreur générique.

## 3. Architecture

```text
OpenCode + skill minecraft-modding
              │
              │ MCP local via stdio
              ▼
     minecraft-agent (TypeScript)
       ┌──────────────┬───────────────┐
       │ CLI          │ MCP server    │
       └──────┬───────┴───────┬───────┘
              ▼               ▼
       moteur partagé : contexte projet, Gradle, logs, artefacts, sécurité
                              │
                              ▼
                    projet Minecraft local
```

Le CLI et le serveur MCP ne doivent pas dupliquer la logique. Ils appellent les
mêmes fonctions de domaine et retournent les mêmes résultats structurés.

OpenCode reste l’agent de raisonnement : il lit les résultats, décide des
modifications et appelle à nouveau les outils. Le moteur local ne contient pas
de modèle IA dans cette première version.

## 4. Composants et emplacement

Le nouvel outil sera isolé dans `tools/minecraft-agent/` afin de ne pas
introduire de dépendances Node dans le build Gradle du mod.

```text
tools/minecraft-agent/
├── package.json
├── tsconfig.json
├── README.md
├── src/
│   ├── cli.ts
│   ├── mcp.ts
│   ├── core/
│   │   ├── project-context.ts
│   │   ├── project-inspector.ts
│   │   ├── gradle-runner.ts
│   │   ├── minecraft-runner.ts
│   │   ├── log-parser.ts
│   │   ├── artifact-manager.ts
│   │   └── safety-policy.ts
│   ├── commands/
│   │   ├── init.ts
│   │   ├── inspect.ts
│   │   ├── build.ts
│   │   ├── test.ts
│   │   ├── logs.ts
│   │   └── artifact.ts
│   └── mcp-tools/
│       ├── inspect-project.ts
│       ├── create-project.ts
│       ├── build-project.ts
│       ├── test-project.ts
│       ├── read-logs.ts
│       └── get-artifact.ts
└── templates/
    └── fabric-1.21.1/
```

Les templates seront versionnés et explicites. Aucun template ne sera choisi
uniquement à partir d’une supposition du modèle.

## 5. CLI locale

Le binaire sera nommé `minecraft-agent`. Chaque commande devra fonctionner
avec un chemin de projet explicite ou le répertoire courant.

```bash
minecraft-agent init <path> --loader fabric --mc 1.21.1
minecraft-agent inspect [--project <path>] [--json]
minecraft-agent build [--project <path>] [--timeout <seconds>] [--json]
minecraft-agent test [--project <path>] [--timeout <seconds>] [--json]
minecraft-agent logs [--project <path>] [--since <run-id>] [--json]
minecraft-agent artifact [--project <path>] [--json]
```

Règles CLI :

- sortie humaine lisible par défaut ; JSON stable avec `--json` ;
- code de sortie non nul si une étape échoue ;
- chaque exécution reçoit un `run_id` ;
- les chemins et le projet cible sont affichés avant une action longue ;
- les timeouts sont configurables mais ont une valeur par défaut ;
- aucune action destructive ou déploiement distant dans le MVP.

## 6. Outils MCP

Le serveur MCP utilisera `stdio` pour être lancé directement par OpenCode. Les
outils exposés seront limités et typés :

### `inspect_project`

Détecte le loader, la version Minecraft, Java, Gradle, le mod ID, les points
d’entrée et les tâches Gradle disponibles. Ne modifie rien.

### `create_project`

Crée un projet depuis un template explicitement sélectionné. Refuse d’écraser
un répertoire non vide sans confirmation explicite.

### `build_project`

Exécute le wrapper Gradle du projet, collecte stdout/stderr, tâche courante,
durée, code de sortie et erreurs pertinentes. Le résultat contient aussi les
artefacts présents dans `build/libs`.

### `test_project`

Lance une vérification Minecraft définie par le projet. Dans la première
version, la stratégie par défaut est de lancer le serveur de test approprié,
d’attendre son signal de démarrage, collecter les logs et l’arrêter proprement.

### `read_logs`

Retourne les logs d’une exécution avec filtrage par niveau et extraction des
erreurs, exceptions, causes et stack traces. Les sorties sont tronquées avec
un indicateur explicite si elles dépassent la limite MCP.

### `get_artifact`

Liste ou copie les artefacts construits vers un chemin de sortie contrôlé. Il
retourne le chemin, la taille et le hash SHA-256.

Les outils ne donneront pas à l’agent un shell arbitraire. Les arguments seront
validés par schéma et les actions seront bornées au projet sélectionné.

## 7. Boucle de qualité

Le modèle orchestrera la boucle, mais chaque étape doit être déterministe :

1. `inspect_project` établit le contexte réel.
2. OpenCode produit une modification ciblée.
3. `build_project` compile avec le wrapper du projet.
4. En cas d’échec, `read_logs` expose la cause racine sans noyer le modèle.
5. OpenCode applique une correction minimale.
6. Le build est relancé, avec un nombre maximal de tentatives configurable.
7. Une fois le build vert, `test_project` vérifie le chargement réel dans
   Minecraft.
8. `get_artifact` retourne le jar remappé attendu.

Le runner doit distinguer :

- résolution de dépendances ;
- configuration Gradle/Loom ;
- compilation Java/Kotlin ;
- exécution du serveur ;
- chargement du mod ;
- test fonctionnel ;
- packaging de l’artefact.

Un timeout ne sera jamais présenté comme un échec de compilation sans preuve.

## 8. Sécurité locale

- Le projet cible est résolu vers un chemin absolu et doit rester sous un
  répertoire autorisé par la configuration locale.
- Les commandes exécutées sont une allowlist Gradle et Minecraft connue.
- Les variables d’environnement et secrets ne seront pas transmis aux prompts
  ni aux réponses MCP.
- Les fichiers générés restent dans le projet ou dans un répertoire d’artefacts
  local dédié.
- Le lancement Minecraft est limité à la machine locale et à des ports
  configurés.
- Les téléchargements ou exécutions de fichiers arbitraires sont hors scope.
- Un futur mode sandbox Docker pourra être ajouté sans changer les interfaces
  CLI/MCP.

## 9. Initialisation des projets

Le template Fabric 1.21.1 minimal devra fournir :

- wrapper Gradle fonctionnel ;
- `settings.gradle.kts` et `build.gradle.kts` ;
- `gradle.properties` ;
- entrypoint Fabric ;
- `fabric.mod.json` ;
- source et ressources minimales ;
- tâche de build vérifiable par le runner.

Le support d’autres loaders ou versions sera ajouté par template séparé. Il ne
sera pas déduit en modifiant silencieusement un template existant.

## 10. Vérification et critères d’acceptation

Le MVP sera accepté lorsque :

1. `minecraft-agent inspect --json` détecte correctement ce dépôt ;
2. `minecraft-agent build --json` retourne un résultat structuré et un code
   de sortie fidèle ;
3. les timeouts et erreurs Gradle identifient leur phase ;
4. un projet Fabric minimal peut être initialisé puis compilé ;
5. `test_project` peut démarrer et arrêter proprement le serveur de test, ou
   signaler explicitement la précondition manquante ;
6. `get-artifact` identifie le bon jar et son hash ;
7. les mêmes capacités sont accessibles via MCP local ;
8. aucun outil ne peut écrire hors du projet autorisé ;
9. les tests unitaires couvrent le parsing de projet, la classification des
   erreurs et la validation des chemins ;
10. la documentation explique l’installation et la configuration OpenCode.

## 11. Découpage d’implémentation proposé

### Phase 1 — moteur et diagnostic

Créer le package TypeScript, le contexte projet, l’inspection, la politique de
chemins et le runner Gradle avec sorties structurées. Résoudre d’abord le
problème de visibilité du build actuel.

### Phase 2 — CLI

Ajouter `inspect`, `build`, `logs`, `artifact`, puis `init` avec le template
Fabric 1.21.1.

### Phase 3 — test Minecraft

Ajouter le lancement contrôlé du serveur, la détection du signal de démarrage,
la collecte des logs et l’arrêt propre.

### Phase 4 — MCP et OpenCode

Exposer les mêmes opérations via `stdio`, ajouter la configuration OpenCode
locale ou globale, puis tester un scénario complet depuis OpenCode.

### Phase 5 — qualité

Ajouter fixtures, tests d’intégration, limites de sortie, documentation,
hash d’artefacts et éventuellement un mode sandbox.

## 12. Décisions et compromis

- **TypeScript plutôt que Java/Kotlin :** meilleur accès au SDK MCP et à la CLI,
  sans coupler l’outillage au build du mod.
- **OpenCode comme orchestrateur :** évite de développer immédiatement un
  second agent IA et permet de profiter de `minecraft-modding`.
- **Moteur partagé CLI/MCP :** une seule implémentation à tester et à sécuriser.
- **Local-first :** feedback rapide, pas de comptes ni de coûts cloud, adapté au
  besoin immédiat.
- **Pas de déploiement distant au MVP :** réduit fortement le risque lié aux
  permissions et aux secrets.
