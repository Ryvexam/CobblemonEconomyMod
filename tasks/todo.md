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

- `npm test -- --run` dans `tools/minecraft-agent` : **37 tests passants**, 10 fichiers.
- `npm run build` dans `tools/minecraft-agent` : **succès**.
- `npm run --silent cli -- inspect --project ../.. --json` : **Fabric 1.21.1**, mod `cobblemon-economy` détecté, aucun fichier modifié.
- `npm run --silent cli -- artifact --project ../.. --json` : **2 JAR détectés**, avec SHA-256 (`cobblemon-economy-0.0.17.jar` et `-sources.jar`).
- Serveur MCP : handshake `initialize` et `tools/list` vérifiés via stdio ; six outils annoncés, aucune sortie parasite sur stdout.
- `./gradlew tasks --no-daemon --console=plain` : **succès** ; `runServer` est disponible, `runGameTestServer` ne l’est pas dans ce projet.
- Test réel `runServer` : **échec attendu et correctement classifié `mod_loading`** ; Cobblemon 1.7.1 et des dépendances YAWP compatibles sont absents/incompatibles dans l’environnement local.
- `./gradlew build --no-daemon --console=plain` : **BUILD SUCCESSFUL** en 7 secondes ; avertissement non bloquant sur la version SemVer de SQLite.
