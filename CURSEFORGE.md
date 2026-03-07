# Cobblemon Economy

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-3C8527?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![Discord](https://img.shields.io/badge/Discord-Join%20Support-5865F2?logo=discord&logoColor=white)](https://discord.gg/zxZXcaTHwe)

Cobblemon Economy is an all-in-one economy + shop + quest layer for Cobblemon servers.

This page is a full practical guide for server admins: install, required/optional mods, shops, skins, quantity controls, quests, and JSON config examples.

## 1) Required and Optional Dependencies

### Required
- Minecraft `1.21.1`
- Fabric Loader
- Fabric API
- Cobblemon `1.7.1`

### Optional integrations
- Cobblemon Raid Dens (raid rewards + `raid_win` objectives)
- Cobblemon Battle Tower / tower-style NPC setups (`tower_win` objectives)
- CobbleDollars (economy bridge compatibility)
- Impactor (economy bridge compatibility)
- YAWP (NPC protection flag support)
- Placeholder API / TAB (balance placeholders)
- Star Academy (`academy`) grading integration

### Raid and Battle Tower notes
- **Raid objectives (`raid_win`)**: fully supported when Cobblemon Raid Dens is installed.
  - Integration uses Raid Dens runtime events (`RAID_BATTLE_START` / `RAID_END`).
  - `raid_win` quest progression and raid rewards are triggered from raid win events.
- **Battle Tower objectives (`tower_win`)**: no hard dependency required.
  - Detection is metadata/tag based (tower actor/entity IDs/tags).
  - For custom NPC towers, use the Tower Tagger (`/eco item`) to apply `tour_de_combat` tag for reliable detection.

## 2) Installation

1. Drop `cobblemon-economy-<version>.jar` into `mods/`.
2. Start server once.
3. Edit files in `world/config/cobblemon-economy/`.
4. Use `/eco reload` after JSON edits (full restart is safest after major changes).

## 3) Files and What They Do

Per world folder:
- `world/config/cobblemon-economy/config.json` -> global economy settings.
- `world/config/cobblemon-economy/shops.json` -> all shop definitions.
- `world/config/cobblemon-economy/quests.json` -> all quest definitions.
- `world/config/cobblemon-economy/quest_npcs.json` -> quest NPC board definitions.
- `world/config/cobblemon-economy/milestone.json` -> capture milestone rewards.
- `world/config/cobblemon-economy/skins/*.png` -> custom NPC skins.
- `world/config/cobblemon-economy/economy.db` -> economy database.
- `world/config/cobblemon-economy/quests.db` -> quest state database.

## 4) Core Commands

### Player
- `/bal` or `/balance`
- `/pco`
- `/pay <player> <amount>`

### Admin
- `/eco reload`
- `/eco shop list`
- `/eco shop get <id>`
- `/eco quest list`
- `/eco questnpc list`
- `/eco questnpc get <id>`
- `/eco questboard list`
- `/eco questboard open <id>`
- `/eco questboard bind <id>`
- `/eco questboard unbind`
- `/eco skin <name>`
- `/eco item`
- `/balance <player> <add|remove|set> <amount>`
- `/pco <player> <add|remove|set> <amount>`

## 5) Shop System (Creation, Binding, Quantity, Buy/Sell)

### 5.1 Create a shop in JSON
Edit `shops.json`:

```json
{
  "shops": {
    "general_shop": {
      "title": "General Shop",
      "currency": "POKE",
      "skin": "shopkeeper",
      "skinModel": "steve",
      "isSellShop": false,
      "items": [
        { "id": "minecraft:diamond", "name": "Diamond", "price": 1000 },
        { "id": "cobblemon:poke_ball", "name": "Poke Ball", "price": 200 }
      ]
    }
  }
}
```

Important fields:
- `currency`: `POKE` or `PCO`.
- `skin`: PNG name (without `.png`) from `world/config/cobblemon-economy/skins/`.
- `skinModel`: `steve` or `alex` (optional, default `steve`).
- `isSellShop`: `true` for sell mode.
- `items`: list of items/command-items.

### 5.2 Bind a shop to an NPC
1. Spawn NPC (`shopkeeper_spawn_egg` or `/summon cobblemon-economy:shopkeeper`).
2. Run `/eco shop get <shopId>`.
3. Right-click NPC with the setter item.

### 5.3 Set NPC skin
1. Put skin file in `world/config/cobblemon-economy/skins/` (example: `nurse.png`).
2. Run `/eco skin nurse`.
3. Right-click NPC with the Skin Setter.

You can also define defaults in JSON per shop or quest NPC:
- `"skin": "nurse"`
- `"skinModel": "alex"`

### 5.4 Change buy quantity in GUI
- Middle-click cycles quantity multiplier: `1x -> 2x -> 4x -> 8x -> 16x -> 32x -> 64x`.
- Price updates using current selected quantity.
- Left click buys at selected quantity.

### 5.5 Sell shops
For a sell shop:
- set `"isSellShop": true`.
- players can sell matching items back.
- supports optional sell limits/cooldowns per item.

## 6) Quest System (How It Works)

Quest system uses 2 files:
- `quests.json` = mission definitions.
- `quest_npcs.json` = which NPC/board offers which quests.

Flow:
1. Player opens board.
2. Accepts quest.
3. Progress updates from captures/battles/raids/tower/fossil events.
4. Quest becomes claimable when all objectives are complete.
5. Player claims rewards.

## 7) `quests.json` (Exact Authoring Guide)

Top-level key must be `quests`.

```json
{
  "quests": {
    "my_quest_id": {
      "name": "My Quest",
      "repeatPolicy": "DAILY",
      "repeatable": true,
      "timeLimitMinutes": 1440,
      "cooldownMinutes": 0,
      "requiresCompleted": [],
      "objectives": [],
      "rewards": {
        "pokedollars": 2000,
        "pco": 0,
        "commands": []
      }
    }
  }
}
```

### Objective types
- `capture`
- `fossil_revive`
- `battle_win`
- `raid_win`
- `tower_win`

### Capture/fossil filters
Use any combination:
- `species` (array)
- `types` (array)
- `labels` (array)
- `pokeball` (array)
- `dimension` (single string) or `dimensions` (array)
- `shiny` (`true`/`false`)

### Example: shiny Gyarados quest

```json
{
  "quests": {
    "catch_shiny_gyarados": {
      "name": "Red Leviator",
      "repeatPolicy": "DAILY",
      "repeatable": true,
      "objectives": [
        {
          "type": "capture",
          "count": 1,
          "species": ["cobblemon:gyarados"],
          "shiny": true
        }
      ],
      "rewards": {
        "pokedollars": 15000,
        "pco": 120,
        "commands": []
      }
    }
  }
}
```

Yes: this setup shows a **shiny Gyarados preview** on the quest board.

### Example: raid and tower objectives

```json
{
  "quests": {
    "raid_runner": {
      "name": "Raid Runner",
      "repeatPolicy": "DAILY",
      "repeatable": true,
      "objectives": [
        { "type": "raid_win", "count": 3 }
      ],
      "rewards": { "pokedollars": 6000, "pco": 60, "commands": [] }
    },
    "tower_runner": {
      "name": "Tower Runner",
      "repeatPolicy": "DAILY",
      "repeatable": true,
      "objectives": [
        { "type": "tower_win", "count": 2 }
      ],
      "rewards": { "pokedollars": 5000, "pco": 80, "commands": [] }
    }
  }
}
```

## 8) `quest_npcs.json` (Board/NPC Definitions)

```json
{
  "quest_npcs": {
    "safari_guide": {
      "displayName": "Safari Guide",
      "skin": "shopkeeper",
      "skinModel": "alex",
      "maxActive": 2,
      "visibleQuests": 6,
      "sharedRotation": true,
      "rotationMode": "MIDNIGHT",
      "rotationHours": 24,
      "questPool": [
        "catch_shiny_gyarados",
        "raid_runner",
        "tower_runner"
      ]
    }
  }
}
```

Field behavior:
- `maxActive`: max simultaneously active quests for that NPC board.
- `visibleQuests`: visible slots on board (minimum effective value is 6).
- `sharedRotation`:
  - `true` = same selection for all players.
  - `false` = per-player selection.
- `rotationMode`:
  - `MIDNIGHT` = rotates at next local server midnight.
  - `HOURS` = rotates every `rotationHours` block.
- `questPool`: quest IDs from `quests.json`.

Bind an NPC to a quest profile:
1. `/eco questnpc list`
2. `/eco questnpc get <id>`
3. Right-click a shopkeeper NPC with the setter.

## 9) Main Currency Modes

Set in `config.json`:

```json
{
  "main_currency": "cobeco"
}
```

Values:
- `cobeco` (default)
- `cobbledollars`
- `impactor`

Behavior:
- `cobeco`: CobEco DB is authoritative, with optional mirror/bridge support.
- `cobbledollars`: balance ops route to CobbleDollars.
- `impactor`: balance ops route to Impactor primary account.

## 10) Common JSON Mistakes (Avoid These)

- Wrong key names:
  - use `types` (not `type` for filters)
  - use `pokeball` (not `ball`)
  - `species` must be an array
- Invalid IDs:
  - use full IDs like `cobblemon:gyarados`, `minecraft:the_nether`
- `objectives` must be an array (`[]`) even for one objective.
- `count` must be `>= 1`.
- `repeatPolicy` must be `DAILY`, `ALWAYS`, or `ONCE`.
- If you changed JSON and behavior does not update, run `/eco reload` or restart.

## 11) Integrations Summary

- Cobblemon: core capture/battle/fossil events.
- Cobblemon Raid Dens: raid win rewards + `raid_win` quest progression.
- Battle Tower-like systems: `tower_win` detection via battle metadata/tags (use Tower Tagger for custom setups).
- CobbleDollars / Impactor: compatibility bridge.
- Placeholder API / TAB: balance placeholders.
- YAWP: shopkeeper protection flag checks.

## 12) Support

Discord support: https://discord.gg/zxZXcaTHwe
