# Cobblemon Economy

Cobblemon Economy is a server-focused economy and shop system for Cobblemon on Fabric. It adds NPC shops, dual currencies, rewards, and a flexible JSON configuration.

## Highlights
- Dual currencies: PokeDollars and PCO
- NPC shopkeepers with GUI buy/sell shops
- Item definitions support component syntax (enchantments, datapack items)
- Loot crates via dropTable or Minecraft loot tables
- Purchase limits per item with optional cooldowns
- Auto-downloaded shopkeeper skins from server
- Per-world config and SQLite storage
- Capture, discovery, and battle rewards with multipliers
- Optional YAWP and Star Academy integrations
- Client/server version enforcement

## Setup
1. Drop the jar into `mods/`.
2. Start the server to generate `world/config/cobblemon-economy/config.json`.
3. Edit shops and items, then run `/eco reload`.
4. Place skin PNGs in `world/config/cobblemon-economy/skins/`.

## Commands
Player:
- `/bal` or `/balance`
- `/pco`
- `/pay <player> <amount>`

Admin:
- `/eco reload`
- `/eco shop list`
- `/eco shop get <id>`
- `/eco skin <name>`
- `/eco item`
- `/balance <player> <add|remove|set> <amount>`
- `/pco <player> <add|remove|set> <amount>`

## Config overview
Global settings:
- `startingBalance`, `startingPco`
- `battleVictoryReward`, `captureReward`, `newDiscoveryReward`, `battleVictoryPcoReward`
- `shinyMultiplier`, `legendaryMultiplier`, `paradoxMultiplier`

Shop fields:
- `title`, `currency`, `skin`, `isSellShop`, `linkedShop`, `linkedShopIcon`, `items`

Item fields:
- `id`, `name`, `price`, `nbt`, `dropTable`, `lootTable`, `buyLimit`, `buyCooldownMinutes`

## Example item entries
```json
{ "id": "minecraft:diamond", "name": "Diamond", "price": 1000 }
{ "id": "minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]", "name": "Sharpness V", "price": 5000 }
{ "id": "academy:booster_pack[academy:booster_pack=\"base\"]", "name": "Booster Pack", "price": 100 }
{ "id": "minecraft:chest", "name": "Mystery Box", "price": 500, "dropTable": ["minecraft:diamond", "cobblemon:rare_candy"] }
{ "id": "minecraft:chest", "name": "Dungeon Loot", "price": 1000, "lootTable": "minecraft:chests/simple_dungeon" }
{ "id": "cobblemon:rare_candy", "name": "Rare Candy", "price": 50, "buyLimit": 3, "buyCooldownMinutes": 1200 }
```

## Support
Discord: https://discord.gg/zxZXcaTHwe
