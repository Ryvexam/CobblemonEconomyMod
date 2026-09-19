# Persistence Compatibility Specification

## Goal

Make Cobblemon Economy safe to upgrade across releases without losing JSON
configuration, economy balances, purchase limits, capture milestones, quest
state, or quest progress.

## Compatibility contract

The implementation must support both directions of the compatibility window:

1. A new mod version reads configurations and SQLite databases created by
   older mod versions.
2. A server administrator can temporarily downgrade to the previous mod
   version without the new version having destructively removed legacy data.

Persistent identifiers are data contracts. Shop IDs, shop item IDs, quest IDs,
and quest NPC IDs must not be renamed silently because they are referenced by
SQLite rows and board assignments.

## Design

### SQLite

`PRAGMA user_version` identifies the schema version for each database. Economy
and quest databases have independent migration chains. Existing version-zero
databases are treated as legacy databases and migrated additively to schema
version 1:

- `economy.db`: ensure balances, purchase/sell limits, capture counts, and
  capture milestone tables; add the historical `balances.username` column.
- `quests.db`: ensure quest state and objective progress tables.

Before upgrading an existing non-empty database, the runner creates a
same-directory `<database>.bak` copy. Migrations run in one SQLite transaction.
If a database reports a version newer than the bundled code understands, startup
fails explicitly instead of running against an incompatible schema.

### JSON

JSON loaders continue to accept all existing names and layouts. A
`configVersion` field identifies files written by the current loader but is not
required when reading old files. `shops.json` remains the preferred source;
legacy inline `config.json.shops` remains readable and is exported when the
external file is missing. The inline representation is retained as a legacy
mirror so a temporary downgrade can still read the shops.

All writes use a temporary file in the same directory followed by an atomic
move, with a `.bak` copy of the previous file. Malformed files are copied to a
timestamped `.broken-*` file before defaults or the legacy fallback are used.

### Runtime lifecycle

Database migration runs before managers expose persistence operations during
`SERVER_STARTING`. `/eco reload` only reloads JSON; it never replaces or resets
SQLite managers. A restart is required for a database schema migration.

## Non-goals

- No automatic renaming of user-defined persistent IDs.
- No deletion of old database tables or configuration keys.
- No migration of external economy backends such as Impactor or CobbleDollars.

## Verification

- Legacy economy database with balances survives migration byte-for-byte at the
  row level and gains missing current tables/columns.
- Legacy quest database preserves state and progress rows.
- A backup is created before an existing database is upgraded.
- A future database version is rejected without modification.
- Old inline shops and old JSON aliases still load.
- Atomic config writes leave the target valid and create a backup.
- Full Gradle tests/build and a dedicated Fabric/Cobblemon server smoke test
  pass.
