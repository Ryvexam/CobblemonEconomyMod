# Cobblemon Economy

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-3C8527?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![Discord](https://img.shields.io/badge/Discord-Join%20Support-5865F2?logo=discord&logoColor=white)](https://discord.gg/zxZXcaTHwe)

Cobblemon Economy is a complete economy + shop system for Cobblemon servers.
It provides NPC shops, dual currencies, reward events, cross-economy bridges (CobbleDollars/Impactor), and a per-world config workflow.

## Discord (Support)
**Join here:** https://discord.gg/zxZXcaTHwe

## What You Get
- PokeDollars + PCO currencies
- Buy and sell NPC shops with pagination
- Command-based shop items (sell commands, not only items)
- Capture/discovery/battle rewards + multipliers
- Raid Dens reward compatibility
- Cross-economy conversion commands
- Switchable main backend: `cobeco`, `cobbledollars`, `impactor`

## Installation
1. Put `cobblemon-economy-0.0.15.jar` in `mods/`.
2. Start the server once.
3. Edit `world/config/cobblemon-economy/config.json`.
4. Restart server (or `/eco reload` for config-only edits).

## File Layout (Per World)
- `world/config/cobblemon-economy/config.json`
- `world/config/cobblemon-economy/milestone.json`
- `world/config/cobblemon-economy/skins/*.png`

`main_currency` is also **per-world** because it lives in this world config folder.

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
- `cobeco`: CobEco DB is authoritative; balances are mirrored to CobbleDollars and Impactor when present; API bridge support is enabled.
- `cobbledollars`: CobEco balance operations route to CobbleDollars.
- `impactor`: CobEco balance operations route to Impactor primary currency.

## Conversions
Rates in `config.json`:
- `cobbleDollarsToPokedollarsRate`
- `impactorToPokedollarsRate`

Examples:
- `/convertcobbledollars all`
- `/convertimpactor 250`

## Rewards
Main reward keys:
- `battleVictoryReward`
- `raidDenVictoryReward`
- `captureReward`
- `newDiscoveryReward`
- `battleVictoryPcoReward`

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
- CobbleDollars (bridge + conversion)
- Impactor (bridge + conversion)
- Cobblemon Raid Dens (raid win rewards)
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

## Need Help?
Discord support: https://discord.gg/zxZXcaTHwe
