# Skills for AI-Driven Development: Cobblemon Economy

This document defines the competencies and operating standards for AI-driven work on this mod. It emphasizes correctness, reliability, and scalability.

## Core platform skills
- Fabric mod lifecycle (server and client entrypoints) for Minecraft 1.21.1.
- Cobblemon API events and data (Pokedex, battle, capture).
- Sgui server-side GUI workflows.
- Fabric networking payloads (CustomPacketPayload with StreamCodec).
- SQLite persistence with BigDecimal values stored as strings.

## Mod architecture knowledge (how it works)
- Server entrypoint: `src/main/java/com/cobblemon/economy/fabric/CobblemonEconomy.java` registers entity, items, commands, networking, and server lifecycle hooks.
- Client entrypoint: `src/main/java/com/cobblemon/economy/client/CobblemonEconomyClient.java` registers renderer and client networking.
- Per-world data root: `world/config/cobblemon-economy/` contains `config.json`, `economy.db`, `skins/`, and `transactions.log`.
- Economy storage: `src/main/java/com/cobblemon/economy/storage/EconomyManager.java` manages SQLite with pre/post hooks from `src/main/java/com/cobblemon/economy/api/EconomyEvents.java`.
- Config model: `src/main/java/com/cobblemon/economy/storage/EconomyConfig.java` defines shops, linked shops, sell shops, and item definitions with optional `nbt`, `dropTable`, and `lootTable`.
- Shop UI: `src/main/java/com/cobblemon/economy/shop/ShopGui.java` uses Sgui, custom font glyphs in `src/main/resources/assets/cobblemon-economy/font/default.json`, and GUI textures in `src/main/resources/assets/cobblemon-economy/textures/gui/shop/`.
- Rewards: `src/main/java/com/cobblemon/economy/events/CobblemonListeners.java` grants rewards for captures, Pokedex updates, and battle victories with duplicate prevention.
- Skins: `src/main/java/com/cobblemon/economy/networking/NetworkHandler.java` + `src/main/java/com/cobblemon/economy/client/ClientNetworkHandler.java` implement request/response for skin textures with local cache.
- Compatibility: YAWP and Star Academy integrations are optional via `src/main/java/com/cobblemon/economy/compat/CompatHandler.java` and `src/main/java/com/cobblemon/economy/mixin/MixinPlugin.java`.

## Reliability standards
- Preserve per-world isolation; avoid writing economy data outside `world/config/cobblemon-economy/` unless explicitly required.
- Keep currency math in BigDecimal; avoid floating-point arithmetic for balances.
- Avoid blocking IO on hot paths (shop clicks, capture rewards). If new IO is needed, defer or batch.
- Validate config inputs and nulls; keep safe defaults and sanitize unknown items.
- Keep optional integrations isolated to prevent ClassNotFound or missing mod crashes.
- Keep translations in sync with UI and command strings in `src/main/resources/assets/cobblemon-economy/lang/en_us.json`.

## Scalability standards
- Avoid per-tick polling; rely on events and callbacks.
- Avoid repeated full registry scans; cache or resolve once per session where possible.
- Keep GUI pagination bounded; render only the current page.
- Keep network payload size bounded (skin payload is capped at 1MB).
- Keep logging lightweight; if expanding logs, add throttling or rotation rules.

## Common change playbooks
- New reward type: update `EconomyConfig`, implement in `CobblemonListeners`, add translations, and smoke-test in a world.
- New shop capability: update `EconomyConfig.ShopDefinition`, wire behavior in `ShopGui`, and add translations.
- New command: extend `EconomyCommands`, add translations, and verify permissions and suggestions.
- New skin source: extend `NetworkHandler` and `ClientNetworkHandler` with sanitization and caching intact.

## Verification checklist
- Build: `./gradlew build`
- Smoke test: spawn a shopkeeper, open a shop, buy/sell, verify balances and rewards.
- Data check: confirm `economy.db` updates, `config.json` loads, and no errors in logs.
