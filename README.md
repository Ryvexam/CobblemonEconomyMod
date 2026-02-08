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
- **Command execution items** - sell commands instead of items (e.g., crate keys, effects, shoutouts)
- Purchase limits per item with optional cooldowns
- Auto-downloaded shopkeeper skins from server
- Transaction logging to file
- Capture, discovery, and battle rewards with multipliers
- Raid den battle rewards (separate configurable PokeDollar amount)
- Fossil revival rewards for shiny/radiant/legendary/paradox Pokémon
- Optional integrations: YAWP protection flag and Star Academy grading
- Optional CobbleDollars conversion command to import another economy balance
- Optional Impactor economy conversion command

## Commands
Player:
- `/bal` or `/balance`
- `/pco`
- `/pay <player> <amount>`
- `/convertcobbledollars <amount|all>` (if CobbleDollars is installed)
- `/convertimpactor <amount|all>` (if Impactor is installed)

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

Milestone rules:
- File missing: defaults are generated.
- Empty file: defaults are used and saved.
- Keys are unique-capture counts (strings), values are rewards in PokeDollars.

Global settings:
- `main_currency` (`cobeco`, `cobbledollars`, `impactor`; default `cobeco`)
- `startingBalance`
- `startingPco`
- `battleVictoryReward`
- `raidDenVictoryReward` (defaults to `battleVictoryReward` if missing)
- `cobbleDollarsToPokedollarsRate` (used by `/convertcobbledollars`, defaults to `1`)
- `impactorToPokedollarsRate` (used by `/convertimpactor`, defaults to `1`)
- `captureReward` (defaults to `battleVictoryReward` if missing)
- `newDiscoveryReward`
- `battleVictoryPcoReward`
- `shinyMultiplier`
- `legendaryMultiplier`
- `paradoxMultiplier`
- `enableProfiling` (logs slow operations when true)
- `profilingThresholdMs` (minimum ms to log)

Shop definition fields:
- `title`
- `currency` (`POKE` or `PCO`)
- `skin` (filename without extension)
- `isSellShop` (true to let players sell)
- `linkedShop` (optional)
- `linkedShopIcon` (optional item id)
- `items`

Item definition fields:
- `type` - `"item"` (default) or `"command"`
- `id` - Item ID (for `type: "item"`)
- `name` - Display name
- `price`
- `nbt` (legacy NBT string)
- `dropTable` (array of item ids)
- `lootTable` (minecraft loot table id)
- `components` (data components for items)
- `buyLimit` (optional)
- `buyCooldownMinutes` (optional, 0 means lifetime limit)
- `sellLimit` (optional)
- `sellCooldownMinutes` (optional, 0 means lifetime limit)
- `command` - Command string for `type: "command"` (use `%player%` placeholder)
- `displayItem` - Custom display configuration for command items:
  - `material` - Item ID to display
  - `displayname` - Custom name shown in shop
  - `enchantEffect` - Boolean for enchantment glint

Item limit rules:
- Missing `buyLimit` or `buyLimit <= 0`: unlimited.
- `buyLimit > 0` and missing `buyCooldownMinutes`: lifetime limit.
- `buyLimit > 0` and `buyCooldownMinutes = 0`: lifetime limit.
- `buyLimit > 0` and `buyCooldownMinutes > 0`: limit resets every N minutes.
- Missing `sellLimit` or `sellLimit <= 0`: unlimited.
- `sellLimit > 0` and missing `sellCooldownMinutes`: lifetime limit.
- `sellLimit > 0` and `sellCooldownMinutes = 0`: lifetime limit.
- `sellLimit > 0` and `sellCooldownMinutes > 0`: limit resets every N minutes.

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

**Command execution item** (sells a command instead of an item):
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

**Mixed shop example** (items + loot tables + commands):
```json
{
  "shops": {
    "mixed_shop": {
      "title": "MIXED SHOP",
      "currency": "POKE",
      "items": [
        { "id": "cobblemon:poke_ball", "name": "Poké Ball", "price": 200 },
        { "id": "minecraft:chest", "name": "Dungeon Loot", "price": 1000, "lootTable": "minecraft:chests/simple_dungeon" },
        {
          "type": "command",
          "command": "effect give %player% minecraft:regeneration 300 1",
          "price": 300,
          "displayItem": {
            "material": "minecraft:potion",
            "displayname": "Regeneration Potion (5min)",
            "enchantEffect": true
          }
        }
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
- Cobblemon Raid Dens: direct raid win detection via `RaidEvents.RAID_END`.
- CobbleDollars: optional `/convertcobbledollars <amount|all>` command to convert CobbleDollars into PokeDollars.
- Impactor: optional `/convertimpactor <amount|all>` command using Impactor primary currency.

Currency backend behavior (`main_currency`):
- `cobeco`: Cobblemon Economy database is authoritative for PokeDollars.
- `cobeco` mirrors balances to CobbleDollars and Impactor accounts when those mods are installed.
- `cobbledollars`: Cobblemon Economy `balance` operations use CobbleDollars player balance (online players).
- `impactor`: Cobblemon Economy `balance` operations use Impactor primary account balance.

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

## Build
```bash
./gradlew build
```
Jar output: `build/libs/` (use the remapped jar).

## Support
Discord: https://discord.gg/zxZXcaTHwe
