# Cobblemon Economy — technical audit

**Audit date:** 2026-09-19  
**Scope:** source, build configuration, operator documentation, and available
local verification.  
**Method:** source inspection plus repository/build checks and local
Minecraft-agent verification. Findings distinguish code validation from runtime
validation requiring external mods.

## Cobblemon 1.8.1 compatibility checkpoint

The upstream tag [Cobblemon 1.8.1](https://gitlab.com/cable-mc/cobblemon/-/tree/1.8.1?ref_type=tags)
still targets Minecraft 1.21.1 and Java 21. The Fabric artifact metadata for
that release requires at least Fabric Loader `0.17.2` and Fabric API
`0.116.6+1.21.1`. The development build now targets the Fabric artifact
`maven.modrinth:MdwFAVRL:gBW3vLC7` (Cobblemon `1.8.1`), Fabric Loader `0.17.2`,
and Fabric API `0.116.6+1.21.1` in `gradle.properties`. The project/version ID
is required to distinguish Fabric from the same-numbered NeoForge artifact.

The Pokédex event API remains usable, but its progression enum changed from
`CAUGHT` to `OWNED`; the mod now compares the logical enum name so both 1.7.x
and 1.8.x can run the reward flow. Cobblemon 1.8 also added a `blockLight`
argument to the client `ModelWidget` constructor; the quest board selects the
matching constructor reflectively at runtime. Clean compilation succeeds
against both version-specific Fabric coordinates: `gBW3vLC7` for 1.8.1 and
`s64m1opn` for 1.7.1. A server smoke test reaches Fabric Loader dependency
resolution but stops before full startup because the local runtime pack is
missing `forgeconfigapiport` required by YAWP.

## Executive summary

The mod has clear feature boundaries and a workable server-authoritative model:
world-scoped persistence, `BigDecimal` currency storage, prepared SQL
statements, guarded optional integrations, and bounded skin payloads are all
visible in the source. The main gaps are security hardening around skin file
resolution, synchronous persistence on hot server paths, and documentation
drift between the current implementation and older guides.

## Evidence-based strengths

| Area | Evidence | Assessment |
| --- | --- | --- |
| World isolation | `fabric/CobblemonEconomy.java` resolves and normalizes the active world root before creating `config/cobblemon-economy` | Economy/config data is naturally separated per world |
| Monetary precision | `storage/EconomyManager.java` stores `balance`/`pco` as `TEXT` and uses `BigDecimal` in the Java API | Avoids SQLite floating-point balance loss |
| SQL parameterization | Economy queries use `PreparedStatement` placeholders for player, shop, item, and amount values | Reduces injection risk in the persistence layer |
| Event lifecycle | `events/CobblemonListeners.java` has a registration guard and `resetListeners()` on server stop | Prevents common duplicate-listener reload behavior |
| Broken shop isolation | `shop/ShopGui.java` catches item resolution errors and falls back to a barrier display | One bad item entry should not prevent the GUI from opening |
| Optional integrations | `compat/CompatHandler.java` checks/isolates YAWP and catches optional bridge registration failures | Missing optional mods have a defined degraded path |
| Network payload bound | `networking/ProvideSkinPayload.java` caps skin bytes at 1 MiB | Prevents unbounded skin payload allocation at codec level |

## Findings

### SEC-01 — Skin request path is not confined to the skin directory

**Severity:** High  
**Evidence:** `networking/RequestSkinPayload.java` accepts an unrestricted
string; `networking/NetworkHandler.java` concatenates it into
`skins/` paths before calling `Files.readAllBytes`. The client sanitizes the
name only for its local `ResourceLocation`, after the server has already read
the file.  
**Impact:** A malicious client may be able to request a path containing `..`
or separators and cause the server to read a file outside the configured skin
directory. The response is constrained to 1 MiB, but that does not make the
path boundary safe.  
**Recommendation:** Accept only a validated skin identifier (for example a
single filename stem), resolve the candidate path, call `toRealPath()` where
appropriate, and verify it remains below the world/global `skins` directory
before reading. Reject extensions and separators that are not part of the
documented format. Add a regression test at the payload/path boundary before
changing the implementation.

### PERF-01 — SQLite access is synchronous on gameplay paths

**Severity:** Medium  
**Evidence:** `storage/EconomyManager.java` opens a JDBC connection for many
individual reads/writes; these methods are called by command handlers, shop
clicks, and Cobblemon event callbacks.  
**Impact:** Disk latency or a busy database can block the server thread during
player interactions, purchases, or reward events and increase tick time.  
**Recommendation:** Measure first with the existing `PerformanceProfiler`, then
consider a single server-safe persistence queue, a bounded executor, or a
carefully managed connection/transaction strategy. Preserve ordering and
durability semantics before optimizing.

### MONEY-01 — Command amount parsing enters the economy through `double`

**Severity:** Medium  
**Evidence:** `commands/EconomyCommands.java` uses
`DoubleArgumentType` for `/pay` and administrative currency operations, then
converts the value to `BigDecimal`.  
**Impact:** The persistence layer is decimal-safe, but the command boundary
does not express a decimal contract explicitly and can accept values whose
scale/representation is surprising for server owners.  
**Recommendation:** Define the supported monetary scale and parse a bounded
string/decimal argument directly into `BigDecimal`; reject values outside the
policy before touching the backend. Add tests for `0.01`, large values,
negative values, and excessive scale.

### ECON-01 — Default quest PCO rewards are discarded

**Severity:** High  
**Evidence:** `storage/QuestConfig.java` defines `reward(String pokedollars,
String pco)`, and the generated quests pass non-zero PCO strings, but the
helper assigns `reward.pco = BigDecimal.ZERO` instead of parsing its `pco`
argument.  
**Impact:** Newly generated default quests advertise PCO rewards in their
definitions but currently grant zero PCO. This changes the server economy and
is not merely a documentation issue.  
**Recommendation:** Add a fixture/regression test that loads a fresh quest
configuration and asserts a known default quest's PCO reward, then parse the
argument or otherwise restore the intended default values in a separate bugfix
pass.

### ECON-02 — Balance and limit mutations are not uniformly atomic

**Severity:** High  
**Evidence:** `EconomyManager.addBalance()`/`subtractBalance()` and the purchase
and sell limit paths perform read-then-write operations across separate SQL
statements. `QuestManager.incrementObjectiveProgress()` follows the same
read-then-write shape for objective progress, while only some multi-step quest
operations explicitly use a transaction.  
**Impact:** Concurrent callbacks or repeated requests can lose updates, allow
double spending, or consume a limit inconsistently. The current server-thread
usage reduces but does not eliminate the risk introduced by future async or
network paths.  
**Recommendation:** Add concurrency-focused persistence tests, then use atomic
SQL updates/upserts or serialize all mutations through one server-owned queue.
Document the chosen transaction/ordering contract before changing it.

### ECON-03 — Public persistence methods accept unsafe currency values

**Severity:** Medium  
**Evidence:** Command arguments reject negative values, but public manager
methods such as `setBalance`, `setPco`, `addPco`, and subtraction helpers are
also callable from integrations and services without one shared non-negative
amount policy.  
**Impact:** A future caller can write negative balances or invert a transfer
in a way the command layer would reject.  
**Recommendation:** Define and enforce amount invariants at the manager
boundary, with explicit behavior for zero, negative, scale, and overflow
values. Keep command validation as user feedback, not as the only guard.

### TEST-01 — Focused persistence/compatibility tests exist; gameplay coverage remains limited

**Severity:** Low
**Evidence:** The mod now has nine JUnit tests covering Cobblemon status
compatibility, legacy inline shops/aliases, atomic JSON writes, SQLite schema
migration, backups, and future-schema refusal. There is still no GameTest suite
for full command, GUI, or reward flows.
**Impact:** Gameplay-specific regressions can still require a dedicated-server
smoke test or manual client interaction.
**Recommendation:** Keep the focused fixtures, then add service-level tests for
quest claims, shop transactions, and reward de-duplication before introducing
asynchronous persistence.

### QUEST-01 — Quest claim grants rewards before recording the claim

**Severity:** High  
**Evidence:** `QuestService.claimQuest()` calls `giveRewards(player, quest)`
before `QuestManager.markQuestClaimed(...)` updates the persistent state.  
**Impact:** A crash, disconnect, or database failure between those operations
can leave a completed quest claimable after rewards were already delivered,
allowing duplicate rewards.  
**Recommendation:** Add an idempotent claim test and make the claim/reward
operation durable as one application-level transaction. If Minecraft command
rewards cannot be rolled back, record a claim intent or unique payout before
executing rewards and provide recovery diagnostics.

### QUEST-02 — Board actions are not tied to an active board interaction

**Severity:** Medium  
**Evidence:** `NetworkHandler` accepts `QuestBoardActionPayload` and validates
the board ID/configuration, but does not verify distance to a board, a recent
server-issued opening, or a matching board block context. `claimQuest()` also
checks quest state but does not revalidate the current board rotation before
paying.  
**Impact:** A client that knows an ID can invoke board actions outside the
normal UI context; stale board state may remain usable longer than intended.
Server-side quest state still prevents many invalid transitions, but the
interaction boundary is broader than the documented board flow.  
**Recommendation:** Track a short-lived server-side board session or validate
the player position/context for each action, then re-resolve availability and
rotation immediately before accept/claim/cancel.

### SHOP-01 — Some purchase failures can consume payment or limits before recovery

**Severity:** Medium  
**Evidence:** `ShopGui` debits currency and consumes the purchase limit before
executing command, loot-table, or drop-table delivery. Missing command text is
refunded, but the already-consumed limit is not restored; loot generation
exceptions are logged after the debit without a general refund path. Inventory
insertion intentionally drops items when full.  
**Impact:** A malformed server configuration or runtime loot failure can charge
the player without delivering the configured result, and retries may be
blocked by a limit.  
**Recommendation:** Add a transaction outcome model with explicit refund and
limit rollback rules, validate command/loot definitions before charging, and
test missing commands, invalid loot tables, full inventories, and exceptions.

### DOC-01 — Admin and knowledge documentation are out of sync

**Severity:** Medium  
**Evidence:** `knowledge-cobblemoneconomy.md` still identifies version `0.0.10`
and old reward keys such as `newDiscoveryReward`; current code/config uses
explicit capture keys. `CURSEFORGE.md` describes the default quest rotation as
4 missions in one section, while `QuestNpcConfig` defaults `visibleQuests` to
6 and current board code enforces at least 6.  
**Impact:** Operators can configure deprecated fields or expect a different
number of visible quests.  
**Recommendation:** Treat `README.md`, `CURSEFORGE.md`, and the source model as
the maintained references; update or archive the knowledge base and correct
the rotation statement in a dedicated documentation change.

The same drift review found additional items to keep in the cleanup backlog:
`CHANGELOG.md` describes paid quest cancellation although the current code
cancels without a fee; `README.md` lists alternate Placeholder namespaces that
are not registered by the current integration; and several translation/config
keys are present in one locale or document but not used by the current code.

### DOC-02 — Configuration schema has no executable source-of-truth check

**Severity:** Low  
**Evidence:** Shop, economy, quest, NPC, and binding schemas are described in
multiple Markdown files while Gson model classes provide the actual accepted
fields. Representative legacy-loading tests now cover the highest-risk
compatibility paths, but there is still no generated schema or exhaustive
fixture comparison.
**Impact:** New fields and legacy aliases can drift between code and operator
examples.  
**Recommendation:** Add representative JSON fixture tests for each loader and
keep one canonical configuration reference linked from README and CURSEFORGE.

### DOC-03 — Mod metadata source link is not a source repository link

**Severity:** Low  
**Evidence:** `src/main/resources/fabric.mod.json` sets `contact.sources` to
`https://ryvexam.fr/`, which is the project website rather than an explicit
source repository URL.  
**Impact:** Mod-loader tooling and users looking for source code may not reach
the code repository directly.  
**Recommendation:** Once the intended source URL is decided, point
`contact.sources` to that repository and keep the website in `homepage`.

## Verification boundaries

| Check | Meaning | Current interpretation |
| --- | --- | --- |
| Static source audit | Confirms source-level structure and risks | Completed for this report |
| `./gradlew build --no-daemon --console=plain` | Compilation, resources, and packaging | Must be run after documentation changes; it does not prove gameplay correctness |
| Standalone agent tests | Tests the external CLI/MCP harness | Separate from this mod; do not count them as mod tests |
| `runServer` | Runtime integration | Valid only with compatible Cobblemon and optional dependencies present; a `mod_loading` result is a diagnostic failure, not a passing gameplay test |

## Follow-up order

1. Fix and regression-test `SEC-01` before exposing the mod to untrusted
   clients.
2. Fix and regression-test `ECON-01`, then define the claim/payout contract
   covering `ECON-02`, `QUEST-01`, and `SHOP-01`.
3. Add configuration/economy unit tests and define the monetary input policy.
4. Measure synchronous SQLite calls under a representative server load before
   selecting an async persistence design.
5. Consolidate the admin/knowledge documentation and add fixture checks for
   configuration examples.
6. Improve mod metadata links when the canonical repository URL is settled.
