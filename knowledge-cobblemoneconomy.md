# Cobblemon Economy Knowledge Base

## Project Overview
**Name:** Cobblemon Economy
**Type:** Minecraft Fabric Mod
**Purpose:** Adds a professional economy system to Cobblemon servers, featuring custom NPC shops, dual currency (PokeDollars & PokeCoins), and a GUI-based shopping experience.
**Version:** 0.0.10 (Targeting Minecraft 1.21.1)
**Dependencies:** 
- Fabric Loader
- Fabric API
- Cobblemon (1.7.1+)
- Sgui (included/required for GUIs)
- Java 21

## Installation
1.  **Download:** Get the JAR file.
2.  **Install:** Place the JAR in the server's `mods` folder.
3.  **First Run:** Start the server once to generate the configuration files.
4.  **Config Location:** `world/config/cobblemon-economy/`

## Configuration
**Main Config File:** `world/config/cobblemon-economy/config.json`

### Global Settings
- `startingBalance`: Amount given to new players (Default: 1000).
- `battleVictoryReward`: Money earned from wild battles.
- `newDiscoveryReward`: Money for first-time species capture.
- `shinyMultiplier` / `legendaryMultiplier`: Bonus multipliers for specific captures.

### Shop Configuration (`shops` object)
Each shop needs a unique ID (e.g., `my_shop`).
- `title`: Display name in the GUI.
- `currency`: `"POKE"` (PokeDollars) or `"PCO"` (PokeCoins).
- `skin`: Filename of the NPC skin (e.g., "nurse" for `nurse.png`).
- `isSellShop`: `true` if the shop buys items from players.
- `items`: Array of item objects.

#### Item Examples
1.  **Simple Item:** `{ "id": "cobblemon:poke_ball", "name": "Poke Ball", "price": 200 }`
2.  **With NBT/Components:** `{ "id": "minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]", ... }`
3.  **Loot Crate (Simple):** `{ "id": "minecraft:chest", "price": 500, "dropTable": ["item1", "item2"] }`
4.  **Loot Crate (Advanced):** `{ "id": "minecraft:chest", "price": 1000, "lootTable": "namespace:path/to/table" }`

## Commands
### Player
- `/bal` or `/balance`: Check PokeDollar balance.
- `/pco`: Check PokeCoin balance.
- `/pay <player> <amount>`: Transfer money.

### Admin (Permission Level 2)
- `/eco reload`: Reload config.json.
- `/eco shop list`: Show available shop IDs.
- `/eco shop get <id>`: Get a **Shop Setter** tool (Right-click NPC to link shop).
- `/eco skin <name>`: Get a **Skin Setter** tool (Right-click NPC to apply skin).
- `/eco item`: Get a **Tower Tagger** tool.
- `/balance <player> <add/remove/set> <amount>`: Modify balance.
- `/pco <player> <add/remove/set> <amount>`: Modify PCO.

## Common Issues & Solutions

### 1. "My Loot Table isn't working"
**Cause:** Incorrect path or missing Datapack.
**Solution:** 
- Loot tables require a **Datapack**, not just a config entry.
- Path format in config: `namespace:path/to/table` (e.g., `mycrates:crates/starter`).
- File location: `world/datapacks/PACK_NAME/data/namespace/loot_table/path/to/table.json`.
- Ensure `pack.mcmeta` exists in the datapack.
- Run `/reload` after changing datapack files.

### 2. "Skins aren't loading"
**Cause:** Missing file or client cache.
**Solution:** 
- Place `.png` files in `world/config/cobblemon-economy/skins/`.
- Use `/eco skin <filename>` (without .png) to apply.
- If it doesn't update, try `/eco skin shopkeeper` to reset, then re-apply.
- Clients auto-download skins; no resource pack needed.

### 3. "NPCs aren't spawning" or "Shop won't open"
- Ensure the `Shop Setter` was used to link the NPC to a valid Shop ID.
- Check server logs for errors during `/eco reload`.

### 4. Build Errors
- Ensure **Java 21** is used (`java -version`).
- Run `./gradlew build`.
