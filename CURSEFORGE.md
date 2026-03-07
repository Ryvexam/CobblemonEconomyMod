# Cobblemon Economy

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-3C8527?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![Discord](https://img.shields.io/badge/Discord-Join%20Support-5865F2?logo=discord&logoColor=white)](https://discord.gg/zxZXcaTHwe)

Cobblemon Economy is an all-in-one economy + shop + quest layer for Cobblemon servers.

This page is a strict admin guide: install, required/optional mods, shops, skins, quantity controls, quests, JSON config examples, and current hard limits.
If a behavior is not described here, do not assume it is supported.

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

What `/eco reload` does:
- Reloads `config.json`, `shops.json`, `quests.json`, `quest_npcs.json`, and `quest_boards_bindings.json`.

What `/eco reload` does not do:
- It does not replace missing dependencies.
- It is not a substitute for a restart after adding/removing mods.

## 3) Files and What They Do

Per world folder:
- `world/config/cobblemon-economy/config.json` -> global economy settings.
- `world/config/cobblemon-economy/shops.json` -> all shop definitions.
- `world/config/cobblemon-economy/quests.json` -> all quest definitions.
- `world/config/cobblemon-economy/quest_npcs.json` -> quest NPC board definitions.
- `world/config/cobblemon-economy/quest_boards_bindings.json` -> block-position to quest-board ID bindings.
- `world/config/cobblemon-economy/milestone.json` -> capture milestone rewards.
- `world/config/cobblemon-economy/skins/*.png` -> custom NPC skins.
- `world/config/cobblemon-economy/economy.db` -> economy database.
- `world/config/cobblemon-economy/quests.db` -> quest state database.

### 3.1) Capture reward keys in `config.json`

To make the reward rules easier to read, the generated config now uses explicit capture-related keys:

- `capture_event_base_reward`
  - Base PokeDollar reward used when a capture payout is allowed.
  - Also reused as the base for fossil revive special payouts.
- `pokedex_new_species_bonus_reward`
  - Extra bonus only when the player adds a new species to the Pokedex.
- `normal_capture_reward_requires_new_pokedex_entry`
  - Default: `true`.
  - If `true`, a normal capture gives no payout when the species was already caught before.
- `special_capture_reward_ignores_pokedex_history`
  - Default: `true`.
  - If `true`, shiny / radiant / legendary / paradox captures can still pay even if the species already exists in the Pokedex.
- `capture_shiny_multiplier`
- `capture_radiant_multiplier`
- `capture_legendary_multiplier`
- `capture_paradox_multiplier`

Exact payout behavior with default flags:
- First normal capture of a new species: `capture_event_base_reward` + `pokedex_new_species_bonus_reward`.
- Re-capture of an already known normal species: no normal capture payout.
- Re-capture of an already known shiny / radiant / legendary / paradox species: still pays if `special_capture_reward_ignores_pokedex_history` is `true`.
- Capture milestone rewards are separate and come from `milestone.json`.
- First-time `legendary`, `mythical`, and `paradox` Pokedex entries do not receive the normal discovery bonus path; they use the special capture reward path instead.
- First-time shiny/radiant species can still combine the special capture payout with the Pokedex discovery bonus.

Special multiplier rule:
- Multipliers are direct payout factors, not `base + bonus`.
- Example with `capture_event_base_reward = 100` and `capture_shiny_multiplier = 5`: shiny payout = `500`, not `600`.
- If a Pokemon matches multiple special categories, factors are added together before multiplying.
- Example: shiny `5` + legendary `10` = `x15` total special payout.

Important scope note:
- The two Pokedex-history flags above affect capture-event payouts.
- Fossil revive rewards are handled by the fossil event path and still use `capture_event_base_reward` / special multipliers separately.

Legacy keys are still read for compatibility:
- `captureReward`
- `newDiscoveryReward`
- `shinyMultiplier`
- `radiantMultiplier`
- `legendaryMultiplier`
- `paradoxMultiplier`

When the config is rewritten, Cobblemon Economy saves the explicit key names.

Recommended strict Pokedex-only setup:

```json
{
  "capture_event_base_reward": 100,
  "pokedex_new_species_bonus_reward": 100,
  "normal_capture_reward_requires_new_pokedex_entry": true,
  "special_capture_reward_ignores_pokedex_history": false
}
```

With that setup:
- already-known species do not pay normal capture rewards
- already-known shiny / radiant / legendary / paradox species also do not pay capture rewards
- only genuinely new Pokedex species pay the normal capture reward path

### 3.2) Manual vs automatic files

- `quest_boards_bindings.json` is normally managed by `/eco questboard bind` and `/eco questboard unbind`.
- You can edit it manually, but the key format is `dimension;x;y;z`.
- The bind command uses the block you are currently looking at.
- If a placed `cobblemon-economy:quest_board` block has no explicit binding, it falls back to the first entry found in `quest_npcs.json`.
- For predictable behavior on production servers, bind every board explicitly.

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
- Every shop item entry must have an `id`, including `type: "command"` entries.
- For `type: "command"`, use `id` as a stable internal key (example: `server:vote_key`), not as a real given item.

Shop item behavior (exact):
- `type: "item"`:
  - uses `id` as the item to give/sell
  - supports `components`
  - supports legacy `nbt`
  - can also use `dropTable` or `lootTable`
- `type: "command"`:
  - executes `command` once per quantity purchased
  - supports `%player%` placeholder only
  - uses `displayItem.material`, `displayItem.displayname`, and `displayItem.enchantEffect` only for the GUI icon
  - does not give a real item to the player

Not supported / do not rely on this in shops:
- Extra placeholders beyond `%player%` in shop commands.
- `displayItem` changing the real purchased item for `type: "item"` entries.
- `components` or `nbt` customizing the visual icon of `type: "command"` entries.
- Weighted `dropTable` entries. If you need weights or advanced rolls, use a Minecraft `lootTable` instead.

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

Sell matching rule:
- Matching uses the same item and the same components/custom data.
- Display name alone is not enough.
- If you sell highly customized items, the player must hold the same effective item definition.

### 5.6 Wildcards, loot tables, and command-item limits

- Wildcards only support `namespace:*` format such as `minecraft:*` or `cobblemon:*`.
- Wildcards pick one random item from that namespace when the shop entry is resolved.
- Wildcards also randomize the configured price by about `-25%` to `+25%`.
- `lootTable` rolls once per purchased quantity.
- `dropTable` gives one random item ID per purchased quantity.
- If both `lootTable` and `dropTable` are present on the same entry, `lootTable` wins.
- `dropTable` entries are simple item IDs only; they do not support per-drop NBT/components/weights.
- `dropTable`, `lootTable`, and wildcards are best used in buy shops, not deterministic sell shops.

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

Lifecycle rules that matter:
- A quest can only be accepted if it is in the current visible rotation for that board.
- Cancelled quests stay unavailable until the next board rotation.
- Expired active quests are also pushed to the next board rotation.
- `requiresCompleted` only checks quests on the same quest-board / NPC ID.
- Cross-board prerequisite chains are not supported.

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

Quest definition behavior:
- `name`: display name shown on the board.
- `repeatPolicy`:
  - `DAILY` = next board rotation / next reset window
  - `ALWAYS` = can become available again immediately after claim unless `cooldownMinutes` is set
  - `ONCE` = permanently locked after claim
- `repeatable`:
  - keep this aligned with `repeatPolicy`
  - if set to `false`, the quest behaves like a one-time quest after claim
- `timeLimitMinutes`:
  - active quest expiry timer
  - when it expires, the quest is cancelled and pushed to the next board rotation
- `cooldownMinutes`:
  - if `> 0`, this explicit cooldown is used after claim
  - if missing/`0`, availability falls back to `repeatPolicy`
- `requiresCompleted`:
  - prerequisite quest IDs on the same board/NPC only
- `rewards`:
  - `pokedollars`, `pco`, and `commands` can be combined in the same quest reward

### Objective types
- `capture`
- `fossil_revive`
- `battle_win`
- `raid_win`
- `tower_win`

Filter logic:
- Within the same field, values are OR-matched.
  - Example: `"types": ["water", "ice"]` means water OR ice.
- Different fields stack as AND-matched.
  - Example: `species + pokeball + shiny` means all three conditions must match.
- `dimensions` takes priority over `dimension` if both are present.

### Capture/fossil filters
Use any combination:
- `species` (array)
- `types` (array)
- `labels` (array)
- `pokeball` (array)
- `dimension` (single string) or `dimensions` (array)
- `shiny` (`true`/`false`)

Exact support by objective type:
- `capture`:
  - supports `species`, `types`, `labels`, `pokeball`, `dimension`, `dimensions`, `shiny`
- `fossil_revive`:
  - supports `species`, `types`, `labels`, `dimension`, `dimensions`, `shiny`
  - `pokeball` is not available for fossils and should not be used
- `battle_win`, `raid_win`, `tower_win`:
  - use `count` only
  - extra filters are ignored

Quest reward commands:
- `rewards.commands` executes as server permission level 4.
- `%player%` is the only supported placeholder.
- Blank command strings are ignored.
- Command output is suppressed; use side effects/rewards, not chat-return values.

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
- `visibleQuests`: keep this at `6`.
  - values below `6` are raised to `6`
  - the current board UI only has 6 card slots, so values above `6` are not useful
- `sharedRotation`:
  - `true` = same selection for all players.
  - `false` = per-player selection.
- `rotationMode`:
  - `MIDNIGHT` = rotates at next local server midnight.
  - `HOURS` = rotates every `rotationHours` block.
- `rotationHours`:
  - only matters when `rotationMode` is `HOURS`
  - minimum effective value is `1`
- `questPool`: quest IDs from `quests.json`.
- `dialogues`:
  - accepted in config and kept in the file
  - the current custom quest board screen does not use these dialogue arrays as its main text source
  - treat them as optional flavor/config data, not as the authoritative board UI copy

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
- `repeatPolicy: "HOURLY"` is not a supported config value. Use board rotation (`rotationMode: "HOURS"`) or `cooldownMinutes` instead.
- Do not use `visibleQuests > 6` expecting extra board pages; the current board screen is fixed to 6 visible cards.
- Do not use `requiresCompleted` to chain quests between different board IDs.
- Do not expect `pokeball` filters to work on `fossil_revive` objectives.
- Do not expect `battle_win`, `raid_win`, or `tower_win` to filter by species/type/label; they are count-only objectives.
- Do not expect shop/quest command strings to support placeholders other than `%player%`.
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
