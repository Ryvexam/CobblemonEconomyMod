# Forgejo Release CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and publish the Cobblemon Economy JAR to Modrinth and CurseForge from Forgejo whenever a `v*` tag is pushed.

**Architecture:** A single Forgejo Actions workflow checks out the tag, provisions Java 21 and release tooling, derives the semantic version, runs the existing Gradle test/build pipeline, selects the one non-sources JAR, and calls the Modrinth and CurseForge upload APIs. All credentials are injected only from Forgejo secrets; the workflow itself remains safe to mirror to GitHub.

**Tech Stack:** Forgejo Actions, Java 21, Gradle 9.4.1/Fabric Loom 1.16.2, Modrinth v2 version API, CurseForge upload API, Bash/curl/jq on the Forgejo runner.

**Spec:** `docs/superpowers/specs/2026-09-19-forgejo-release-ci-spec.md`

## Global Constraints

- Trigger only on tags matching `v*`.
- Build with Java 21 and `./gradlew clean test build --no-daemon --console=plain`.
- Require `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`, `MODRINTH_PROJECT_ID`, `CURSEFORGE_PROJECT_ID`, and `CURSEFORGE_GAME_VERSION_IDS`.
- Publish only `build/libs/cobblemon-economy-<tag-version>.jar`.
- Never expose tokens in workflow source, command arguments, logs, or artifacts.
- Keep CI Forgejo-only; do not add `.github/workflows`.

## Review Focus

- A branch push does not publish: the trigger must be tag-only.
- A tag `v0.0.18` must build version `0.0.18`, not stale `gradle.properties` version `0.0.17`.
- Missing secrets/variables must fail before build or upload.
- `-sources.jar` must never be selected for publication.
- Modrinth and CurseForge upload failures must fail the job instead of being ignored.
- The workflow must contain no credential value and remain safe on GitHub.

---

### Task 1: Add the Forgejo release workflow

**Files:**
- Create: `.forgejo/workflows/release.yml`

**Interfaces:**
- Consumes Forgejo secrets/variables listed in the spec.
- Produces a tested distributable JAR and publishes it to both registries.

- [x] **Step 1: Define the tag-only trigger and reproducible Java toolchain**

  Use `on.push.tags: ['v*']`, `runs-on: docker`, and
  fully qualified checkout/setup actions. Use the setup-java action to install
  Temurin 21, then install `curl` and `jq` before the validation step.

- [x] **Step 2: Validate tag and publishing configuration**

  Derive `VERSION=${FORGEJO_REF_NAME#v}`, require a non-empty semantic version,
  and fail if any required secret/variable is empty. Do not print secret values.

- [x] **Step 3: Build using the tag version**

  Run:

  ```bash
  ./gradlew clean test build --no-daemon --console=plain -Pmod_version="$VERSION"
  ```

  Select exactly `build/libs/cobblemon-economy-${VERSION}.jar` and reject a
  missing or ambiguous artifact.

- [x] **Step 4: Extract release notes safely**

  Extract the `Unreleased` section from `CHANGELOG.md`; if empty, write
  `Release ${FORGEJO_REF_NAME}`. Keep the notes in a temporary file only.

- [x] **Step 5: Upload to Modrinth**

  POST multipart data to `https://api.modrinth.com/v2/version` with the project
  ID, version, `release` channel, `fabric` loader, `1.21.1`, changelog, and the
  JAR. Use the token through an environment variable and `curl --fail`.

- [x] **Step 6: Upload to CurseForge**

  POST multipart metadata and the JAR to
  `https://minecraft.curseforge.com/api/projects/${CURSEFORGE_PROJECT_ID}/upload-file`.
  Parse `CURSEFORGE_GAME_VERSION_IDS` into the numeric `gameVersions` JSON
  array and use `X-Api-Token` from the secret.

### Task 2: Document setup and verify locally

**Files:**
- Modify: `CONTRIBUTING.md`
- Modify: `README.md`
- Modify: `tasks/todo.md`

- [x] **Step 1: Document Forgejo secrets and tag command**

  Document the required Forgejo secrets/variables and the release command:
  `git tag v<version> && git push origin v<version>`.

- [x] **Step 2: Validate workflow statically**

  Parse YAML, run `git diff --check`, verify no token-shaped literal is present,
  and confirm no `.github/workflows` file is created.

- [x] **Step 3: Run existing build verification**

  Run `./gradlew clean test build --no-daemon --console=plain`; do not call real
  publishing APIs from the local machine.

### Task 3: Commit and mirror

- [x] **Step 1: Commit the workflow and docs**

  Commit with `ci: publish tagged releases from Forgejo`.

- [x] **Step 2: Push the same commit to both configured push URLs**

  Run `git push origin main`, then verify GitHub and Forgejo expose the same
  commit SHA. A tag push remains a separate deliberate release action.
