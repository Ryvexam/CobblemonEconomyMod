# Cobblemon Economy AI Agent Guide

This guide provides a concise operational model of how the mod works and how to make reliable, scalable changes.

## Purpose and scope
- Maintain a stable Cobblemon economy system for Fabric 1.21.1.
- Keep per-world data isolated and compatible across updates.
- Prefer additive, backward-compatible changes unless a migration is explicit.

## Architecture map
- Server entrypoint: `src/main/java/com/cobblemon/economy/fabric/CobblemonEconomy.java`
- Client entrypoint: `src/main/java/com/cobblemon/economy/client/CobblemonEconomyClient.java`
- Economy storage: `src/main/java/com/cobblemon/economy/storage/EconomyManager.java`
- Config model: `src/main/java/com/cobblemon/economy/storage/EconomyConfig.java`
- Commands: `src/main/java/com/cobblemon/economy/commands/EconomyCommands.java`
- Shop UI: `src/main/java/com/cobblemon/economy/shop/ShopGui.java`
- Entity: `src/main/java/com/cobblemon/economy/entity/ShopkeeperEntity.java`
- Rewards: `src/main/java/com/cobblemon/economy/events/CobblemonListeners.java`
- Networking: `src/main/java/com/cobblemon/economy/networking/NetworkHandler.java`
- Client skins: `src/main/java/com/cobblemon/economy/client/ClientNetworkHandler.java`
- Renderer: `src/main/java/com/cobblemon/economy/client/ShopkeeperRenderer.java`
- Optional compat: `src/main/java/com/cobblemon/economy/compat/CompatHandler.java`, `src/main/java/com/cobblemon/economy/mixin/MixinPlugin.java`
- Assets: `src/main/resources/assets/cobblemon-economy/`

## Runtime flow (how it works)
1. Server start initializes per-world folder `world/config/cobblemon-economy/`, loads `config.json`, and opens `economy.db`.
2. Shopkeeper entity and spawn egg are registered, plus commands and networking.
3. Player interacts with shopkeeper:
   - Shop Setter (Nether Star with custom data) assigns shop ID and optional skin.
   - Skin Setter (Player Head with custom name) updates skin.
   - Tower Tagger (Blaze Rod) toggles the `tour_de_combat` tag.
   - Otherwise, the shop GUI opens.
4. Shop GUI resolves items, handles buy/sell, updates balances in SQLite, and appends `transactions.log`.
5. Cobblemon events grant rewards for captures, Pokedex progress, and battle victories.
6. Client requests skins from server if not cached; server responds with PNG bytes and client registers a dynamic texture.

## Data locations
- Config and data root: `world/config/cobblemon-economy/`
- Config file: `world/config/cobblemon-economy/config.json`
- SQLite: `world/config/cobblemon-economy/economy.db`
- Skins: `world/config/cobblemon-economy/skins/`
- Transactions log: `world/config/cobblemon-economy/transactions.log`

## Config schema essentials
- Global values: `startingBalance`, `startingPco`, `battleVictoryReward`, `newDiscoveryReward`, `battleVictoryPcoReward`, `shinyMultiplier`, `legendaryMultiplier`, `paradoxMultiplier`.
- Shop entry: `title`, `currency` (POKE or PCO), `skin`, `isSellShop`, `linkedShop`, `linkedShopIcon`, `items`.
- Item entry: `id`, `name`, `price`, optional `nbt`, optional `dropTable`, optional `lootTable`.

## UI and assets
- GUI backgrounds are custom font glyphs mapped in `src/main/resources/assets/cobblemon-economy/font/default.json`.
- Texture atlases are in `src/main/resources/assets/cobblemon-economy/textures/gui/shop/`.
- The shop title layout uses negative spacing glyphs such as `\uF804` and background glyphs `\uE000` to `\uE00F`; keep these consistent if modifying the GUI.

## Networking and skins
- `RequestSkinPayload` (C2S) and `ProvideSkinPayload` (S2C) are registered in `NetworkHandler`.
- Server searches for skins in world config first, then global config; payload size is capped at 1MB.
- Client caches and reuses textures by sanitized skin name.

## Compatibility rules
- YAWP flag `melee-npc-cobeco` controls shopkeeper vulnerability.
- Star Academy integration is optional and loaded via mixin plugin when mod `academy` is present.

## Reliability and scalability guardrails
- Do not break per-world isolation or data formats without a migration plan.
- Avoid blocking IO or large computations in event handlers and GUI callbacks.
- Keep BigDecimal storage in string form to preserve precision.
- Keep translations updated for all user-facing text in `src/main/resources/assets/cobblemon-economy/lang/en_us.json`.
- Keep optional integrations isolated to avoid hard dependencies.

## Development commands
- Build: `./gradlew build`
- Run client/server: `./gradlew runClient` / `./gradlew runServer`

## Change checklist
- Update config model and defaults if you add new features.
- Update translations and user docs when behavior changes.
- Verify shop opening, buy/sell flow, and reward events in a test world.
