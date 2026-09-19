# Version compatibility and upgrade matrix

This document records the compatibility behavior of the Cobblemon Economy JARs
currently available in the local `Downloads` directory and describes the safe
upgrade path for a real server.

## Test environment

The runtime matrix was executed on 2026-09-19 with:

| Component | Test value |
| --- | --- |
| Minecraft | `1.21.1` |
| Fabric Loader | `0.19.5` |
| Fabric API | `0.116.17+1.21.1` |
| Cobblemon | Fabric `1.8.1+1.21.1` |
| Java | 21 |
| Runtime | Dedicated server, offline test world |

The JARs were tested one at a time, never side by side. Each version reused the
same temporary world so the next version started with the previous version's
configuration and databases.

## JARs found and tested

| JAR | Metadata | Runtime result | Persistence role |
| --- | --- | --- | --- |
| `0.0.13` | Fabric, MC `~1.21.1`, Cobblemon `>=1.7.1` | Started, initialized, generated the legacy world, reloaded config, listed shops, stopped cleanly | Purchase limits, capture reward, milestones, `economy.db` |
| `0.0.14` | Fabric, MC `~1.21.1`, Cobblemon `>=1.7.1` | Started, initialized, reloaded config, listed shops, stopped cleanly | Command shop items, fossil rewards, placeholders, same economy DB family |
| `0.0.15` | Fabric, MC `~1.21.1`, Cobblemon `>=1.7.1` | Started, initialized, reloaded config, listed shops, stopped cleanly | Sell limits, currency backend settings, raid/tower settings |
| `0.0.16` | Fabric, MC `~1.21.1`, Cobblemon `>=1.7.1` | Started, initialized, reloaded config, listed shops/quêtes, stopped cleanly | `shops.json`, `quests.json`, `quest_npcs.json`, `quests.db` |
| `0.0.17` | Fabric, MC `~1.21.1`, Cobblemon `>=1.7.1` | Started, initialized, reloaded config, listed shops/quêtes, stopped cleanly | Quest boards, bindings, modern quest data |
| `0.0.18` | Fabric, MC `~1.21.1`, Cobblemon `>=1.7.1` | Migrated the complete legacy chain, preserved fixture data, reloaded config, stopped cleanly | SQLite schema version `1`, atomic JSON writes |

The old JARs were runtime-tested against Cobblemon `1.8.1`. The current source
was also compile-checked against Cobblemon `1.7.1` with its matching Loader and
Fabric API. A separate runtime test against Cobblemon `1.7.1` still requires a
local 1.7.1 server dependency set.

## What changed across releases

### `0.0.13`

- Added per-item purchase limits and cooldowns.
- Added capture reward configuration and capture milestones.
- Added `milestone.json`.
- Existing balance rows remain in `economy.db`.

### `0.0.14`

- Added command-type shop items.
- Added fossil revival rewards.
- Added placeholder and profiling support.
- No destructive database replacement is required.

### `0.0.15`

- Added sell limits.
- Added currency backend selection (`cobeco`, `cobbledollars`, `impactor`).
- Added raid and Battle Tower reward settings.
- Added optional external economy bridges.

External CobbleDollars/Impactor balances are owned by those mods. Cobblemon
Economy only migrates its own SQLite data; changing the selected backend is a
configuration decision and is not an automatic transfer between external
economies.

### `0.0.16`

- Added `shops.json` as the preferred shop file.
- Added `quests.json` and `quest_npcs.json`.
- Added `quests.db` for quest state and objective progress.
- If `shops.json` is absent, legacy inline `config.json.shops` is exported.

### `0.0.17`

- Added quest board blocks and board bindings.
- Added compatibility behavior for modern Cobblemon APIs.
- Existing quest IDs and board bindings become persistent references.

## Complete migration test

The actual test sequence was:

```text
clean world
  ↓
JAR 0.0.13
  ↓
JAR 0.0.14
  ↓
JAR 0.0.15
  ↓
JAR 0.0.16
  ↓
JAR 0.0.17 from Downloads
  ↓
insert known legacy balance/limit/capture/quest rows
  ↓
JAR 0.0.18
  ↓
old 0.0.13, 0.0.14, 0.0.15, 0.0.16 and 0.0.17 again for downgrade verification
```

Before starting the current source build, the legacy databases reported:

```text
economy.db: user_version = 0
quests.db:  user_version = 0
```

After the `0.0.18` build:

```text
economy.db: user_version = 1
quests.db:  user_version = 1
economy.db.bak: created
quests.db.bak: created
```

The following legacy values were verified after migration:

```text
balance:          1234.56
pco:              42
username:         LegacyTester
purchase count:   2
sell count:       3
capture count:    77
quest status:     ACTIVE
quest progress:   6
```

Every old JAR from `0.0.13` through `0.0.17` then started successfully on the
migrated databases and the known balance/progress values remained available.
This confirms that the current version-1 schema is additive and does not block
a temporary downgrade to any tested old release.

## What happens on a normal upgrade

1. Stop the server.
2. Back up the whole `world/config/cobblemon-economy/` directory.
3. Remove the old Cobblemon Economy JAR from `mods/`; do not leave two versions.
4. Install the new JAR.
5. Start the server.
6. The server loads JSON compatibility aliases and legacy inline shops.
7. The SQLite runner reads `PRAGMA user_version` for each database.
8. If the database is legacy version `0`, the mod creates a `.bak` and applies
   the additive migration in a transaction.
9. Check the log for:

   ```text
   Economy database schema ready at version 1
   Quest database schema ready at version 1
   ```

10. Verify `/bal`, `/pco`, `/eco shop list`, and `/eco quest list`.
11. Use `/eco reload` after JSON edits.

`/eco reload` never replaces SQLite managers and never resets balances or quest
progress. Restart the server for database or mod dependency changes.

## JSON compatibility rules

- Missing `shops.json`: read `config.json.shops`, then export `shops.json`.
- Both shop formats present: a non-empty `shops.json` is authoritative; a
  missing or empty separate file can fall back to the inline copy, which is
  retained for downgrade compatibility.
- Legacy aliases such as `mainCurrency`, `captureReward`, `shinyMultiplier`,
  and related snake/camel variants remain readable.
- Current structured JSON writes include `configVersion: 1`.
- Existing JSON files are backed up as `<file>.bak` before replacement.
- Malformed files are copied to `<file>.broken-<timestamp>` before fallback
  defaults or legacy data are used.

## SQLite compatibility rules

- `economy.db` and `quests.db` have independent schema versions.
- Version-zero databases are the historical format from the downloaded JARs.
- Migration creates missing tables and adds the historical `username` column;
  it does not delete or rewrite data rows.
- Migrations run in one transaction.
- A database with a version newer than the current mod is refused unchanged.
- A future release must add a new migration instead of editing version-one
  semantics in place.

## Persistent identifiers

These IDs must remain stable:

- shop IDs;
- shop item IDs used by buy/sell limits;
- quest IDs;
- quest NPC IDs;
- board binding coordinates.

Renaming one makes old SQLite rows point to a missing definition. If a future
release needs a rename, add an explicit alias migration before removing the old
ID. The current release intentionally does not guess or rename user data.

## Failure and recovery

### Future database schema

If the log reports an unsupported future schema:

1. Stop the server.
2. Use the newer mod version that created the database, or restore the matching
   backup.
3. Do not delete `user_version` or manually edit SQLite tables.

### Broken JSON

The original file is preserved as `.broken-*`. Inspect or repair that file,
then copy the corrected JSON back under its original name and run `/eco reload`.

### Failed migration

The `.bak` files are created before a legacy upgrade. Stop the server, preserve
the failed database for diagnosis, restore the backup, and retry with the
matching mod version. Never test a downgrade by deleting the only database.

## Architecture links

- Runtime/component diagrams: [`architecture.md`](architecture.md)
- Persistence implementation and file ownership: [`persistence-compatibility.md`](persistence-compatibility.md)
- Operator installation and configuration: [`../CURSEFORGE.md`](../CURSEFORGE.md)
- Contributor verification checklist: [`../CONTRIBUTING.md`](../CONTRIBUTING.md)
