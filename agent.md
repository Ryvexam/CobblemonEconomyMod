# Cobblemon Economy Project Documentation

This project is a Fabric mod for Minecraft 1.21.1, specifically designed to add a robust, data-driven economy system to Cobblemon servers and single-player worlds.

## Core Features

- **Dual Currency**: Supports "PokeDollars" (₽) and "PCo" (Combat Points).
- **Per-World Storage**: Balances and configurations are isolated within each world's save folder (`saves/WORLD_NAME/cobblemon-economy/`).
- **Dynamic Shop System**: Shops are fully configurable via a `config.json` file in the world folder. Each NPC (Shopkeeper) can be assigned a specific shop ID.
- **Custom UI**: Features a custom `HandledScreen` that integrates with Minecraft's inventory system while using high-quality custom textures (`ultraspace_shop_x.png`).
- **Cobblemon Integration**: Awards PokeDollars for first-time captures (Pokedex updates) and rewards PCo for defeating NPCs tagged with `tour_de_combat`.

## Project Structure

- `src/main/java/com/cobblemon/economy/`:
    - `fabric/CobblemonEconomy.java`: Main entry point, handles networking registration, entity registration, and world-start lifecycle events.
    - `storage/`:
        - `EconomyManager.java`: Manages SQLite persistence for player balances (`economy.db`).
        - `EconomyConfig.java`: Handles the GSON-based configuration for rewards and shop definitions (`config.json`).
    - `commands/EconomyCommands.java`: Implements `/bal`, `/pco`, `/pay`, and admin commands under `/eco` and `/cobeco`.
    - `entity/ShopkeeperEntity.java`: The custom NPC entity that players interact with to open shops.
    - `shop/ShopScreenHandler.java`: Server-side logic for the shop container and inventory slot positioning.
    - `client/`:
        - `CobblemonEconomyClient.java`: Client-side initializer, handles packet reception and screen opening.
        - `ShopScreen.java`: The custom UI rendering logic, aligning items and inventory slots to custom textures.
        - `ShopkeeperRenderer.java`: Handles the 3D rendering of the Shopkeeper NPC using a player model and custom skin.
    - `events/CobblemonListeners.java`: Subscribes to Cobblemon API events for capture and battle rewards.
    - `networking/`: Defines `OpenShopPayload` and `PurchasePayload` for server-client communication.

## Configuration & Data

- **Path**: `world_folder/cobblemon-economy/`
- **`config.json`**:
    - `startingBalance`, `startingPco`: Initial values for new players.
    - `shops`: A map of shop IDs to definitions (title, currency, items list).
- **`economy.db`**: SQLite database with a `balances` table storing UUIDs and balance strings.

## Admin Tools

- **/eco shop get <id>**: Gives a "Shop Setter" item. Right-click a Shopkeeper to assign the specified shop. Consumed on use (unless creative).
- **/eco item**: Gives a "Tower Tagger" item. Right-click any entity to toggle the `tour_de_combat` tag (used for PCo rewards).
- **/eco reload**: Reloads the `config.json` from the current world folder.
- **/cobeco**: Displays the help menu.

## Assets

- Found in `src/main/resources/assets/cobblemon-economy/`.
- Textures for UI are categorized by currency: `textures/gui/shop/pokedollar/` and `textures/gui/shop/pco/`.
- The mod icon is `icon.png` and the NPC skin is `textures/entity/shopkeeper.png`.

## Technical Notes for Future Agents

- **Networking**: Uses Fabric's latest `PayloadTypeRegistry` and `StreamCodec` for 1.21.1.
- **UI Scaling**: The `ShopScreen` is designed for 256x256 textures. Inventory slots are centered using an offset of 48 pixels.
- **SQLite**: Using `sqlite-jdbc`. Ensure the driver is included and shaded in the JAR.
- **Mappings**: Project uses official Mojang mappings.
