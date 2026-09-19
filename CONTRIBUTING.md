# Contributing to Cobblemon Economy

This guide is for code, configuration, documentation, and release work on the
Fabric 1.21.1 / Cobblemon 1.8.1 development line, with runtime compatibility
for Cobblemon 1.7.x. Read [`docs/architecture.md`](docs/architecture.md)
before changing a cross-cutting system.

## Prerequisites

- Java 21 (`java -version`)
- Git
- Network access for the first Gradle dependency resolution
- A Fabric 1.21.1 test server with Cobblemon and the optional mods relevant to
  the feature being tested
- Node.js/npm only when using the standalone local Minecraft agent in
  `../minecraft-agent`

The Gradle wrapper is the source of truth for build commands. Do not install a
system Gradle version to work around a wrapper failure before inspecting the
actual error.

The compatibility build uses the Fabric Cobblemon version ID
`gBW3vLC7` (1.8.1). To compile-check the same source against Cobblemon 1.7.1,
override the Modrinth version ID and matching platform dependencies:

```bash
./gradlew clean compileJava --no-daemon --console=plain \
  -Pcobblemon_version=1.7.1 \
  -Pcobblemon_modrinth_version=s64m1opn \
  -Ploader_version=0.16.5 \
  -Pfabric_version=0.103.0+1.21.1
```

## Daily development loop

### 1. Inspect before editing

```bash
git status --short
./gradlew tasks --all
```

For the reusable local harness:

```bash
AGENT=../minecraft-agent
node "$AGENT/dist/cli.js" inspect --project . --json
```

If the standalone agent has not been built in this checkout:

```bash
(cd ../minecraft-agent && npm ci && npm run build && npm test)
```

### 2. Define the change boundary

Before writing code, identify which layer owns the behavior:

- lifecycle and callbacks: `fabric/CobblemonEconomy.java`
- persisted economy state: `storage/EconomyManager.java`
- JSON defaults/compatibility: `storage/EconomyConfig.java`
- shop interaction and transactions: `shop/ShopGui.java`
- Cobblemon events/rewards: `events/CobblemonListeners.java`
- quests and progress: `quest/QuestService.java` and `QuestManager.java`
- client/server payloads: `networking/` and `client/`
- optional mods: `compat/` and `mixin/`

Prefer a small service-level change over adding more behavior to the Fabric
entrypoint. Preserve server authority for balances, item limits, quest state,
and rewards.

### 3. Implement with the data contract in mind

- Use `BigDecimal` for currency values; do not introduce `float`/`double`
  arithmetic for balances.
- Keep world data under `world/config/cobblemon-economy/`.
- Use prepared SQL statements and document any schema migration before
  changing `economy.db` or `quests.db`.
- Validate client payloads against server configuration.
- Keep optional integrations behind their existing compatibility boundaries.
- Add or update both `en_us.json` and `fr_fr.json` for player-facing text.
- Update `README.md` and `CURSEFORGE.md` when a command or admin-facing config
  key changes.
- Add a `CHANGELOG.md` entry for user-visible behavior.

### 4. Verify configuration changes

Start a disposable test world once so the mod generates current defaults. Edit
only the relevant file, then use:

```text
/eco reload
```

`/eco reload` reloads `config.json`, `shops.json`, `quests.json`,
`quest_npcs.json`, and `quest_boards_bindings.json`. It does not add missing
mods or replace the SQLite managers; restart after dependency or database
changes.

Keep a backup of the whole `world/config/cobblemon-economy/` directory before
editing production data. Treat `economy.db` and `quests.db` as persistent data,
not disposable build output.

## Feature checklists

### Economy or command change

- [ ] Check the selected `main_currency` backend and optional bridge behavior.
- [ ] Check zero, negative, large, and decimal amounts.
- [ ] Check self-payment, insufficient balance, and offline/online assumptions.
- [ ] Check username updates and transaction logging.
- [ ] Verify `/bal`, `/balance`, `/pco`, `/pay`, and permission-level-2 paths.

### Shop change

- [ ] Test a normal item, a component-customized item, and an invalid item ID.
- [ ] Test buy and sell paths, quantity selection, linked shops, and currency.
- [ ] Test command items only with trusted server configuration.
- [ ] Test buy/sell limits and both lifetime and timed cooldowns.
- [ ] Test malformed JSON and confirm one broken entry does not hide the shop.
- [ ] Verify the transaction log and resulting inventory/balance.

### Quest or reward change

- [ ] Test accept, active progress, completion, claim, cooldown, cancellation,
  prerequisites, and rotation behavior.
- [ ] Test the relevant event source: capture, Pokédex, battle, raid, tower,
  or fossil revival.
- [ ] Test both normal and special Pokémon filters where applicable.
- [ ] Verify rewards are granted once and objective progress is persisted.
- [ ] Test a board block binding in more than one dimension when bindings are
  part of the change.

### Networking or client UI change

- [ ] Validate every incoming action server-side.
- [ ] Test a missing/unknown ID and a stale client state.
- [ ] Test reconnect/reload behavior and client cache behavior.
- [ ] Keep payload sizes bounded and update both sides of each codec.

## Build and runtime verification

Run the fast static checks first:

```bash
./gradlew build --no-daemon --console=plain
```

Using the standalone agent from the repository root:

```bash
AGENT=../minecraft-agent
node "$AGENT/dist/cli.js" build --project . --task build --timeout 600000
node "$AGENT/dist/cli.js" artifact --project . --json
node "$AGENT/dist/cli.js" logs --project .
```

A controlled server run is useful only when the local `mods/` directory
contains compatible runtime dependencies:

```bash
node "$AGENT/dist/cli.js" test --project . --task runServer --timeout 900000
```

Inspect the persisted run output under `.minecraft-agent/runs/`. Classify the
result as `success`, `compilation`, `dependency`, `mod_loading`, or `timeout`.
Do not report a runtime test as passing when Cobblemon or an optional
integration is absent/incompatible; report the classification and the missing
dependency instead.

The Java mod has a focused `src/test/` suite for compatibility predicates.
Source review, Gradle tests/build, generated-config checks, and a
disposable-server smoke test remain complementary rather than interchangeable.

## Documentation and review

When behavior changes, update documentation in this order:

1. source comments/Javadocs for non-obvious invariants;
2. `README.md` for general users;
3. `CURSEFORGE.md` for server operators;
4. `docs/architecture.md` for implementation flow;
5. `CHANGELOG.md` for the release-facing summary.

Before committing:

```bash
git diff --check
git status --short
```

Keep commits focused. Mention the target mod version (`0.0.17` at the time of
this document) in commit messages when the change is release-related. Never
commit a production world, database, generated run logs, or machine-specific
OpenCode/MCP paths.

## Current audit

The known risks and deferred hardening work are tracked in
[`docs/technical-audit.md`](docs/technical-audit.md). Treat that document as a
review backlog, not as evidence that an item has already been fixed.
