# Cobblemon Economy

Cobblemon Economy is a server-focused economy and shop system for Cobblemon on Fabric. It adds NPC shops, dual currencies, rewards, and a flexible JSON configuration.

## Highlights
- Dual currencies: PokeDollars and PCO
- NPC shopkeepers with GUI buy/sell shops
- Item definitions support component syntax (enchantments, datapack items)
- Loot crates via dropTable or Minecraft loot tables
- **Command execution items** - sell commands instead of items (e.g., crate keys, buffs, shoutouts)
- Purchase limits per item with optional cooldowns
- Auto-downloaded shopkeeper skins from server
- Per-world config and SQLite storage
- Capture, discovery, and battle rewards with multipliers
- Fossil revival rewards for shiny/radiant/legendary/paradox Pokémon
- Optional YAWP, Star Academy, and Placeholder API integrations

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
- `/convertcobbledollars <amount|all>`
- `/convertimpactor <amount|all>`

Admin:
- `/eco reload`
- `/eco shop list`
- `/eco shop get <id>`
- `/eco skin <name>`
- `/eco item`
- `/balance <player> <add|remove|set> <amount>`
- `/pco <player> <add|remove|set> <amount>`

## Placeholders (Placeholder API)
If `placeholder-api` is installed, Cobblemon Economy exposes balance placeholders for tablists/scoreboards.
Use the placeholder format required by your tablist plugin (often `%namespace:placeholder%` or `{namespace:placeholder}`).

Available placeholders (recommended namespace: `cobeco`):
- `cobeco:balance`
- `cobeco:balance_symbol`
- `cobeco:pco`
- `cobeco:pco_symbol`

Alternate namespaces also registered for compatibility:
- `cobblemon_economy:*`
- `cobblemon-economy:*`

## Config overview
Global settings:
- `main_currency` (`cobeco`, `cobbledollars`, `impactor`; default `cobeco`)
- `startingBalance`, `startingPco`
- `battleVictoryReward`, `raidDenVictoryReward`, `captureReward`, `newDiscoveryReward`, `battleVictoryPcoReward`
- `cobbleDollarsToPokedollarsRate`, `impactorToPokedollarsRate`
- `shinyMultiplier`, `legendaryMultiplier`, `paradoxMultiplier`
- `enableProfiling`, `profilingThresholdMs`

Milestones file:
- `world/config/cobblemon-economy/milestone.json`

Milestone rules:
- File missing: defaults are generated.
- Empty file: defaults are used and saved.
- Keys are unique-capture counts (strings), values are rewards in PokeDollars.

Shop fields:
- `title`, `currency`, `skin`, `isSellShop`, `linkedShop`, `linkedShopIcon`, `items`

Item fields:
- `type` - `"item"` (default) or `"command"` for command execution
- `id`, `name`, `price`, `nbt`, `dropTable`, `lootTable`, `components`, `buyLimit`, `buyCooldownMinutes`, `sellLimit`, `sellCooldownMinutes`
- `command` - Command to execute for `type: "command"` (use `%player%` placeholder)
- `displayItem` - Custom display for command items (`material`, `displayname`, `enchantEffect`)

Item limit rules:
- Missing `buyLimit` or `buyLimit <= 0`: unlimited.
- `buyLimit > 0` and missing `buyCooldownMinutes`: lifetime limit.
- `buyLimit > 0` and `buyCooldownMinutes = 0`: lifetime limit.
- `buyLimit > 0` and `buyCooldownMinutes > 0`: limit resets every N minutes.
- Missing `sellLimit` or `sellLimit <= 0`: unlimited.
- `sellLimit > 0` and missing `sellCooldownMinutes`: lifetime limit.
- `sellLimit > 0` and `sellCooldownMinutes = 0`: lifetime limit.
- `sellLimit > 0` and `sellCooldownMinutes > 0`: limit resets every N minutes.

## Example item entries
```json
{ "id": "minecraft:diamond", "name": "Diamond", "price": 1000 }
{ "id": "minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]", "name": "Sharpness V", "price": 5000 }
{ "id": "academy:booster_pack[academy:booster_pack=\"base\"]", "name": "Booster Pack", "price": 100 }
{ "id": "minecraft:chest", "name": "Mystery Box", "price": 500, "dropTable": ["minecraft:diamond", "cobblemon:rare_candy"] }
{ "id": "minecraft:chest", "name": "Dungeon Loot", "price": 1000, "lootTable": "minecraft:chests/simple_dungeon" }
{ "id": "cobblemon:rare_candy", "name": "Rare Candy", "price": 50, "buyLimit": 3, "buyCooldownMinutes": 1200 }
```

Item limit only:
```json
{ "id": "cobblemon:rare_candy", "name": "Rare Candy", "price": 50, "buyLimit": 3, "buyCooldownMinutes": 1200 }
```

Item with components (booster example):
```json
{
  "id": "academy:booster_pack",
  "name": "Oui",
  "price": 200,
  "components": {
    "academy:booster_pack": "\"base\""
  }
}
```

**Command execution item** (executes command on purchase):
```json
{
  "type": "command",
  "command": "crate key give vote 1 %player%",
  "price": 100,
  "buyLimit": 1,
  "buyCooldownMinutes": 1440,
  "displayItem": {
    "material": "supplementaries:key",
    "displayname": "Vote Crate Key",
    "enchantEffect": true
  }
}
```

Capture milestones example (`milestone.json`):
```json
{ "10": 300, "50": 700, "100": 1500, "200": 3000, "300": 6000 }
```

## Support
Discord: https://discord.gg/zxZXcaTHwe

## Integrations
- Raid Dens: raid win rewards via `RaidEvents.RAID_END`.
- CobbleDollars: optional conversion command and backend bridge.
- Impactor: optional conversion command and backend bridge.

Currency backend behavior (`main_currency`):
- `cobeco`: Cobblemon Economy is authoritative and bridges CobbleDollars/Impactor transactions back into CobEco.
- `cobbledollars`: CobEco balance operations route to CobbleDollars balance.
- `impactor`: CobEco balance operations route to Impactor primary currency.
