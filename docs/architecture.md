# Cobblemon Economy — architecture

This document describes the implementation currently shipped by this
repository. It is intentionally source-oriented: when behavior changes, update
the relevant section together with the code and configuration documentation.

## Runtime target

| Component | Current target |
| --- | --- |
| Loader | Fabric Loader `>=0.16.5` |
| Minecraft | `1.21.1` |
| Cobblemon | `>=1.7.1` / development configuration `1.8.1` |
| Java | 21 for Minecraft runtime tasks |
| Persistence | SQLite JDBC, balances represented as decimal strings |
| Server GUI | Sgui for shops; custom Fabric networking for quest boards and skins |

The authoritative version values are in `gradle.properties` and
`src/main/resources/fabric.mod.json`.

The published Fabric JAR supports both Cobblemon 1.7.x and 1.8.x. Servers must
still use the matching platform dependencies: Cobblemon 1.7.x with its
compatible Loader/API, or Cobblemon 1.8.x with Loader `0.17.2+` and Fabric API
`0.116.6+1.21.1`.

## Package map

| Area | Responsibility | Main entry points |
| --- | --- | --- |
| `fabric` | Fabric common initialization, registries, callbacks, lifecycle | `fabric/CobblemonEconomy.java` |
| `client` | Client entrypoint, shopkeeper renderer, quest board screen, skin cache | `client/CobblemonEconomyClient.java` |
| `storage` | JSON configuration, SQLite economy and quest state | `storage/EconomyConfig.java`, `EconomyManager.java`, `QuestConfig.java`, `QuestManager.java` |
| `commands` | Player and permission-level-2 administration commands | `commands/EconomyCommands.java` |
| `shop` / `entity` | Shopkeeper interaction and Sgui shop transaction flow | `shop/ShopGui.java`, `entity/ShopkeeperEntity.java` |
| `events` | Cobblemon capture, Pokédex, battle, raid and fossil reward hooks | `events/CobblemonListeners.java` |
| `quest` / `questboard` | Quest selection, progress, claims, board state and block bindings | `quest/QuestService.java`, `questboard/QuestBoardService.java` |
| `networking` | Typed C2S/S2C payloads and server-side validation/dispatch | `networking/NetworkHandler.java` |
| `compat` | Optional YAWP, CobbleDollars, Impactor, TAB, Placeholder API and Academy bridges | `compat/CompatHandler.java` |
| `mixin` | Optional integrations selected by Fabric's mixin plugin | `mixin/MixinPlugin.java` |

## Initialization and lifecycle

The server entrypoint is `CobblemonEconomy.onInitialize()`.

1. `CompatHandler.init()` discovers optional mods and registers bridges where
   possible. Optional integration failures are logged and do not intentionally
   make the required Cobblemon path fail.
2. The shopkeeper entity, spawn egg, quest board block/item, creative tab,
   networking payload types, and item-group entries are registered.
3. `SERVER_STARTING` resolves the active world's root path and creates
   `world/config/cobblemon-economy/`. It loads JSON configuration and opens
   `economy.db` and `quests.db`.
4. Cobblemon listeners are registered once for the running server. The
   listener guard is reset at `SERVER_STOPPED`, preventing duplicate callbacks
   after a development server restart.
5. `SERVER_STARTED` registers the TAB integration. Player joins initialize the
   balance lookup and username cache when the economy manager is ready.
6. `/eco reload` reloads the five JSON configuration/binding files without
   replacing the SQLite managers.

The startup and callback wiring lives in
`src/main/java/com/cobblemon/economy/fabric/CobblemonEconomy.java`.

## Per-world data ownership

The active world is the owner of economy and quest data. The root is resolved
from `LevelResource.ROOT`, normalized to an absolute path, then extended with
`config/cobblemon-economy`.

| Path | Owner | Purpose |
| --- | --- | --- |
| `config.json` | `EconomyConfig` | Global currency, rewards, multipliers, profiling, and legacy inline shop fallback |
| `shops.json` | `EconomyConfig` | Buy/sell shop definitions and item entries |
| `milestone.json` | `EconomyConfig` | Unique-capture milestone rewards |
| `quests.json` | `QuestConfig` | Quest definitions and objectives |
| `quest_npcs.json` | `QuestNpcConfig` | Quest NPC/board pools, rotation, and display settings |
| `quest_boards_bindings.json` | `QuestBoardBindings` | Dimension/position → quest NPC board mapping |
| `economy.db` | `EconomyManager` | Balances, purchase/sell limits, capture counts and claimed milestones |
| `quests.db` | `QuestManager` | Active/completed/cooldown quest state and objective progress |
| `transactions.log` | shop/economy transaction code | Human-readable transaction history |
| `skins/*.png` | networking/client skin flow | World-local shopkeeper and quest NPC textures |

If `shops.json` is absent, `EconomyConfig.load()` can read existing shops from
`config.json` and writes the separate shop file on the next configuration
save. Do not manually move a world database between worlds without preserving
the matching configuration and backup.

## Economy and currency flow

`EconomyManager` is the local persistence boundary. It stores decimal values as
strings and reconstructs them as `BigDecimal`; new balance code must not use
floating-point arithmetic.

```text
command / shop / reward / quest claim
                 │
                 ▼
       EconomyManager operation
                 │
       ┌─────────┴─────────┐
       ▼                   ▼
 economy.db          optional bridge
 balances/pco        CobbleDollars/Impactor
       │                   │
       └─────────┬─────────┘
                 ▼
        transaction log + player message
```

The `main_currency` setting selects the authoritative read/write backend:

- `cobeco`: Cobblemon Economy's SQLite balance is authoritative, with bridge
  mirroring when optional integrations are present.
- `cobbledollars`: balance operations use the online player's CobbleDollars
  balance.
- `impactor`: balance operations use the Impactor primary account.

The command surface and permission rules are defined in
`commands/EconomyCommands.java`; shop transaction behavior is defined in
`shop/ShopGui.java` and the backend calls in `storage/EconomyManager.java`.

## Shopkeeper and shop flow

1. A player uses `/eco shop get <id>` to receive a tagged Nether Star.
2. The server-side `UseEntityCallback` validates the tag and resolves the shop
   ID from the loaded configuration. It assigns the shop and optional skin to
   the `ShopkeeperEntity` and consumes the setter for non-creative players.
3. A normal right-click opens `ShopGui`; a quest NPC opens the quest board
   instead. Sneaking preserves the normal entity interaction path.
4. `ShopGui` resolves an item entry into an `ItemStack`. It supports normal
   item IDs, inline component syntax, shorthand components, loot tables,
   drop-table entries, command items, and linked shops.
5. Buy/sell clicks validate the configured currency, balance/inventory, item
   limit and cooldown. A successful operation updates persistence, executes
   configured rewards/commands where applicable, logs the transaction, and
   refreshes the GUI.

One malformed shop entry is converted to a safe barrier display rather than
preventing the whole shop from opening. Configuration details and examples
remain in the root `README.md`.

## Cobblemon rewards

`CobblemonListeners` subscribes to Cobblemon events once per server lifecycle.

- **Pokédex change:** the PRE/POST pair detects a new species and updates the
  unique-capture count and milestone rewards.
- **Capture:** duplicate events are suppressed for a short window; the base
  reward is selected for a first species capture, repeat captures use the
  configured multi reward, and special labels add the configured multipliers.
- **Battle victory:** player winners receive the configured battle or raid
  reward. Battle Tower wins can add PCO, and quest objective progress is
  advanced in the same event flow.
- **Raid Dens:** reflection detects the optional Raid Dens API. When present,
  raid completion is handled by its end event; otherwise the Cobblemon battle
  event provides the fallback path.
- **Fossil revival:** the optional event is subscribed defensively and uses the
  same special-label multiplier family as capture rewards.

Quest progress is updated by `QuestService` from capture, battle, raid and
fossil events. Reward claiming is separate from completion: the player must
claim a completed quest before rewards are granted and the cooldown is stored.

## Quest board and networking

`QuestBoardService` builds a server-authoritative `QuestBoardState` from the
loaded NPC/quest definitions and player state, then sends it through
`OpenQuestBoardPayload`. The client renders it in `QuestBoardScreen`.

Client actions (`ACCEPT`, `CLAIM`, `CANCEL`, `REFRESH`) arrive as
`QuestBoardActionPayload`; `NetworkHandler` resolves the board again on the
server and calls `QuestService`. The server sends a fresh board state after an
action, so the client is not trusted for quest status, rewards, or limits.

Skins use `RequestSkinPayload` and `ProvideSkinPayload`. The server searches
the active world's `skins/` directory first, then the global Fabric config
directory. The payload codec caps a skin at 1 MiB, and the client caches the
decoded texture by sanitized skin name.

## Extension rules

When adding a feature:

1. Keep server authority in the server-side service/manager; validate all
   client payload values against the loaded configuration.
2. Keep persistent data under the active world's
   `config/cobblemon-economy/` directory and document migrations before
   changing table or file formats.
3. Use `BigDecimal` for currencies and prepared SQL statements for database
   values.
4. Isolate optional integrations behind `CompatHandler` or a dedicated
   compatibility class; the required Cobblemon path must not load optional
   classes unconditionally.
5. Add translations in both `en_us.json` and `fr_fr.json` for user-facing
   messages, then update `README.md` when a configuration or command changes.
6. Run the checklist in [`CONTRIBUTING.md`](../CONTRIBUTING.md) before
   publishing a build.

## Source of truth

- Runtime wiring: `src/main/java/com/cobblemon/economy/fabric/CobblemonEconomy.java`
- Economy persistence: `src/main/java/com/cobblemon/economy/storage/EconomyManager.java`
- Configuration schema/defaults: `src/main/java/com/cobblemon/economy/storage/EconomyConfig.java`
- Shop behavior: `src/main/java/com/cobblemon/economy/shop/ShopGui.java`
- Rewards: `src/main/java/com/cobblemon/economy/events/CobblemonListeners.java`
- Quests: `src/main/java/com/cobblemon/economy/quest/QuestService.java`
- Payload registration: `src/main/java/com/cobblemon/economy/networking/NetworkHandler.java`
- Operator-facing examples: [`README.md`](../README.md)
