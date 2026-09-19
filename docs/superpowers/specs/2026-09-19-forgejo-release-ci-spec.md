# Forgejo release CI specification

## Goal

Build and publish one release JAR automatically when a `v*` tag is pushed to
Forgejo, while keeping the exact same commit mirrored to GitHub and ensuring
publishing credentials never enter the repository.

## Scope

- Add one workflow at `.forgejo/workflows/release.yml`.
- Run only on pushed version tags matching `v*`.
- Use the repository Gradle wrapper with Java 21 provisioned by the workflow.
- Override `mod_version` from the tag so `v0.0.18` produces
  `cobblemon-economy-0.0.18.jar`.
- Run `./gradlew clean test build` before publishing.
- Upload the main JAR to Modrinth and CurseForge through their APIs.
- Use Forgejo repository secrets/variables only; GitHub receives the workflow
  file but has no credentials and does not execute `.forgejo/workflows`.

## Required Forgejo configuration

Secrets:

- `MODRINTH_TOKEN`
- `CURSEFORGE_TOKEN`

Public configuration file:

- `.forgejo/release-config.json` — Modrinth/CurseForge project IDs, Minecraft
  version, loader, and numeric CurseForge game-version IDs.

The workflow must fail before upload when any required value is empty.

## Security constraints

- Never put API tokens in YAML literals, URLs, artifacts, logs, or Gradle
  properties.
- Do not run release publishing for branches or pull requests.
- Do not upload `-sources.jar`; publish only the distributable mod JAR.
- Keep checkout and build steps before any external publishing step.
- Protect `v*` tags in Forgejo and restrict tag creation to trusted release
  maintainers; a tag-triggered workflow necessarily receives publishing
  secrets and must not run from untrusted workflow changes.

## Release metadata

- Tag: `v<semantic-version>`.
- Version passed to Gradle: tag without the leading `v`.
- Minecraft: `1.21.1`.
- Loader: Fabric.
- Release channel: release.
- Changelog: `Unreleased` section from `CHANGELOG.md`, with a fallback message
  containing the tag when that section is empty.

## Non-goals

- No GitHub Actions workflow.
- No automatic creation of a GitHub or Forgejo release object in this first
  version.
- No changes to runtime mod behavior or dependency resolution.
