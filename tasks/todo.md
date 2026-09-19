# Installation de `obra/superpowers`

- [x] Examiner la méthode d’installation officielle et l’environnement OpenCode.
- [x] Installer les skills dans l’emplacement compatible avec OpenCode.
- [x] Vérifier les fichiers installés et la configuration détectée.
- [x] Documenter le résultat et les éventuels points de vigilance.

## Revue

- Installation globale effectuée via `opencode plugin add`.
- Plugin détecté : `superpowers` à la révision `5bf4e78`.
- OpenCode recharge le serveur sur `http://127.0.0.1:49374`.
- Le cache contient 15 fichiers `SKILL.md`, dont `brainstorming`, `using-superpowers` et `verification-before-completion`.
- La configuration projet du dépôt n’a pas été modifiée ; seule la configuration globale OpenCode a reçu le plugin.

## Skill Minecraft Modding

- Installation demandée exécutée avec `npx skillfish add jahrome907/minecraft-agent-skills minecraft-modding`.
- Skill installé globalement pour 12 agents, dont OpenCode.
- Emplacement OpenCode : `~/.opencode/skills/minecraft-modding/SKILL.md`.
- Skill découvert par OpenCode sous le nom `minecraft-modding`.

## Documentation et audit Cobblemon Economy

- [x] Cartographier et documenter l’architecture runtime réelle.
- [x] Ajouter un guide contributeur/opérateur reproductible.
- [x] Produire un audit technique séparant faits vérifiés et recommandations.
- [x] Ajouter les liens de navigation depuis les guides existants.
- [x] Vérifier les références, le diff et le build Gradle.

Plan : `docs/superpowers/plans/2026-09-19-cobblemon-economy-documentation-plan.md`
Spécification : `docs/superpowers/specs/2026-09-19-cobblemon-economy-documentation-spec.md`

### Revue

- `git diff --check` : OK.
- Références Markdown des documents actifs : 11 liens vérifiés, aucun chemin manquant.
- Références actives à l’ancien chemin `tools/minecraft-agent` : aucune ; les anciens plans Superpowers conservent volontairement leur historique.
- `./gradlew build --no-daemon --console=plain` : **BUILD SUCCESSFUL** (8 tâches, 1 exécutée, 7 à jour).
- Aucun code Java/Kotlin ou ressource runtime modifié pendant cette passe.
- La référence Cobblemon 1.8.1 a été examinée : migration possible sur MC 1.21.1/Java 21, mais nécessite une montée coordonnée du Loader/API ; aucune migration appliquée.

## Setup agent Minecraft type Orca

- [x] Choisir le périmètre initial : assistant local pour créer et maintenir des mods, sans cloud.
- [x] Définir l’architecture validée : orchestrateur, outils CLI, serveur MCP, build/test Minecraft et sandbox.
- [x] Implémenter le MVP approuvé et sa boucle de vérification.
- [x] Vérifier le build Gradle, les outils et les contrôles de sécurité.

Implementation plan: `docs/superpowers/plans/2026-09-19-local-minecraft-agent-plan.md`

- [x] Task 1 — package TypeScript
- [x] Task 2 — inspection et sécurité des chemins
- [x] Task 3 — runner Gradle, logs et artefacts
- [x] Task 4 — CLI et template Fabric
- [x] Task 5 — exécution de tests Minecraft
- [x] Task 6 — serveur MCP et intégration OpenCode
- [x] Task 7 — documentation et vérification finale

## Vérification finale

- `npm test -- --run` dans `../minecraft-agent` : **46 tests passants**, 10 fichiers.
- `npm run build` dans `../minecraft-agent` : **succès**.
- `npm run --silent cli -- inspect --project ../.. --json` : **Fabric 1.21.1**, mod `cobblemon-economy` détecté, aucun fichier modifié.
- `npm run --silent cli -- artifact --project ../.. --json` : **2 JAR détectés**, avec SHA-256 (`cobblemon-economy-0.0.17.jar` et `-sources.jar`).
- Serveur MCP : handshake `initialize` et `tools/list` vérifiés via stdio ; six outils annoncés, aucune sortie parasite sur stdout.
- Inspection finale : Java 21 détecté, huit tâches Gradle inférées dont `runServer`, diagnostics vides pour le projet valide.
- `./gradlew tasks --no-daemon --console=plain` : **succès** ; `runServer` est disponible, `runGameTestServer` ne l’est pas dans ce projet.
- Test réel `runServer` : **échec attendu et correctement classifié `mod_loading`** ; Cobblemon 1.7.1 et des dépendances YAWP compatibles sont absents/incompatibles dans l’environnement local.
- `./gradlew build --no-daemon --console=plain` : **BUILD SUCCESSFUL** en 7 secondes ; avertissement non bloquant sur la version SemVer de SQLite.

## Compatibilité Cobblemon 1.7/1.8

- [x] Confirmer la cause du crash : `PokedexEntryProgress.CAUGHT` a été remplacé par `OWNED` en 1.8.1.
- [x] Ajouter le prédicat indépendant de version et ses tests JUnit (`CAUGHT`, `OWNED`, statuts non propriétaires).
- [x] Remplacer les références binaires Pokédex et adapter le constructeur `ModelWidget` 1.7/1.8 par réflexion.
- [x] Définir le build de référence sur Cobblemon 1.8.1, Loader 0.17.2 et Fabric API 0.116.6+1.21.1.
- [ ] Vérifier le serveur réel avec un pack de mods local Cobblemon 1.8.1 complet.

### Revue

- Test TDD rouge observé avant création de `PokedexProgressCompat`, puis `./gradlew test` vert.
- La première tentative 1.8.1 a révélé que `maven.modrinth:cobblemon:1.8.1` était l’artefact NeoForge ; la coordonnée Fabric correcte est `maven.modrinth:MdwFAVRL:gBW3vLC7`.
- Compilation Fabric 1.8.1 réelle : **BUILD SUCCESSFUL** avec `gBW3vLC7`, Loom 1.16.2, Gradle 9.4.1 et Kotlin 2.3.20.
- Compilation Fabric 1.7.1 réelle : **BUILD SUCCESSFUL** avec `s64m1opn`, Loader 0.16.5 et Fabric API 0.103.0+1.21.1.
- `./gradlew test build` : **BUILD SUCCESSFUL**.
- MCP inspect/build : succès, build run `1789824822971-e116094e`.
- MCP serveur : `mod_loading` uniquement sur `forgeconfigapiport` absent de YAWP, run `1789824832959-d68ddb14`; Fabric Loader atteint la résolution des dépendances, mais le démarrage complet reste non vérifié.

## CI release Forgejo

- [x] Ajouter le workflow `.forgejo/workflows/release.yml`, déclenché uniquement par les tags `v*`.
- [x] Construire avec la version issue du tag et publier le JAR principal via les API Modrinth/CurseForge.
- [x] Limiter les tokens aux secrets Forgejo et documenter la configuration requise.
- [x] Déplacer les IDs publics et la configuration de plateforme dans `.forgejo/release-config.json`.
- [ ] Exécuter un premier tag de test avec les secrets et IDs de projets configurés sur Forgejo.

### Revue CI

- YAML Ruby parse, tous les blocs `run` passent `bash -n`, et aucun workflow GitHub n’a été ajouté.
- Simulation du tag `v9.8.7` : sorties `version=9.8.7` et `tag=v9.8.7` correctes ; `./gradlew clean test build` produit puis sélectionne `cobblemon-economy-9.8.7.jar`.
- Notes `Unreleased`, commande Modrinth et commande CurseForge validées avec un faux `curl`; aucun appel réel de publication n’a été effectué.
- Ruling: utiliser `setup-java` puis installer `curl`/`jq` dans le runner Docker — l’image Forgejo par défaut n’assure pas Java 21, et cela évite de dépendre d’une image personnalisée.
- Ruling: lancer `clean` avant le build — un workspace réutilisé peut contenir le JAR d’une version précédente et rendre la sélection ambiguë.

### Smoke test CurseForge local

- JAR ajouté : `cobblemon-economy-0.0.17.jar`, avec Cobblemon Fabric `1.8.1+1.21.1` et Fabric API `0.116.17+1.21.1`.
- Lancement terminal Java 21 validé : Fabric Loader `0.19.5` a chargé `79 mods`, puis le log a confirmé `Launching Cobblemon 1.8.1` avec `cobblemon-economy 0.0.17`.
- Le client a été arrêté après l’initialisation par le harness de test ; les erreurs « No data fixer registered » viennent de Cobblemon et ne sont pas un échec de chargement du mod.

### Extraction standalone

- Harness déplacé hors du dépôt du mod vers `../minecraft-agent/`.
- Dépôt autonome initialisé dans ce dossier : commit `49fa28a`.
- Vérification depuis le dossier standalone : **46 tests passants**, build TypeScript passant, inspection du mod et handshake MCP validés.
