# Changelog

All notable changes to this project will be documented in this file.

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
