# Cobblemon Economy

**Cobblemon Economy** is a robust, server-side focused economy and shop mod for Minecraft Fabric, designed specifically for Cobblemon servers. It provides a professional, highly customizable shopping experience with dual-currency support and interactive NPCs.

---

## ✨ Key Features

### 🛒 Advanced NPC Shop System
*   **Sgui Integration:** Utilizes the Sgui library for high-performance, server-side GUI management.
*   **Dynamic Pagination:** Automatically handles shops with hundreds of items. No more inventory clutter!
*   **Custom Backgrounds:** Supports unique GUI textures (Resource Pack compatible) that change based on navigation (First page, middle, last).
*   **Visual Balance:** Displays the player's head and current balance directly within the shop interface.

### 💰 Dual-Currency Management
*   **PokeDollars (₽):** The standard currency for everyday items.
*   **PokeCoins (PCO):** A secondary premium or battle-reward currency.
*   **SQLite Storage:** All player balances are stored in a reliable, local SQLite database for maximum performance and stability.

### 🛠️ In-Game Configuration Tools
*   **Shopkeepers:** Custom entities that don't despawn and serve as your shop NPCs.
*   **Shop Setter:** A specialized tool to link any NPC to a specific shop ID defined in your config.
*   **Tower Tagger:** Tag NPCs for specific roles (like Battle Tower rewards).

### 📜 Transparency & Security
*   **Transaction Logs:** Every single purchase is logged in `world/cobblemon-economy/transactions.log` with timestamps, player UUIDs, and price details.
*   **Cooldown Protection:** Built-in preventions for "double-click" exploits on tools and UI elements.

---

## 🎮 Commands

### Players
*   `/bal` | `/balance` - Check your PokeDollar balance.
*   `/pco` - Check your PokeCoin (PCO) balance.
*   `/pay <player> <amount>` - Send money to a friend.

### Admins
*   `/eco reload` - Hot-reload the shop configurations.
*   `/eco shop list` - View all registered shop IDs.
*   `/eco shop get <id>` - Obtain a Shop Setter for a specific shop.
*   `/eco item` - Obtain the Tower Tagger tool.
*   `/balance <player> <add|remove|set> <amount>` - Direct balance manipulation.
*   `/pco <player> <add|remove|set> <amount>` - Direct PCO manipulation.

---

## 🔧 Installation & Setup

1.  Drop the JAR into your `mods` folder.
2.  Start the server to generate the default configuration.
3.  Edit `world/cobblemon-economy/config.json` to create your custom shops.
4.  Use `/eco shop get <id>` to set up your NPCs in-game!

---

**Visit our website:** [ryvexam.fr](https://ryvexam.fr)

*Developed for Cobblemon 1.5.2+ on Fabric 1.21.1.*
