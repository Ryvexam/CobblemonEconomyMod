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
      { "id": "cobblemon:poke_ball", "name": "Poke Ball", "price": 200 }
    ]
  }
}
```

---

## 📄 License

This project is private and intended for use with the Cobblemon mod.
