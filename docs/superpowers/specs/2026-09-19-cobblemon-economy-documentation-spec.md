# Cobblemon Economy Documentation Specification

## Goal

Make the current Cobblemon Economy implementation understandable and safer to
maintain without changing runtime behavior.

## Scope

- Document the real Fabric 1.21.1 / Cobblemon 1.7.1 runtime architecture.
- Document server startup, per-world files, economy persistence, shops,
  quests, rewards, networking, and optional integrations.
- Provide a contributor workflow covering inspection, implementation,
  configuration changes, translations, build, and runtime smoke tests.
- Record technical audit findings with severity, evidence, impact, and a
  concrete follow-up recommendation.

## Non-goals

- No Java/Kotlin behavior changes.
- No database migration or configuration schema change.
- No dependency upgrade.
- No claim that a runtime server test passes when required external mods are
  unavailable locally.

## Documentation contract

- Every important statement must point to a source file, command, or runtime
  path when it is not obvious from the surrounding text.
- Configuration examples must use the field names currently accepted by the
  loader.
- Findings must distinguish verified facts from recommendations.
- Documentation must remain useful for both human contributors and AI agents.
