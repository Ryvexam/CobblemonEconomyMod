# Persistence Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add lossless, versioned SQLite/JSON persistence migrations and document the mod architecture.

**Architecture:** A small SQLite migration runner owns schema versioning, backups, transactions, and future-version refusal. Economy and quest schema classes provide additive version-one migrations. A shared JSON file store provides atomic writes, backups, and malformed-file quarantine while existing loaders preserve legacy aliases and inline shops.

**Tech Stack:** Java 21, Fabric Loom, SQLite JDBC, Gson, JUnit 5, Mermaid diagrams in Markdown.

**Spec:** `docs/superpowers/specs/2026-09-19-persistence-compatibility-spec.md`

## Global Constraints

- Minecraft `1.21.1`, Fabric, Java `21`.
- Preserve existing economy balances, PCO, limits, capture milestones, quest state, and quest progress.
- Preserve legacy JSON names and inline `config.json.shops` fallback.
- Never silently run against a database schema newer than the bundled code.
- Keep `/eco reload` JSON-only; database migration happens at server startup.
- Do not rename persistent shop, item, quest, or NPC IDs.

## Review Focus

- Version-zero economy DB containing only the original balances table: must preserve rows and add all current tables/columns.
- Existing quest DB with state/progress rows: must preserve rows while recording the current schema version.
- Database with a future `user_version`: must be rejected and left untouched.
- Malformed JSON: must be quarantined before fallback/default generation.
- Legacy inline shops and old aliases: must still load and export without losing shop IDs.

---

### Task 1: Add failing persistence tests

**Files:**
- Create: `src/test/java/com/cobblemon/economy/storage/SqliteMigrationTest.java`
- Create: `src/test/java/com/cobblemon/economy/storage/ConfigFileStoreTest.java`

- [x] Write tests for legacy SQLite preservation, backup creation, future-version refusal, and atomic JSON backup/quarantine behavior.
- [x] Run `./gradlew test --tests 'com.cobblemon.economy.storage.*' --no-daemon --console=plain` and confirm the tests fail because the migration/store APIs do not exist.

### Task 2: Implement SQLite migration infrastructure

**Files:**
- Create: `src/main/java/com/cobblemon/economy/storage/SqliteMigrationRunner.java`
- Create: `src/main/java/com/cobblemon/economy/storage/EconomyDatabaseSchema.java`
- Create: `src/main/java/com/cobblemon/economy/quest/QuestDatabaseSchema.java`

- [x] Implement transactional `PRAGMA user_version` migrations, `.bak` creation, sequential migration checks, and future-version refusal.
- [x] Implement additive version-one economy and quest schemas.
- [x] Run the focused migration tests and confirm they pass.

### Task 3: Integrate schema migrations into runtime managers

**Files:**
- Modify: `src/main/java/com/cobblemon/economy/storage/EconomyManager.java:23-81`
- Modify: `src/main/java/com/cobblemon/economy/quest/QuestManager.java:19-93`

- [x] Replace direct schema initialization with the versioned schema classes.
- [x] Preserve the existing public manager APIs and error semantics for normal operations.
- [x] Run the complete Gradle test suite and build.

### Task 4: Add safe JSON file writes and config versions

**Files:**
- Create: `src/main/java/com/cobblemon/economy/storage/ConfigFileStore.java`
- Modify: `src/main/java/com/cobblemon/economy/storage/EconomyConfig.java`
- Modify: `src/main/java/com/cobblemon/economy/storage/QuestConfig.java`
- Modify: `src/main/java/com/cobblemon/economy/storage/QuestNpcConfig.java`
- Modify: `src/main/java/com/cobblemon/economy/questboard/QuestBoardBindings.java`

- [x] Add atomic writes, `.bak` backups, malformed-file quarantine, and `configVersion` output.
- [x] Keep legacy JSON aliases and inline shop fallback; external shops remain preferred.
- [x] Run focused JSON tests and the complete Gradle suite.

### Task 5: Document architecture and operator migration procedure

**Files:**
- Modify: `docs/architecture.md`
- Create: `docs/persistence-compatibility.md`
- Modify: `README.md`
- Modify: `CURSEFORGE.md`
- Modify: `CONTRIBUTING.md`
- Modify: `CHANGELOG.md`
- Modify: `tasks/todo.md`

- [x] Add Mermaid runtime and persistence diagrams.
- [x] Document database files, schema versions, backups, downgrade rules, and stable IDs.
- [x] Add release-facing migration notes.

### Task 6: Verify end to end

- [x] Run `git diff --check`.
- [x] Run `./gradlew clean test build --no-daemon --console=plain`.
- [x] Inspect SQLite schema/user versions and backup artifacts from the tests.
- [x] Run the disposable dedicated-server smoke test and verify init/reload/shop/quest/stop markers.
- [x] Review the final diff and working tree.
