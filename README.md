# Cobblemon Economy

Cobblemon Economy is a server-focused economy and shop system for Cobblemon on Fabric. It provides NPC shops, dual currencies, persistent storage, and reward hooks for captures and battles.

Website: https://ryvexam.fr

Developer documentation:
- [Architecture and runtime flows](docs/architecture.md)
- [Contributor and operator workflow](CONTRIBUTING.md)
- [Technical audit and follow-up priorities](docs/technical-audit.md)

## Cobblemon compatibility

The development build targets Cobblemon 1.8.1 on Fabric 1.21.1. Capture and
Pokédex reward handling remains compatible with Cobblemon 1.7.x: the former
`CAUGHT` progression name and the 1.8+ `OWNED` name are normalized internally.
Install the matching Fabric Loader/API required by the Cobblemon version on the
server; the economy mod itself does not require a separate 1.7 or 1.8 build.

### Tested release matrix

Each JAR below was tested individually on a dedicated Fabric server with
Minecraft `1.21.1`, Fabric Loader `0.19.5`, Fabric API `0.116.17+1.21.1`,
Cobblemon `1.8.1+1.21.1`, and Java 21. The JARs were never installed side by
side.

| Version | Start | Mod init | `/eco reload` | Shops | Quests | Clean stop |
| --- | :---: | :---: | :---: | :---: | :---: | :---: |
| `0.0.13` | ✅ | ✅ | ✅ | ✅ | N/A | ✅ |
| `0.0.14` | ✅ | ✅ | ✅ | ✅ | N/A | ✅ |
| `0.0.15` | ✅ | ✅ | ✅ | ✅ | N/A | ✅ |
| `0.0.16` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `0.0.17` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `0.0.18` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

The complete migration sequence `0.0.13 → 0.0.14 → 0.0.15 → 0.0.16 →
0.0.17 → 0.0.18` was also tested. Legacy balances, PCO, purchase/sell limits,
capture counts, quest state, and quest progress were preserved. See the full
[version and migration report](docs/version-compatibility.md).

Tagged releases are built and published by Forgejo CI only. Push a version tag
such as `v0.0.18` to run the Java 21 build and publish the JAR to Modrinth and
CurseForge; credentials are stored only as Forgejo repository secrets.

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
- Quest board interface (client custom screen, non-SGUI)
- Optional integrations: YAWP protection flag and Star Academy grading
- Optional CobbleDollars and Impactor bridge compatibility

## Commands
Player:
- `/bal` or `/balance`
- `/pco`
- `/pay <player> <amount>`

Admin (permission level 2):
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

## Configuration
All server data is stored under `world/config/cobblemon-economy/`. The JSON
files are intentionally separated by responsibility:

| File | Contents | How to edit |
| --- | --- | --- |
| `config.json` | Global economy settings: currency backend, starting balances, rewards, multipliers, profiling, and legacy inline shop fallback | Edit global economy values here |
| `shops.json` | Shop IDs, NPC display settings, buy/sell modes, item entries, limits, loot tables, and command rewards | Edit shops here; this is the authoritative shop file |
| `milestone.json` | Unique-capture milestone thresholds and rewards | Edit milestone rewards here |
| `quests.json` | Quest IDs, objectives, prerequisites, rotation rules, and rewards | Edit quest definitions here |
| `quest_npcs.json` | Quest NPC/board IDs, dialogue, quest pools, rotations, skins, and active-quest limits | Edit which NPC/board offers which quests here |
| `quest_boards_bindings.json` | Dimension and block-position bindings to quest board IDs | Prefer `/eco questboard bind` and `/eco questboard unbind` |

`shops.json` is the preferred source for shops. Existing servers remain
backward-compatible: if it is missing, the mod reads the legacy `shops` object
inside `config.json`, writes a new `shops.json`, and keeps the legacy data
readable for older JARs. If both are present and `shops.json` contains shops,
it is authoritative; a missing or empty separate file can still fall back to
the legacy inline object. Shop, item, quest, NPC, and board IDs are persistent
identifiers and must not be renamed without an explicit migration.

After editing JSON, run `/eco reload`. The reload validates and rewrites only
the affected JSON files; it does not reset SQLite balances or quest progress.
Current writes are atomic, create `.bak` backups, and quarantine malformed files
as `.broken-<timestamp>` before defaults are regenerated.

Persistence compatibility:
- JSON files accept the legacy field names and contain a `configVersion` when written by current versions.
- Existing `economy.db` and `quests.db` files are migrated additively with a `.bak` backup before upgrade.
- Invalid JSON is preserved as a timestamped `.broken-*` file before defaults are regenerated.
- Keep shop, item, quest, NPC, and board IDs stable because they are persistent references.
- See [`docs/persistence-compatibility.md`](docs/persistence-compatibility.md) for upgrade, downgrade, and recovery procedures.
- See [`docs/version-compatibility.md`](docs/version-compatibility.md) for the tested `0.0.13` → current migration matrix.

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
- `capture_event_base_reward` (defaults to `battleVictoryReward` if missing; reward for a first valid capture, and reused as the base for fossil revive special payouts)
- `capture_multi_reward` (defaults to `0`; reward for repeat non-special captures of an already known species)
- Special captures (`shiny`, `radiant`, `legendary`/`mythical`, `paradox`) still use `capture_event_base_reward` multiplied by their configured special multiplier.
- `battleVictoryPcoReward`
- `battleTowerCompletionPcoBonus` (small extra PCO reward on Battle Tower wins)
- `capture_shiny_multiplier`
- `capture_radiant_multiplier`
- `capture_legendary_multiplier`
- `capture_paradox_multiplier`
- `enableProfiling` (logs slow operations when true)
- `profilingThresholdMs` (minimum ms to log)

Legacy compatibility:
- Old keys like `captureReward`, `capture_reward`, `newDiscoveryReward`, `special_capture_reward_ignores_pokedex_history`, `normal_capture_reward_requires_new_pokedex_entry`, `shinyMultiplier`, `radiantMultiplier`, `legendaryMultiplier`, and `paradoxMultiplier` are still detected for migration.
- On the next config rewrite, Cobblemon Economy saves `capture_event_base_reward`, `capture_multi_reward`, and the explicit multiplier key names above.

Shop definition fields:
- `title`
- `currency` (`POKE` or `PCO`)
- `skin` (filename without extension)
- `skinModel` (optional: `steve` or `alex`, default `steve`)
- `isSellShop` (true to let players sell)
- `linkedShop` (optional)
- `linkedShopIcon` (optional item id)
- `items`

Item definition fields:
- `type` - `"item"` (default) or `"command"`
- `id` - Required stable identifier for every entry
  - for `type: "item"`: use the real item ID
  - for `type: "command"`: use an internal key such as `server:vote_key`
- `name` - Display name
- `price`
- `nbt` (legacy NBT string)
- `dropTable` (array of item ids)
- `lootTable` (minecraft loot table id)
- `components` (data components for items, written as normal JSON)
- `enchantments` (shorthand, no escaping needed)
- `lore` (shorthand, list of lines)
- `unbreakable` (shorthand, boolean)
- `customModelData` (shorthand, integer)
- `glint` (shorthand, boolean fake enchant shine)
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
        { "id": "academy:booster_pack[academy:booster_pack='base']", "name": "Booster Pack", "price": 100 },
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

### Enchanted / customized items (no escaping needed)

You never have to write escaped JSON like `"{\"levels\":{...}}"`. Pick whichever style you prefer.

**1. Shorthand fields (simplest):**
```json
{
  "id": "minecraft:diamond_sword",
  "name": "Champion Blade",
  "price": 5000,
  "enchantments": { "sharpness": 5, "unbreaking": 3, "mending": 1 },
  "lore": ["Reward of the Champion", "Untradeable"],
  "unbreakable": true
}
```
`enchantments` also accepts a list: `["sharpness 5", "unbreaking 3"]`.
Names without a namespace are treated as `minecraft:`, and a missing level means `1`.

**2. Real JSON in `components` (full control, any component):**
```json
{
  "id": "minecraft:diamond_sword",
  "name": "Sharpness V",
  "price": 5000,
  "components": {
    "minecraft:enchantments": { "levels": { "minecraft:sharpness": 5 } },
    "minecraft:custom_name": { "text": "Sharpness V", "color": "gold" },
    "academy:booster_pack": "base"
  }
}
```

**3. Vanilla `/give` syntax on the id** (use single quotes so JSON stays clean):
```json
{ "id": "minecraft:diamond_sword[minecraft:enchantments={levels:{'minecraft:sharpness':5}}]", "name": "Sharpness V", "price": 5000 }
```

Notes:
- Component names without a namespace are treated as `minecraft:`.
- If the same component is set in several ways, `components` wins over the shorthand fields, which win over the inline id syntax.
- Old configs using escaped strings (`"minecraft:enchantments": "{\"levels\":{\"minecraft:sharpness\":5}}"`) keep working.
- The shorthands also work on `type: "command"` entries to decorate the GUI icon.

**Command execution item** (sells a command instead of an item):
```json
{
  "id": "server:vote_key",
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

Command item note:
- `id` is still mandatory for command entries because the config loader and limit tracking use it as the entry key.

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
          "id": "server:regeneration_potion",
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
- In `shops.json` and `quest_npcs.json`, you can set `skinModel` to `steve` or `alex` for arm model type.

## Storage
- Config: `world/config/cobblemon-economy/config.json`
- Shops: `world/config/cobblemon-economy/shops.json`
- Milestones: `world/config/cobblemon-economy/milestone.json`
- Quests: `world/config/cobblemon-economy/quests.json`
- Quest NPCs: `world/config/cobblemon-economy/quest_npcs.json`
- Quest board bindings: `world/config/cobblemon-economy/quest_boards_bindings.json`
- Database: `world/config/cobblemon-economy/economy.db`
- Quest database: `world/config/cobblemon-economy/quests.db`
- Transactions: `world/config/cobblemon-economy/transactions.log`
- Skins: `world/config/cobblemon-economy/skins/`

For the full upgrade, downgrade, and recovery procedure, see
[`docs/persistence-compatibility.md`](docs/persistence-compatibility.md).

## Integrations
- Cobblemon (required): capture, pokedex, battle victory, and fossil events come from Cobblemon's event bus.
- Cobblemon Raid Dens (optional): raid completion is read from `com.necro.raid.dens.common.events.RaidEvents` (`RAID_BATTLE_START` / `RAID_END`).
- Battle Tower-like systems (optional): no hard dependency; tower wins are detected from battle actor metadata, entity IDs/tags, and explicit `tour_de_combat` NPC tag.
- YAWP: flag `melee-npc-cobeco` controls shopkeeper vulnerability.
- Star Academy: optional grading integration when the `academy` mod is present.
- CobbleDollars: optional compatibility bridge support.
- Impactor: optional compatibility bridge support.

Quest event notes:
- `raid_win` objectives use Raid Dens `RAID_END` when the Raid Dens mod is available, with fallback detection from Cobblemon battle events when it is not.
- `tower_win` objectives rely on tower actor detection; for custom NPC towers, tag NPCs with `tour_de_combat` (Tower Tagger from `/eco item`) to guarantee detection.

Currency backend behavior (`main_currency`):
- `cobeco`: Cobblemon Economy database is authoritative for PokeDollars.
- `cobeco` mirrors balances to CobbleDollars and Impactor accounts when those mods are installed, and bridges their API transactions into CobEco.
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

Only the `cobeco` namespace is registered by the current Placeholder API
integration.

## Build
```bash
./gradlew build
```
Jar output: `build/libs/` (use the remapped jar).

For the complete development checklist, see [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Support
Discord: https://discord.gg/zxZXcaTHwe

## Acknowledgements
Thanks to Rikunji for their contributions to the project.
