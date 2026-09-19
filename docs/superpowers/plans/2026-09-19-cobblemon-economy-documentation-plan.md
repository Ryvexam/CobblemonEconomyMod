# Cobblemon Economy Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document the current Cobblemon Economy architecture, contributor workflow, and technical risks without changing mod behavior.

**Architecture:** The documentation will mirror the existing runtime boundaries: Fabric entrypoints and lifecycle, storage/configuration, shops/economy, quests/rewards, networking, and compatibility layers. A separate audit report will preserve evidence and prioritize follow-up work instead of mixing recommendations into operational documentation.

**Tech Stack:** Markdown, Fabric 1.21.1, Cobblemon 1.7.1, Gradle/Loom, SQLite, the standalone local Minecraft agent.

**Spec:** `docs/superpowers/specs/2026-09-19-cobblemon-economy-documentation-spec.md`

## Global Constraints

- No Java/Kotlin/resource behavior changes.
- Preserve the current Fabric 1.21.1 and Cobblemon 1.7.1 support target.
- Describe only configuration keys accepted by the current loaders.
- Record unavailable external-mod runtime tests as limitations, not successes.
- Keep machine-specific paths out of committed configuration.

## Review Focus

- Documentation path drift: all referenced source files, commands, and paths must exist.
- Configuration drift: examples must match `EconomyConfig`, `QuestConfig`, and `QuestNpcConfig`.
- Lifecycle drift: startup, reload, stop, and per-world storage behavior must match `CobblemonEconomy`.
- Optional integration claims: compatibility must be labeled optional and tied to actual classes.
- Verification honesty: build and runtime checks must distinguish local evidence from unavailable dependencies.

---

### Task 1: Baseline and audit ledger

**Files:**
- Create: `.superpowers/sdd/2026-09-19-cobblemon-economy-documentation-plan/progress.md`
- Modify: `tasks/todo.md`

**Interfaces:**
- Consumes: current repository state and the documentation specification.
- Produces: a traceable execution ledger and checklist for the documentation pass.

- [ ] **Step 1: Record the implementation baseline**

  Record the current commit, clean/dirty state, and the planned documentation
  files in the SDD ledger. Do not modify production code.

- [ ] **Step 2: Confirm the audit boundary**

  Record that findings are documentation-only recommendations unless a
  correctness issue prevents an accurate description.

- [ ] **Step 3: Mark the documentation checklist**

  Add a new section to `tasks/todo.md` with checkboxes for architecture,
  contribution guide, audit report, README links, and verification.

### Task 2: Runtime architecture documentation

**Files:**
- Create: `docs/architecture.md`
- Modify: `README.md`
- Modify: `agent.md`

**Interfaces:**
- Consumes: `CobblemonEconomy`, `EconomyManager`, `EconomyConfig`, `ShopGui`,
  `QuestService`, `QuestBoardService`, networking, and compatibility classes.
- Produces: one source-linked architecture reference and navigation links.

- [ ] **Step 1: Document startup and lifecycle**

  Describe common/client entrypoints, server-start loading, server-started
  integrations, stop cleanup, reload behavior, and interaction callbacks.

- [ ] **Step 2: Document data ownership**

  Describe `world/config/cobblemon-economy/`, JSON files, SQLite databases,
  skins, transaction logs, and the migration fallback from embedded shops to
  `shops.json`.

- [ ] **Step 3: Document feature flows**

  Add concise flow diagrams or ordered flows for economy operations, shop
  resolution/purchase/sale, reward events, quest board networking, and skins.

- [ ] **Step 4: Add navigation from existing guides**

  Link `README.md` and `agent.md` to the architecture document without
  duplicating the full reference.

### Task 3: Contributor and operator documentation

**Files:**
- Create: `CONTRIBUTING.md`
- Modify: `README.md`
- Modify: `skills.md`

**Interfaces:**
- Consumes: current Gradle tasks, local Minecraft agent CLI/MCP workflow, and
  the existing AI-agent guardrails.
- Produces: a repeatable inspect → change → build → test → diagnose workflow.

- [ ] **Step 1: Document prerequisites and commands**

  Document Java 21, Gradle wrapper commands, the standalone agent location,
  and the distinction between TypeScript harness checks and mod checks.

- [ ] **Step 2: Document change checklists**

  Cover Java changes, config schema changes, translations, network payloads,
  optional integrations, persistence changes, and runtime smoke tests.

- [ ] **Step 3: Document failure handling**

  Explain how to inspect `.minecraft-agent/runs/`, classify compilation,
  dependency, mod-loading, and timeout failures, and avoid claiming a server
  test passed without its runtime dependencies.

- [ ] **Step 4: Add concise repository navigation**

  Link the contributor guide from `README.md` and keep `skills.md` focused on
  AI-specific guardrails rather than duplicating operator instructions.

### Task 4: Technical audit report

**Files:**
- Create: `docs/technical-audit.md`

**Interfaces:**
- Consumes: source inspection, current documentation, build configuration,
  and verified command output.
- Produces: severity-ranked findings with evidence and follow-up actions.

- [ ] **Step 1: Record verified strengths**

  List architecture and reliability behaviors confirmed in source, such as
  prepared SQL statements, BigDecimal balance storage, path isolation, and
  optional integration guards.

- [ ] **Step 2: Record findings**

  For each finding include severity, evidence path/line area, user or operator
  impact, and a bounded recommendation. Do not silently fix findings.

- [ ] **Step 3: Record verification limits**

  Separate static audit evidence, local build evidence, and runtime tests that
  depend on Cobblemon/optional mods being installed.

### Task 5: Documentation verification

**Files:**
- Modify: `tasks/todo.md`
- Modify: `.superpowers/sdd/2026-09-19-cobblemon-economy-documentation-plan/progress.md`

**Interfaces:**
- Consumes: all documentation deliverables.
- Produces: checked references, clean diff, successful build, and review notes.

- [ ] **Step 1: Check referenced paths and commands**

  Run repository searches/scripts that detect stale `tools/minecraft-agent`
  paths, missing documentation targets, and Markdown whitespace errors.

- [ ] **Step 2: Run the mod build**

  Run `./gradlew build --no-daemon --console=plain` and record the actual
  result. If it fails, classify the failure instead of masking it.

- [ ] **Step 3: Review the diff and record results**

  Re-read all new documentation, update `tasks/todo.md` with a review section,
  append verification evidence to the ledger, and leave unrelated code
  untouched.
