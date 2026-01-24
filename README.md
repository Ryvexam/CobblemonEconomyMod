# Cobblemon Economy

A professional economy system for Cobblemon on Fabric, featuring a custom GUI-based shop system, dual-currency management, and interactive shopkeepers.

Visit our website: [ryvexam.fr](https://ryvexam.fr)

## 🚀 Features

*   **Dual Currency System:** Supports both **PokeDollars** (₽) and **PCO** (PokeCoins).
*   **Persistent Storage:** Data is stored locally using a robust SQLite database.
*   **Custom Shopkeepers:** Specialized entities that can be assigned unique shops via in-game tools.
*   **Advanced GUI:** 
    *   Powered by **Sgui** for server-side stability.
    *   Dynamic pagination with custom background textures.
    *   Integrated Player Balance display (using player heads).
*   **In-Game Tools:**
    *   **Shop Setter:** Link a shopkeeper to a specific shop ID.
    *   **Tower Tagger:** Toggle specific tags on entities for specialized behavior.
    *   **Skin Setter:** Apply a custom skin to a shopkeeper NPC.
    *   **External Skins:** Drop `.png` files into the world's `cobblemon-economy/skins/` folder to use them.
    *   Default skin is `shopkeeper.png`.
    *   If a local file is not found, the mod attempts to load the player skin by name (client-side cache).

---

## 🛠 Commands

### Player Commands
*   `/bal` or `/balance` - View your PokeDollar balance.
*   `/pco` - View your PokeCoin balance.
*   `/pay <player> <amount>` - Transfer PokeDollars to another player.

### Admin Commands (Permission Level 2)
*   `/eco reload` - Reload the JSON configuration.
*   `/eco shop list` - List all configured shop IDs.
*   `/eco shop get <id>` - Get a *Shop Setter* item for a specific shop.
*   `/eco skin <username>` - Get a *Skin Setter* item to change a shopkeeper's skin.
*   `/eco item` - Give yourself a *Tower Tagger*.
*   `/balance <player> <add/remove/set> <amount>` - Manage player balances.
*   `/pco <player> <add/remove/set> <amount>` - Manage player PCO.

---

## 🏗 Build Instructions

### Prerequisites
*   **Java 21** or higher.
*   **Git**.

### Compilation
To build the mod and generate the JAR file, run:

```bash
# Set Java 21 environment (macOS Homebrew example)
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

# Build the project
./gradlew build
```

The resulting JAR file will be located in: `build/libs/cobblemon-economy-0.0.1.jar`.

---

## ⚙️ Configuration

The configuration is located in your world folder: `world/config/cobblemon-economy/config.json`.

Example shop structure:
```json
"shops": {
  "my_custom_shop": {
    "title": "My Shop Name",
    "currency": "POKE",
    "items": [
      { "id": "minecraft:diamond", "name": "Diamond", "price": 1000 },
      { "id": "minecraft:netherite_sword", "name": "Excalibur", "price": 5000, "nbt": "{Unbreakable:1b,Enchantments:[{id:'minecraft:sharpness',lvl:10s}]}" },
      { "id": "cobblemon:poke_ball", "name": "Poke Ball", "price": 200 }
    ]
  }
}
```

---

## Loot Crates and Random Items

The shop system supports two methods for creating loot crates that give random items when purchased:

### Method 1: Simple Drop Table (dropTable)

A simple list of item IDs where one random item is selected per purchase:

```json
{
  "id": "minecraft:chest",
  "name": "Mystery Box",
  "price": 500,
  "dropTable": [
    "minecraft:diamond",
    "minecraft:emerald",
    "minecraft:gold_ingot",
    "cobblemon:rare_candy",
    "cobblemon:master_ball"
  ]
}
```

### Method 2: Minecraft Loot Tables (lootTable)

Use Minecraft's native loot table system for advanced randomization with weights, conditions, and multiple items per roll.

#### Using Built-in Loot Tables

Reference any vanilla Minecraft loot table:

```json
{
  "id": "minecraft:chest",
  "name": "Dungeon Loot",
  "price": 1000,
  "lootTable": "minecraft:chests/simple_dungeon"
}
```

Common vanilla loot tables:
- `minecraft:chests/simple_dungeon`
- `minecraft:chests/desert_pyramid`
- `minecraft:chests/end_city_treasure`
- `minecraft:chests/bastion_treasure`
- `minecraft:chests/ancient_city`

#### Creating Custom Loot Tables

To create custom loot tables, you need to set up a **datapack** on your server.

**Folder Structure:**
```
your_server/
  world/
    datapacks/
      my_custom_loot/                    <- Your datapack folder
        pack.mcmeta                      <- Required datapack metadata
        data/
          mycrates/                      <- Your namespace (can be any name)
            loot_table/
              crates/                    <- Subfolder for organization
                starter_crate.json       <- Your loot table file
                premium_crate.json
                legendary_crate.json
```

**Step 1: Create `pack.mcmeta`**

Create the file `world/datapacks/my_custom_loot/pack.mcmeta`:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "Custom loot tables for Cobblemon Economy"
  }
}
```

> Note: `pack_format: 48` is for Minecraft 1.21.x. Adjust if using a different version.

**Step 2: Create Your Loot Table**

Create the file `world/datapacks/my_custom_loot/data/mycrates/loot_table/crates/starter_crate.json`:

```json
{
  "type": "minecraft:gift",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "cobblemon:poke_ball",
          "weight": 50,
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": { "min": 5, "max": 10 }
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "cobblemon:great_ball",
          "weight": 30,
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": { "min": 2, "max": 5 }
            }
          ]
        },
        {
          "type": "minecraft:item",
          "name": "cobblemon:ultra_ball",
          "weight": 15
        },
        {
          "type": "minecraft:item",
          "name": "cobblemon:master_ball",
          "weight": 5
        }
      ]
    },
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "cobblemon:potion",
          "weight": 60
        },
        {
          "type": "minecraft:item",
          "name": "cobblemon:rare_candy",
          "weight": 40
        }
      ]
    }
  ]
}
```

**Step 3: Reference in Shop Config**

In your `config.json`, reference the loot table using the namespace and path:

```json
{
  "id": "minecraft:chest",
  "name": "Starter Crate",
  "price": 500,
  "lootTable": "mycrates:crates/starter_crate"
}
```

The format is: `namespace:path/to/loot_table` (without `.json` extension)

**Step 4: Reload the Datapack**

After creating or modifying loot tables, reload with:
```
/reload
```

#### Loot Table Tips

- **Weights**: Higher weight = more likely to be selected
- **Rolls**: Number of times to pick from the pool (can give multiple items)
- **Functions**: Modify items (set count, add enchantments, set NBT, etc.)
- **Conditions**: Add requirements (luck-based, player conditions, etc.)

Example with enchantments:
```json
{
  "type": "minecraft:item",
  "name": "minecraft:diamond_sword",
  "weight": 5,
  "functions": [
    {
      "function": "minecraft:enchant_randomly",
      "options": ["minecraft:sharpness", "minecraft:fire_aspect", "minecraft:looting"]
    },
    {
      "function": "minecraft:set_name",
      "name": {"text": "Lucky Blade", "color": "gold", "italic": false}
    }
  ]
}
```

---

## 📄 License

This project is private and intended for use with the Cobblemon mod.
