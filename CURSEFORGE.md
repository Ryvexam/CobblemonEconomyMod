# Cobblemon Economy

**Cobblemon Economy** is a robust, server-side focused economy and shop mod for Minecraft Fabric, designed specifically for Cobblemon servers. It provides a professional, highly customizable shopping experience with dual-currency support and interactive NPCs.

## 🐛 Bug Reports & Support
For any bugs please report here: [https://discord.gg/zxZXcaTHwe](https://discord.gg/zxZXcaTHwe)

---

## ✨ Key Features

### 🛒 Advanced NPC Shop System
*   **Sgui Integration:** Utilizes the Sgui library for high-performance, server-side GUI management.
*   **Dynamic Pagination:** Automatically handles shops with hundreds of items. No more inventory clutter!
*   **Custom Backgrounds:** Supports unique GUI textures (Resource Pack compatible) that change based on navigation (First page, middle, last).
*   **Visual Balance:** Displays the player's head and current balance directly within the shop interface.
*   **Buy & Sell Support:** Configure specific shops where NPCs buy items from players.

### 💰 Dual-Currency Management
*   **PokeDollars (₽):** The standard currency for everyday items.
*   **PokeCoins (PCO):** A secondary premium or battle-reward currency.
*   **SQLite Storage:** All player balances are stored in a reliable, local SQLite database for maximum performance and stability.

### 🛠️ In-Game Configuration Tools
*   **Shopkeepers:** Custom entities that don't despawn and serve as your shop NPCs. They automatically rotate to face you when spawned!
*   **Shop Setter:** A specialized tool to link any NPC to a specific shop ID defined in your config. Works in any language!
*   **Skin Setter:** Apply custom skins to your NPCs. Clients automatically download these skins from the server—no resource pack required!
*   **Tower Tagger:** Tag NPCs for specific roles (like Battle Tower rewards).

### 📜 Transparency & Security
*   **Transaction Logs:** Every purchase or sale is logged in `world/config/cobblemon-economy/transactions.log`.
*   **Unified Storage:** All mod data (config, database, logs, skins) is stored within the world directory (`world/config/cobblemon-economy/`) for complete save portability.
*   **Smart Rewards:** Capture and Pokedex rewards are intelligent—you only get paid for the *first* time you catch a species (unless it's Shiny or Legendary). Set rewards to 0 in config to disable them silently.
*   **Lootboxes:** Create "Black Market" shops that sell crates containing random items from a custom drop table.

---

## 🎮 Commands

### Players
*   `/bal` | `/balance` - Check your PokeDollar balance.
*   `/pco` - Check your PokeCoin (PCO) balance.
*   `/pay <player> <amount>` - Send money to a friend.

### Admins
*   `/eco reload` - Hot-reload the shop configurations.
*   `/eco shop list` - View all registered shop IDs.
*   `/eco shop get <id>` - Obtain a Shop Setter (Auto-completes shop IDs!).
*   `/eco skin <name>` - Obtain a Skin Setter (Auto-completes available skins!).
*   `/eco item` - Obtain the Tower Tagger tool.
*   `/balance <player> <add|remove|set> <amount>` - Direct balance manipulation.
*   `/pco <player> <add|remove|set> <amount>` - Direct PCO manipulation.

---

## 🔧 Installation & Setup

1.  Drop the JAR into your `mods` folder.
2.  Start the server to generate the configuration folder.
3.  Edit `world/config/cobblemon-economy/config.json` to create your custom shops.
4.  Drop custom skin PNGs into `world/config/cobblemon-economy/skins/`.
5.  Use `/eco shop get <id>` to set up your NPCs in-game!

---

## ⚙️ Configuration Guide

The configuration file is located at `world/config/cobblemon-economy/config.json`.

### Global Settings
| Setting | Default | Description |
| :--- | :---: | :--- |
| `startingBalance` | 1000 | Starting money for new players. |
| `battleVictoryReward` | 100 | Money for winning wild battles (0 to disable). |
| `newDiscoveryReward` | 100 | Money for **first-time** capture of a species (0 to disable). |

### Creating Shops
Shops are defined in the `"shops"` object.
*   **title**: GUI Title.
*   **currency**: `"POKE"` or `"PCO"`.
*   **skin**: Filename of the skin in `skins/` folder (e.g. "nurse").
*   **items**: List of items to sell.

#### Item Examples
**1. Standard Item:**
```json
{ "id": "cobblemon:poke_ball", "name": "Poke Ball", "price": 200 }
```

**2. Item with Components (Enchantments, etc.):**
Use standard Minecraft 1.21 component syntax in the ID.
```json
{ "id": "minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]", "name": "Sharpness V Sword", "price": 5000 }
```

**3. Lootbox / Mystery Box:**
Add a `dropTable` list. Buying this item gives one random item from the list!
```json
{
  "id": "minecraft:chest",
  "name": "Mystery Box",
  "price": 500,
  "dropTable": [ "cobblemon:rare_candy", "cobblemon:master_ball" ]
}
```

---

## 🛠️ How to Use Admin Tools

### 1. Setting Up a Shop (Shop Setter)
The **Shop Setter** is a Nether Star item used to link a Shopkeeper NPC to a specific shop ID from your config.
1.  Run `/eco shop list` to see available shop IDs.
2.  Run `/eco shop get <shop_id>` (e.g., `/eco shop get ball_emporium`).
    *   *Tip: Use TAB for auto-completion!*
3.  **Right-Click** any Shopkeeper NPC with the star.
4.  The NPC is now linked to that shop! Players can Right-Click it to open the shop.

### 2. Changing NPC Skins (Skin Setter)
The **Skin Setter** is a Player Head item used to apply custom textures to your Shopkeepers.
1.  Place your `.png` skin files in `world/config/cobblemon-economy/skins/`.
2.  Run `/eco skin <name>` (e.g., `/eco skin nurse` for `nurse.png`).
    *   *Tip: Use TAB to see available skin files.*
3.  **Right-Click** a Shopkeeper NPC with the head.
4.  The skin updates instantly for everyone! (Clients automatically download it).
    *   *Note: Use `/eco skin shopkeeper` to revert to the default look.*

---

*Developed for Cobblemon 1.6+ on Fabric 1.21.1.*

---

**Visit our website:** [ryvexam.fr](https://ryvexam.fr)
