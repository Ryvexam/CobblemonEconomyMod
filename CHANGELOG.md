# Changelog

All notable changes to this project will be documented in this file.

## [0.0.6] - 2026-01-21

### Added
- **Unified Per-World Storage**: All mod data, including the `skins/` folder, is now stored within the world directory (`world/config/cobblemon-economy/`). This ensures complete portability of saves.
- **Battle Reward Multipliers**: Winning battles against **Shiny**, **Legendary**, or **Paradox** pokemon now grants significantly higher PokeDollar rewards (configurable multipliers).
- **Improved Shop Stability**: Added validation logic to skip malformed or null items in the `config.json` instead of crashing.
- **Default General Shop**: Added a `default_poke` shop to the default configuration to match the default shopkeeper state.
- **External Skins Support**: You can now add custom skins by dropping `.png` files into the `skins/` folder.
- **Wildcard Item Support**: You can now use wildcards in the config (e.g., `minecraft:*` or `cobblemon:*`) to offer a random item from that namespace.
- **Dynamic Pricing**: Prices now fluctuate by +/- 25% randomly each time a shop session starts.
- **Improved Default Shops**: Added more comprehensive default shops (`apothecary`, `ball_emporium`, `jeweler`, `battle_rewards`, `berry_gardener`) with specialized items and logical buy/sell configurations.

### Changed
- **Config Path**: Reverted the global config path change and moved all files to `world/config/cobblemon-economy/` for better save organization.

## [0.0.5] - 2026-01-21

### Fixed
- **Ghost Item Bug**: Resolved issue where tools (Shop/Skin Setters) could be used after being consumed in Survival mode by adding a 1-second cooldown and forcing server-side inventory synchronization.
- **Interaction Logic**: Refactored entity interaction to prevent the shop interface from opening simultaneously when using a configuration tool.
- **Tool Validation**: Improved security by strictly checking both item type and custom name before applying modifications to shopkeepers.

## [0.0.4] - 2026-01-21

### Added
- **Internationalization (i18n)**: All mod text is now translatable using language files.
- **Default English Support**: Added `en_us.json` as the default language.
- **French Translation**: Added `fr_fr.json` for French users.
- **Default Content Update**: Default shops and items are now in English for fresh installations.

### Fixed
- **SQLite Driver**: Fixed `java.sql.SQLException: No suitable driver found` by explicitly loading the SQLite JDBC driver and re-including the library in the mod JAR.

## [0.0.3] - 2026-01-20

### Fixed
- **Sneak Interaction**: Explicitly prevented shop opening when the player is sneaking (Shift+Click) to allow for other entity interactions.
- **Texture Standardization**: Moved default shopkeeper texture to the custom skin folder to unify the skinning system.
- **Startup Stability**: Final cleanup of classloading logic to ensure zero crashes on both client and server environments.

## [0.0.2] - 2026-01-19

### Added
- **Sell System**: Shopkeepers can now buy items from players. Toggle via `"isSellShop": true` in the config.
- **Bulk Selling**: Right-click an item in a sell shop to sell an entire stack at once.
- **Dynamic Shop Textures**:
  - Backgrounds now support "Sell Mode" with specific textures (`pokedollar_sell`, `pco_sell`).
  - Added Unicode characters `\uE008` through `\uE00F` for sell UI states.
- **Skin Customization**: 
  - New `/eco skin <name>` command to get a *Skin Setter* item.
  - **Local Skin Support**: The mod now looks for skins in `assets/cobblemon-economy/textures/entity/shopkeeper/<name>.png`.
  - **Standardized Default Skin**: New entities spawn with the "shopkeeper" skin by default, stored in the local skins folder.
- **NPC Customization**: Shopkeepers can now be renamed using standard Minecraft Name Tags.
- **Transaction Logging**: Detailed logging of all purchases and sales (including quantity and total price) in `world/cobblemon-economy/transactions.log`.
- **Advanced Navigation**:
  - "Return to Start" button (Slot 0) on multi-page shops.
  - Improved multi-page click zones (Slots 45-48 and 50-53).
- **Default Content**: Added a default emerald selling shop (`sell_gems`).

### Fixed
- **Startup Crash**: Resolved critical client-side crash by isolating server-only logic (Sgui, Listeners) and removing empty Mixin configurations.
- **Rendering Crash**: Fixed a `NullPointerException` in `GameProfile` constructor during skin loading (NPE fix).
- **Classloading Deadlock**: Added a dedicated client logger to prevent classloading conflicts at startup.
- **Interaction Logic**: Prevented shop opening when the player is sneaking (Shift+Click).

### Changed
- **Branding**: Removed specific branding from logs and documentation.
- **UI Alignment**: Fine-tuned vertical ascent and horizontal offset for pixel-perfect GUI alignment.
- **Security**: Added a 1-second cooldown to the *Tower Tagger* to prevent double-click accidental toggles.
- **Dependency Management**: SQLite is no longer bundled by default to avoid native library conflicts on macOS.

## [0.0.1] - 2026-01-19

### Added
- **Core Economy**: Initial implementation of PokeDollar and PCO currencies.
- **Sgui Integration**: Server-side GUI system for shop interfaces.
- **Shopkeepers**: Initial custom NPC entity.
- **Admin Tools**: Added *Shop Setter* and *Tower Tagger* tools.
- **Storage**: SQLite database for persistent player balances.
- **Multi-page Support**: Automatic pagination for shops with many items.
- **Custom Font**: Integrated bitmap font for high-resolution GUI backgrounds.
