# Cobblemon Economy

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-3C8527?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![Discord](https://img.shields.io/badge/Discord-Join%20Support-5865F2?logo=discord&logoColor=white)](https://discord.gg/zxZXcaTHwe)
[![Economy Bridge](https://img.shields.io/badge/Bridge-CobbleDollars%20%2B%20Impactor-1F8B4C)](#main-currency-modes)
[![Server Ready](https://img.shields.io/badge/Use%20Case-Cobblemon%20Servers-2D7D9A)](#what-you-get)

Cobblemon Economy is the all-in-one economy core for Cobblemon servers.
It gives you modern NPC shops, dual currencies, reward systems, and most importantly a compatibility bridge so you can keep mod compatibility while still using CobEco as your main economy.

## Why Cobblemon Economy
- One economy core for your server instead of fragmented balances.
- Bridge support for CobbleDollars and Impactor, so many existing economy-dependent mods keep working.
- Main-currency switch with per-world config (`cobeco`, `cobbledollars`, `impactor`).
- PvE progression rewards (capture, battle, raid dens, battle tower).
- Production-friendly: JSON config, hot reload tools, SQLite persistence.

## Discord (Support)
**Join here:** https://discord.gg/zxZXcaTHwe

## What You Get
- PokeDollars + PCO currencies
- Buy and sell NPC shops with pagination
- Quest NPCs with configurable mission boards
- Command-based shop items (sell commands, not only items)
- Capture/discovery/battle rewards + multipliers
- Raid Dens reward compatibility
- Battle Tower PCO rewards + completion bonus
- Cross-economy bridge + conversion support
- Switchable main backend: `cobeco`, `cobbledollars`, `impactor` (per world)

## Quest NPC System (0.0.16)
- One NPC entity, two roles: `SHOP` or `QUEST` (no duplicate entity type).
- Per-world quest files with full data-driven behavior:
  - `quests.json`: objectives, rewards, repeatability, cooldowns, prerequisites.
  - `quest_npcs.json`: NPC pool, max active quests, visible board size, rotation strategy.
- Quest objective coverage includes:
  - Capture filters (`species`, `type`, `ball`, `dimension`/`dimensions`, shiny, labels).
  - Progression events (`battle_win`, `raid_win`, `tower_win`, `fossil_revive`).
- Reward model supports:
  - Currency rewards (`pokedollars`, `pco`) and optional command rewards per quest.
  - Prerequisite chains, repeat policies, time limits, and cooldown lockouts.
- Custom quest board UX:
  - Up to 4 missions visible by default (configurable per NPC).
  - Detailed objective text (species/type/ball/location/shiny/labels).
  - Time remaining, cooldown remaining, and clear action states.
- Rotation system:
  - Shared board for all players or per-player board (configurable).
  - Rotation by midnight or every X hours.
  - Cancelled/expired quests can be blocked until next rotation.

## How To Set A Quest NPC (Summon + Bind)
- Spawn a shopkeeper NPC:
  - Use the spawn egg `cobblemon-economy:shopkeeper_spawn_egg`, or
  - Run `/summon cobblemon-economy:shopkeeper`.
- Check available quest NPC profiles with `/eco questnpc list`.
- Get a Quest NPC setter item with `/eco questnpc get <id>`.
- Right-click the spawned shopkeeper with that setter to switch it to `QUEST` role and bind `questNpcId`.
- Optional: switch the same NPC back to a normal shop with `/eco shop get <shopId>` then right-click.

### Minimal `quest_npcs.json` Example
```json
{
  "quest_npcs": {
    "safari_guide": {
      "displayName": "Safari Guide",
      "skin": "shopkeeper",
      "maxActive": 2,
      "visibleQuests": 4,
      "sharedRotation": true,
      "rotationMode": "MIDNIGHT",
      "rotationHours": 24,
      "questPool": ["safari_water_10", "safari_bug_12"]
    }
  }
}
```

## Configuration Warning
- Configuration support is limited.
- No hand-holding for custom config troubleshooting: test changes incrementally on a staging world/server.
- If a custom setup fails, validate JSON syntax, quest IDs, npc IDs, and objective keys first.

## Installation
1. Put `cobblemon-economy-0.0.16.jar` in `mods/`.
2. Start the server once.
3. Edit `world/config/cobblemon-economy/config.json`.
4. Restart server (or `/eco reload` for config-only edits).

## File Layout (Per World)
- `world/config/cobblemon-economy/config.json`
- `world/config/cobblemon-economy/shops.json`
- `world/config/cobblemon-economy/quests.json`
- `world/config/cobblemon-economy/quest_npcs.json`
- `world/config/cobblemon-economy/milestone.json`
- `world/config/cobblemon-economy/economy.db`
- `world/config/cobblemon-economy/quests.db`
- `world/config/cobblemon-economy/skins/*.png`

`main_currency` is also **per-world** because it lives in this world config folder.

### How Each Config Works
- `config.json`
  - Global economy behavior (currencies, rewards, multipliers, profiling).
  - Does **not** need to contain shops anymore.
- `shops.json`
  - All shop definitions (`title`, `currency`, `items`, limits, linked shops, etc.).
  - **Backward compatibility:** if this file is missing, the mod loads legacy `config.json.shops` and auto-creates `shops.json`.
- `quests.json`
  - Quest definitions and objectives.
  - Supports capture filters (species, type, ball, `dimension` or `dimensions`, shiny, labels).
  - Supports event objectives: `battle_win`, `raid_win`, `tower_win`, `fossil_revive`.
  - Per-quest control: `repeatable`, `repeatPolicy`, `cooldownMinutes`, `timeLimitMinutes`, `requiresCompleted`.
  - Supports rewards (`pokedollars`, `pco`, optional command list).
- `quest_npcs.json`
  - Quest NPC definitions (display name, skin, dialogue lines, quest pool, max active quests).
  - Board/rotation control: `visibleQuests`, `sharedRotation`, `rotationMode` (`MIDNIGHT`/`HOURS`), `rotationHours`.
  - One NPC can offer a list of multiple quests via `questPool`.
- `milestone.json`
  - Unique-capture milestone rewards.

## Quick Start (5 Minutes)
1. Start server once to generate defaults.
2. Open `config.json` and set:
```json
{
  "main_currency": "cobeco"
}
```
3. Add one test item to any shop.
4. Join server and run `/bal`.
5. Buy/sell to confirm economy flow.

## Commands
### Player
- `/bal` or `/balance`
- `/pco`
- `/pay <player> <amount>`
- `/convertcobbledollars <amount|all>`
- `/convertimpactor <amount|all>`

### Admin
- `/eco reload`
- `/eco shop list`
- `/eco shop get <id>`
- `/eco quest list`
- `/eco questnpc list`
- `/eco questnpc get <id>`
- `/eco skin <name>`
- `/eco item`
- `/balance <player> <add|remove|set> <amount>`
- `/pco <player> <add|remove|set> <amount>`

## Main Currency Modes
Configure in `config.json`:

```json
{
  "main_currency": "cobeco"
}
```

Valid values:
- `cobeco` (default)
- `cobbledollars`
- `impactor`

Behavior:
- `cobeco`: CobEco DB is authoritative; balances are mirrored to CobbleDollars and Impactor when present, and bridge hooks keep external transactions synced back into CobEco.
- `cobbledollars`: CobEco balance operations route to CobbleDollars.
- `impactor`: CobEco balance operations route to Impactor primary currency.

In practice: you can choose CobEco as your main economy and still keep broad compatibility with mods that expect CobbleDollars/Impactor-style flows.

## Rewards
Main reward keys:
- `battleVictoryReward`
- `raidDenVictoryReward`
- `captureReward`
- `newDiscoveryReward`
- `battleVictoryPcoReward`
- `battleTowerCompletionPcoBonus`

Multipliers:
- `shinyMultiplier`
- `radiantMultiplier`
- `legendaryMultiplier`
- `paradoxMultiplier`

## Milestones
File: `world/config/cobblemon-economy/milestone.json`

Example:
```json
{ "10": 300, "50": 700, "100": 1500, "200": 3000 }
```

## Shop Item Types
### Standard item
```json
{ "id": "minecraft:diamond", "name": "Diamond", "price": 1000 }
```

### Component item
```json
{
  "id": "academy:booster_pack",
  "name": "Booster Pack",
  "price": 200,
  "components": {
    "academy:booster_pack": "\"base\""
  }
}
```

### Command item
```json
{
  "type": "command",
  "command": "crate key give vote 1 %player%",
  "price": 100,
  "displayItem": {
    "material": "supplementaries:key",
    "displayname": "Vote Crate Key",
    "enchantEffect": true
  }
}
```

## Integrations
- CobbleDollars (bridge + conversion, transaction sync support)
- Impactor (bridge + conversion, transaction sync support)
- Cobblemon Raid Dens (raid win rewards)
- Cobblemon Battle Tower (PCO rewards on tower wins + completion bonus)
- Star Academy (optional)
- Placeholder API (optional)
- YAWP (optional)

## Placeholder API
Recommended namespace: `cobeco`
- `cobeco:balance`
- `cobeco:balance_symbol`
- `cobeco:pco`
- `cobeco:pco_symbol`

## Troubleshooting
- Command missing after update: restart server fully (not only `/eco reload`).
- Economy bridge not active: verify target mod is loaded and version-compatible.
- Wrong backend behavior: check `main_currency` in world config.
- Existing world does not show new default quest pools: your `quests.json` / `quest_npcs.json` already exists; merge manually or regenerate after backup.

## Need Help?
Discord support: https://discord.gg/zxZXcaTHwe
