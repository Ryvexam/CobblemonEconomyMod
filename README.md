# Cobblemon Economy

Cobblemon Economy is a server-focused economy and shop system for Cobblemon on Fabric. It provides NPC shops, dual currencies, persistent storage, and reward hooks for captures and battles.

Website: https://ryvexam.fr

## Features
- Dual currencies: PokeDollars and PCO
- NPC shopkeepers with GUI-based shops (buy and sell)
- Per-world config and SQLite storage (config, DB, logs, skins)
- Dynamic quantity selection in shops (middle click)
- Item definitions support component syntax (enchantments, datapack items)
- Loot crates via dropTable or Minecraft loot tables
- Purchase limits per item with optional cooldowns
- Auto-downloaded shopkeeper skins from server
- Transaction logging to file
- Capture, discovery, and battle rewards with multipliers
- Optional integrations: YAWP protection flag and Star Academy grading

## Commands
Player:
- `/bal` or `/balance`
- `/pco`
- `/pay <player> <amount>`

Admin (permission level 2):
- `/eco reload`
- `/eco shop list`
- `/eco shop get <id>`
- `/eco skin <name>`
- `/eco item`
- `/balance <player> <add|remove|set> <amount>`
- `/pco <player> <add|remove|set> <amount>`

## Configuration
Config path: `world/config/cobblemon-economy/config.json`
Milestones path: `world/config/cobblemon-economy/milestone.json`

Global settings:
- `startingBalance`
- `startingPco`
- `battleVictoryReward`
- `captureReward` (defaults to `battleVictoryReward` if missing)
- `newDiscoveryReward`
- `battleVictoryPcoReward`
- `shinyMultiplier`
- `legendaryMultiplier`
- `paradoxMultiplier`

Shop definition fields:
- `title`
- `currency` (`POKE` or `PCO`)
- `skin` (filename without extension)
- `isSellShop` (true to let players sell)
- `linkedShop` (optional)
- `linkedShopIcon` (optional item id)
- `items`

Item definition fields:
- `id`
- `name`
- `price`
- `nbt` (legacy NBT string)
- `dropTable` (array of item ids)
- `lootTable` (minecraft loot table id)
- `buyLimit` (optional)
- `buyCooldownMinutes` (optional, 0 means lifetime limit)

Example shop:
```json
{
  "shops": {
    "my_shop": {
      "title": "My Shop",
      "currency": "POKE",
      "items": [
        { "id": "minecraft:diamond", "name": "Diamond", "price": 1000 },
        { "id": "minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]", "name": "Sharpness V", "price": 5000 },
        { "id": "academy:booster_pack[academy:booster_pack=\"base\"]", "name": "Booster Pack", "price": 100 },
        { "id": "minecraft:chest", "name": "Mystery Box", "price": 500, "dropTable": ["minecraft:diamond", "cobblemon:rare_candy"] },
        { "id": "minecraft:chest", "name": "Dungeon Loot", "price": 1000, "lootTable": "minecraft:chests/simple_dungeon" },
        { "id": "cobblemon:rare_candy", "name": "Rare Candy", "price": 50, "buyLimit": 3, "buyCooldownMinutes": 1200 }
      ]
    }
  }
}
```

## Skins
- Place PNGs in `world/config/cobblemon-economy/skins/`.
- Use `/eco skin <name>` to get a Skin Setter.

## Storage
- Config: `world/config/cobblemon-economy/config.json`
- Milestones: `world/config/cobblemon-economy/milestone.json`
- Database: `world/config/cobblemon-economy/economy.db`
- Transactions: `world/config/cobblemon-economy/transactions.log`
- Skins: `world/config/cobblemon-economy/skins/`

## Integrations
- YAWP: flag `melee-npc-cobeco` controls shopkeeper vulnerability.
- Star Academy: optional grading integration when the `academy` mod is present.

## Build
```bash
./gradlew build
```
Jar output: `build/libs/` (use the remapped jar).

## Support
Discord: https://discord.gg/zxZXcaTHwe
