# Changelog

All notable changes to this project will be documented in this file.

## [0.0.15] - 2026-02-03

### Added
- **Command Execution Items:** New item type that executes commands instead of giving items.
  - Use `type: "command"` in shop item definitions.
  - `command` field supports `%player%` placeholder (replaced with buyer's username).
  - Commands execute with OP permission level 4.
  - `displayItem` configuration for custom visual appearance:
    - `material` - Item ID to display (e.g., "supplementaries:key")
    - `displayname` - Custom name shown in shop GUI
    - `enchantEffect` - Boolean to add enchantment glint
  - Supports all existing features: buy limits, cooldowns, both currencies.
  - Cannot be sold back to shops (virtual items).
- **Sell limits:** Per-item sell limits with optional cooldowns, plus UI display of remaining quota.
- **Raid Dens Integration:** Direct raid reward support through `RaidEvents` with per-player raid win payouts.
- **Cross-Economy Conversion Commands:**
  - `/convertcobbledollars <amount|all>`
  - `/convertimpactor <amount|all>`
  - Configurable conversion rates: `cobbleDollarsToPokedollarsRate`, `impactorToPokedollarsRate`
- **Main Currency Backend Switch:** New optional `main_currency` config (`cobeco`, `cobbledollars`, `impactor`).
  - `cobeco`: Cobblemon Economy balance is authoritative.
  - `cobbledollars`: Cobblemon Economy balance operations route to CobbleDollars.
  - `impactor`: Cobblemon Economy balance operations route to Impactor.
- **Bridge Mixins for External Economy APIs (optional):**
  - CobbleDollars API calls can be redirected to CobEco when `main_currency: cobeco`.
  - Impactor account transactions can be redirected to CobEco when `main_currency: cobeco`.

### Changed
- **Default Shop Format:** All default shop items now explicitly use `type: "item"` for clarity.
- **Config Format:** Generated configs now show `type` field for all items, making the format self-documenting.

### Fixed
- **Duplicate Event Listeners on World Rejoin:** Fixed a critical bug where players could receive multiple rewards when rejoining a world without restarting the game.
  - Event listeners are now properly tracked and only registered once per server session.
  - Added automatic cleanup of listeners when the server stops (world disconnect).
  - Prevents exploit where players could farm unlimited capture/battle rewards by repeatedly leaving and rejoining worlds.

- **Capture Rewards for Shiny/Radiant:** Fixed potential issue where capture multipliers could be null in malformed configs.
  - Added validation to ensure `shinyMultiplier`, `radiantMultiplier`, `legendaryMultiplier`, and `paradoxMultiplier` are never null.
  - Added debug logging to help diagnose capture reward issues.
  - Added null check for species labels to prevent NPE.

- **Fossil Revival Rewards:** Shiny, radiant, legendary, and paradox Pokémon revived from fossils now give rewards!
  - Uses same reward system as captures (base reward × multipliers).
  - Special messages for shiny/radiant/legendary/paradox fossil revivals.
  - Added new translation keys for fossil events in both English and French.

## [0.0.14] - 2026-01-31

### Added
- **Placeholder API integration:** Exposes player balance and PCO placeholders for tablists/scoreboards (multiple namespaces for compatibility).
- **Performance profiling:** Optional timing logs for shop rendering, purchases, and database operations.

## [0.0.13] - 2026-01-28

### Added
- **Purchase limits:** Per-item buy limits with optional cooldowns (minutes) and UI display of remaining quota.
- **Capture reward config:** New `captureReward` setting, defaulting to `battleVictoryReward` if missing.
- **Capture milestones:** Configurable rewards for unique-capture counts.

### Changed
- **Default shops:** Added limited items for Battle Rewards/Black Market and removed emoji titles.
- **Booster default item:** Switched the default booster entry to components-based config and updated docs.
- **Item parsing:** More robust parsing for component-based item IDs (datapack/mod items).
- **Milestones config:** Capture milestones now live in `milestone.json`.

### Fixed
- **Name cleanup:** Clears persisted default shopkeeper names from older versions.

## [0.0.12] - 2026-01-27

### Added
- **Audio Feedback:**
  - Buying and selling items now plays a satisfying "Experience Orb Pickup" sound effect.

### Changed
- **NPC Hardening:**
  - **Invulnerability:** Shopkeepers are now strictly invulnerable to Survival players by default. They remain killable by Creative players and the `/kill` command.
  - **Name Visibility:** Shopkeepers names are now hidden by default and only show when looking at them within 8 blocks.
  - **Permissions:** Using a Name Tag on a Shopkeeper is now restricted to **Operators** (level 2).
- **Internationalization:**
  - Fully translated all remaining hardcoded strings in the Shop GUI.
  - Improved consistency across translations.

## [0.0.11] - 2026-01-26

### Added
- **Top Players Ranking:**
  - Added `/bal top` (or `/balance top`) to display the top 10 richest players in PokeDollars.
  - Added `/pco top` to display the top 10 players in PokeCoins.
- **Star Academy Integration:**
  - Added optional integration with the **Star Academy** mod (id: `academy`).
  - Card grading NPCs now charge players using Cobblemon Economy instead of `numismatic-overhaul` if Star Academy is installed. (Huge thanks to **Rinkuji** for the mixin integration!)
  - Automatically handles transaction failure messages (e.g., if the player is broke).

### Changed
- **Internal Economy Logic:** Refactored `EconomyManager` to utilize the new event system for all balance changes, ensuring consistency across the mod.

## [0.0.10] - 2026-01-24

### Added
- **Lootbox Support:** Added the `dropTable` configuration field. Purchasing an item with a `dropTable` will give the player one random item from the list instead of the item itself.

## [0.0.9] - 2026-01-24

### Added
- **Dynamic Quantity Selection:**
  - **Middle-Click Interaction:** Players can now Middle-Click shop items to rotate the quantity multiplier (1x -> 2x -> 4x ... -> 64x).
  - **Live Price Calculation:** The tooltip dynamically updates to show the total price for the selected quantity.
  - **Audio Feedback:** Rotating quantity now plays a UI click sound.
- **Advanced Item Parsing:**
  - **Component Syntax:** Shop items now support full Minecraft component syntax in their ID field (e.g., `minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]`).
  - **Robust Parser:** Implemented a new reflection-based parsing system that works across both Development and Production environments, solving runtime obfuscation issues.

### Changed
- **Strict Configuration Loading:** The mod no longer automatically re-creates default shops (like `default_poke`, `apothecary`) if they are missing from the configuration file. This allows server admins to permanently delete default shops they don't want.
- **Silent Rewards:** Set reward values to `0` in the config to disable them completely. The "You received 0P" chat message will no longer appear if the reward is zero.
- **Improved Tooltips:** Shop tooltips now clearly indicate "Left: Buy | Middle: x{Quantity}" for better user experience.

### Fixed
- **Internationalization Support for Tools:** The **Shop Setter** (Nether Star) now uses internal NBT data to identify the target shop instead of relying on the item's display name. This fixes the issue where the tool wouldn't work if the game language was not English.
- **Path & Database Stability:** Fixed a `java.sql.SQLException` caused by malformed file paths (`./world/./config`) by normalizing directory path construction.
- **Startup Crash:** Resolved a `ClassNotFoundException` related to `ItemStackArgumentType` by implementing a safe reflection wrapper that handles Yarn, Mojang, and Intermediary mapping names.
- **Path & Database Stability:** Fixed a `java.sql.SQLException` caused by malformed file paths (`./world/./config`) by normalizing directory path construction.
- **Startup Crash:** Resolved a `ClassNotFoundException` related to `ItemStackArgumentType` by implementing a safe reflection wrapper that handles Yarn, Mojang, and Intermediary mapping names.

## [0.0.8] - 2026-01-22

### Added
- **Network Skin Synchronization:** Players no longer need resource packs to see custom shopkeeper skins.
  - **Auto-Download:** The client now automatically downloads custom skins from the server upon encountering them.
  - **Drop & Play:** Admins can simply place `.png` files in the server config folder, and they will sync to all connected players.
- **Black Market & Lootboxes:**
  - **Loot Table Support:** Added the `dropTable` field to shop items, allowing shops to sell "Mystery Boxes" or "Crates" that give a random item from a defined list upon purchase.
  - **New Preset:** Added a `black_market` shop selling a "Suspicious Crate" containing rare items.
- **Command Auto-Completion:**
  - `/eco shop get <id>`: Now suggests all available shop IDs.
  - `/eco skin <name>`: Now suggests all available skin files found in the config folder.

### Fixed
- **Shopkeeper Orientation:** Fixed a visual bug where shopkeepers wouldn't look at the player when spawned. They now instantly rotate their head and body to face the player who placed the spawn egg.
- **Reward System Bug:** Fixed an issue where the "New Discovery" reward was being granted repeatedly for the same Pokémon. It now correctly checks the player's Pokedex and only rewards the *first* capture of a species (unless it is Shiny, Legendary, or Paradox).
- **Config Path Correction:** Resolved an issue where the client and server were looking in different locations for skins. Both now correctly respect the world-specific configuration folder (`saves/<world>/config/cobblemon-economy/skins/` for Singleplayer).

## [0.0.7] - 2026-01-22

### Added
- **Smart Pokedex Rewards:**
  - **Logic:** The system now intelligently checks if a player *already* has the species (or any of its forms) unlocked in their Pokedex before granting the "New Discovery" reward.
  - **Exploit Prevention:** Prevents players from receiving money multiple times by catching different forms (e.g., Alolan vs Normal) or shiny variants of a species they already own.
  - **Trade & Evolution Safety:** Applies the same strict check to Pokémon obtained via trading or evolution.
- **Improved NPC Spawning:**
  - **Auto-Rotation:** Shopkeeper NPCs spawned via Spawn Egg or Command will now automatically rotate their body and head to face the nearest player upon creation.
- **NBT Support for Shop Items**: You can now define custom NBT data for shop items using the `"nbt"` field in `config.json` (String format).
- **Unified Per-World Storage**: All mod data, including the `skins/` folder, is now stored within the world directory at `world/config/cobblemon-economy/`.
- **Cumulative Battle Rewards**: Winning battles against special Pokémon now grants additive rewards (e.g., Shiny + Legendary = 15x reward).
- **Improved Shop Stability**: Added validation logic to skip malformed or null items in the `config.json` instead of crashing.
- **Default General Shop**: Added a `default_poke` shop to the default configuration.

### Changed
- **Config Path**: Moved all files to `world/config/cobblemon-economy/` to follow standard save organization.

### Fixed
- **NPC Persistence:** Hardened the `ShopkeeperEntity` code to override standard mob despawning rules, ensuring shopkeepers *never* disappear naturally, regardless of distance or chunk unloading.

## [0.0.6] - 2026-01-21



### Added
- **External Skins Support**: You can now add custom skins by dropping `.png` files into the `skins/` folder.
- **Wildcard Item Support**: You can now use wildcards in the config (e.g., `minecraft:*` or `cobblemon:*`) to offer a random item.
- **Dynamic Pricing**: Prices now fluctuate by +/- 25% randomly each time a shop session starts.
- **Improved Default Shops**: Added specialized default shops (`apothecary`, `ball_emporium`, etc.).

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
