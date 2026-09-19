# Local Minecraft Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local TypeScript CLI and MCP server that let OpenCode inspect, initialize, build, test, diagnose, and package Minecraft mod projects.

**Architecture:** A shared `tools/minecraft-agent` domain engine exposes project inspection, path safety, Gradle execution, log classification, and artifact hashing. A Commander CLI and an MCP stdio server are thin adapters over that engine; OpenCode remains responsible for reasoning and applying source changes.

**Tech Stack:** Node.js 20+, TypeScript, Commander, `@modelcontextprotocol/sdk`, Zod, Vitest, Gradle wrappers, Fabric 1.21.1 template.

**Spec:** `docs/superpowers/specs/2026-09-19-local-minecraft-agent-design.md`

## Global Constraints

- The tool is local-only for the MVP; no cloud, accounts, or remote server deployment.
- The existing Gradle mod build must remain independent from Node dependencies.
- The engine must distinguish dependency resolution, Gradle/Loom configuration, compilation, server startup, mod loading, functional testing, and packaging.
- Project and output paths must stay inside explicitly allowed roots.
- The agent must not receive an arbitrary shell tool through MCP.
- CLI output must have human and stable JSON modes; failures must return non-zero exit codes.
- Every execution must receive a `run_id` and persist bounded logs locally.
- The Fabric 1.21.1 template must be explicit and versioned.
- A timeout must never be reported as a compilation error without evidence.

## Review Focus

- A path outside the configured project root must be rejected before any filesystem or process operation; covered in Task 2 path-policy tests.
- A project with missing or conflicting loader/version metadata must return `unknown`/diagnostic data rather than silently selecting a template; covered in Task 2 inspector tests.
- A Gradle timeout must preserve partial output and classify as `timeout`, not `compile`; covered in Task 3 runner tests.
- A process that emits a non-zero exit code without a known error pattern must remain `process_failure` with raw bounded output; covered in Task 3 parser tests.
- MCP input must reject unknown paths, unsupported templates, and oversized output requests; covered in Task 6 schema tests.

---

### Task 1: Scaffold the isolated TypeScript package

**Files:**
- Create: `tools/minecraft-agent/package.json`
- Create: `tools/minecraft-agent/tsconfig.json`
- Create: `tools/minecraft-agent/vitest.config.ts`
- Create: `tools/minecraft-agent/src/index.ts`
- Create: `tools/minecraft-agent/tests/smoke.test.ts`
- Modify: `tools/minecraft-agent/README.md`

**Interfaces:**
- Produces the package scripts `test`, `test:watch`, `build`, `cli`, and `mcp` used by later tasks.
- Exposes a package-local test command that works without touching the Gradle build.

- [ ] **Step 1: Create the package manifest and compiler configuration**

  Add Node ESM metadata, scripts using `tsx`, `tsc`, and Vitest, and dependencies for Commander, MCP SDK, Zod, and Vitest. Set TypeScript to strict mode, NodeNext module resolution, and output to `dist`.

- [ ] **Step 2: Add the smoke test before implementation**

  Create `tests/smoke.test.ts` importing `packageName` from `src/index.ts` and asserting it equals `minecraft-agent`.

- [ ] **Step 3: Run the package test and confirm the initial failure**

  Run `npm install` then `npm test -- --run tests/smoke.test.ts` from `tools/minecraft-agent`. Expected initial failure: `packageName` is not exported.

- [ ] **Step 4: Add the minimal package export and README usage**

  Export `const packageName = "minecraft-agent"` and document local commands:

  ```bash
  npm install
  npm run build
  npm test
  npm run cli -- inspect --project ../..
  npm run mcp
  ```

- [ ] **Step 5: Run the package checks**

  Run `npm test -- --run` and `npm run build`. Both must exit 0.

### Task 2: Implement project inspection and path safety

**Files:**
- Create: `tools/minecraft-agent/src/core/types.ts`
- Create: `tools/minecraft-agent/src/core/safety-policy.ts`
- Create: `tools/minecraft-agent/src/core/project-inspector.ts`
- Create: `tools/minecraft-agent/tests/safety-policy.test.ts`
- Create: `tools/minecraft-agent/tests/project-inspector.test.ts`

**Interfaces:**
- Produces `ProjectInspection`, `Loader`, `resolveProjectRoot`, `assertAllowedPath`, and `inspectProject` for all later adapters.

  ```ts
  type Loader = "fabric" | "forge" | "neoforge" | "unknown";

  interface ProjectInspection {
    root: string;
    loader: Loader;
    minecraftVersion: string | null;
    javaVersion: string | null;
    modIds: string[];
    gradleWrapper: string | null;
    gradleTasks: string[];
    sourceFiles: string[];
    diagnostics: string[];
  }

  function resolveProjectRoot(project: string | undefined, allowedRoots: string[]): string;
  function assertAllowedPath(target: string, allowedRoots: string[]): string;
  async function inspectProject(root: string): Promise<ProjectInspection>;
  ```

- [ ] **Step 1: Write failing safety tests**

  Cover a valid nested project, a sibling path escape (`../outside`), a path-prefix collision (`allowed-other`), and a missing path. Assert that invalid inputs throw `ProjectPathError` with the rejected absolute path.

- [ ] **Step 2: Run the safety tests and verify they fail**

  Run `npm test -- --run tests/safety-policy.test.ts`. Expected failure: missing `resolveProjectRoot`/`assertAllowedPath`.

- [ ] **Step 3: Implement canonical path validation**

  Resolve all paths with `realpath` when present, use `path.relative` to test containment, reject empty roots and paths outside every root, and return normalized absolute paths.

- [ ] **Step 4: Write failing inspector tests**

  Create fixture projects under `tests/fixtures/fabric-project` containing Fabric metadata, `minecraft_version=1.21.1`, `fabric.mod.json`, and a Gradle wrapper marker. Assert Fabric detection, version detection, mod ID, wrapper detection, and diagnostics for an empty project and conflicting Forge/Fabric markers.

- [ ] **Step 5: Implement metadata inspection**

  Read `gradle.properties`, `build.gradle(.kts)`, `settings.gradle(.kts)`, and `src/main/resources/fabric.mod.json` if present. Detect loader from dependency/plugin markers, extract Minecraft version from properties, collect mod IDs, list source files, and report missing metadata without invoking Gradle.

- [ ] **Step 6: Run the inspection test suite**

  Run `npm test -- --run tests/safety-policy.test.ts tests/project-inspector.test.ts` and confirm all tests pass.

### Task 3: Implement the bounded Gradle runner, logs, and artifacts

**Files:**
- Create: `tools/minecraft-agent/src/core/gradle-runner.ts`
- Create: `tools/minecraft-agent/src/core/log-parser.ts`
- Create: `tools/minecraft-agent/src/core/artifact-manager.ts`
- Create: `tools/minecraft-agent/tests/log-parser.test.ts`
- Create: `tools/minecraft-agent/tests/gradle-runner.test.ts`
- Create: `tools/minecraft-agent/tests/artifact-manager.test.ts`

**Interfaces:**
- Produces:

  ```ts
  type RunPhase = "dependency_resolution" | "gradle_configuration" | "compilation" | "server_startup" | "mod_loading" | "functional_test" | "packaging" | "unknown";
  type RunStatus = "success" | "process_failure" | "timeout";

  interface RunResult {
    runId: string;
    status: RunStatus;
    phase: RunPhase;
    exitCode: number | null;
    durationMs: number;
    stdout: string;
    stderr: string;
    errors: string[];
    logFile: string;
  }

  interface GradleRunOptions {
    projectRoot: string;
    task: string;
    timeoutMs: number;
    runRoot: string;
  }

  async function runGradle(options: GradleRunOptions): Promise<RunResult>;
  function classifyOutput(stdout: string, stderr: string, task: string): { phase: RunPhase; errors: string[] };
  async function listArtifacts(projectRoot: string): Promise<ArtifactInfo[]>;
  async function copyArtifact(projectRoot: string, fileName: string, outputRoot: string): Promise<ArtifactInfo>;
  ```

- [ ] **Step 1: Write parser tests first**

  Feed output containing `Could not resolve`, `FAILURE: Build failed`, `Compilation failed`, `Mixin apply failed`, and a plain non-zero message. Assert the expected phase/error class. Feed a timeout marker and assert the parser does not classify it as compilation.

- [ ] **Step 2: Implement bounded output and phase classification**

  Normalize output, retain at most 200,000 characters per stream, extract the first 20 relevant error lines, and classify using ordered patterns so dependency and Loom errors are not mislabeled as Java compilation errors.

- [ ] **Step 3: Write runner tests with an injected process factory**

  Test a successful fake process, a non-zero process with logs, and a process that exceeds the timeout. Assert run ID, exit code, status, phase, partial output, and persisted log file.

- [ ] **Step 4: Implement the Gradle runner**

  Select `./gradlew` or `gradlew.bat`, invoke only the requested allowlisted task with `--no-daemon --console=plain`, stream output to a run file, terminate on timeout, and return the structured result. Do not report a timeout as a compile failure.

- [ ] **Step 5: Write and implement artifact tests**

  Create temporary `build/libs` files, assert that only `.jar` files are listed, verify SHA-256, reject traversal in `fileName`, and copy into an allowed output directory.

- [ ] **Step 6: Run all core tests**

  Run `npm test -- --run tests/log-parser.test.ts tests/gradle-runner.test.ts tests/artifact-manager.test.ts`. Confirm all pass and run `npm run build`.

### Task 4: Add the CLI adapters and Fabric project template

**Files:**
- Create: `tools/minecraft-agent/src/cli.ts`
- Create: `tools/minecraft-agent/src/commands/inspect.ts`
- Create: `tools/minecraft-agent/src/commands/build.ts`
- Create: `tools/minecraft-agent/src/commands/logs.ts`
- Create: `tools/minecraft-agent/src/commands/artifact.ts`
- Create: `tools/minecraft-agent/src/commands/init.ts`
- Create: `tools/minecraft-agent/src/core/template-manager.ts`
- Create: `tools/minecraft-agent/templates/fabric-1.21.1/settings.gradle.kts`
- Create: `tools/minecraft-agent/templates/fabric-1.21.1/build.gradle.kts`
- Create: `tools/minecraft-agent/templates/fabric-1.21.1/gradle.properties`
- Create: `tools/minecraft-agent/templates/fabric-1.21.1/src/main/java/example/ExampleMod.java`
- Create: `tools/minecraft-agent/templates/fabric-1.21.1/src/main/resources/fabric.mod.json`
- Create: `tools/minecraft-agent/templates/fabric-1.21.1/README.md`
- Create: `tools/minecraft-agent/tests/cli.test.ts`
- Create: `tools/minecraft-agent/tests/template-manager.test.ts`

**Interfaces:**
- The CLI calls the Task 2/3 engine functions and emits JSON matching `ProjectInspection`, `RunResult`, and `ArtifactInfo`.
- `init` accepts only `fabric` + `1.21.1` in the MVP and returns the created absolute path.

- [ ] **Step 1: Write command tests with temporary fixtures**

  Assert `inspect --json` returns valid JSON, `build --json` forwards the selected project and task, `artifact --json` returns hashes, and `init` creates a new empty target but refuses a non-empty target.

- [ ] **Step 2: Implement the template manager**

  Copy only files from the selected template, substitute the mod ID and package name after validating Java identifiers, create parent directories, and refuse overwrite unless the target is empty.

- [ ] **Step 3: Implement the CLI commands**

  Register `init`, `inspect`, `build`, `logs`, and `artifact` with Commander. Use `--project`, `--json`, `--timeout`, `--task`, and `--output` flags. Print errors to stderr and set `process.exitCode = 1` rather than returning success-shaped JSON for failures.

- [ ] **Step 4: Add the package executable and documentation**

  Point the `minecraft-agent` bin entry to the built CLI, document local invocation with `npm run cli --`, and document that `runServer`/game tests require the corresponding Minecraft dependencies and Java runtime.

- [ ] **Step 5: Run CLI tests and a real inspection**

  Run `npm test -- --run tests/cli.test.ts tests/template-manager.test.ts`, then `npm run build`, then `npm run cli -- inspect --project ../.. --json`. The real inspection must identify Fabric 1.21.1 and `cobblemon-economy` without modifying the repository.

### Task 5: Add controlled Minecraft test execution

**Files:**
- Create: `tools/minecraft-agent/src/core/minecraft-runner.ts`
- Create: `tools/minecraft-agent/src/commands/test.ts`
- Create: `tools/minecraft-agent/tests/minecraft-runner.test.ts`
- Modify: `tools/minecraft-agent/src/core/types.ts`
- Modify: `tools/minecraft-agent/src/cli.ts`

**Interfaces:**

  ```ts
  interface MinecraftTestOptions {
    projectRoot: string;
    timeoutMs: number;
    task: "runServer" | "runGameTestServer";
    port?: number;
  }

  async function runMinecraftTest(options: MinecraftTestOptions): Promise<RunResult>;
  ```

- [ ] **Step 1: Write lifecycle tests with an injected process**

  Test readiness detection from `Done (...)! For help, type "help"`, graceful termination after readiness, timeout before readiness, and a process that reports a mod-loading exception.

- [ ] **Step 2: Implement the controlled runner**

  Reuse `runGradle` process primitives, select only the two allowlisted tasks, detect readiness from output, preserve logs, send a graceful stop signal, and force-kill after a short shutdown grace period.

- [ ] **Step 3: Implement the `test` CLI command**

  Add `--task`, `--timeout`, and `--port`; default to `runServer`; return the same structured run result and non-zero status on timeout or test failure.

- [ ] **Step 4: Run lifecycle tests and a guarded project test**

  Run `npm test -- --run tests/minecraft-runner.test.ts`. Run the real project test only if `./gradlew tasks` confirms the selected task is available; otherwise return a documented precondition result instead of hanging.

### Task 6: Add the MCP stdio server and OpenCode integration

**Files:**
- Create: `tools/minecraft-agent/src/mcp.ts`
- Create: `tools/minecraft-agent/src/mcp-tools/inspect-project.ts`
- Create: `tools/minecraft-agent/src/mcp-tools/create-project.ts`
- Create: `tools/minecraft-agent/src/mcp-tools/build-project.ts`
- Create: `tools/minecraft-agent/src/mcp-tools/test-project.ts`
- Create: `tools/minecraft-agent/src/mcp-tools/read-logs.ts`
- Create: `tools/minecraft-agent/src/mcp-tools/get-artifact.ts`
- Create: `tools/minecraft-agent/tests/mcp.test.ts`
- Modify: `tools/minecraft-agent/package.json`
- Modify: `tools/minecraft-agent/README.md`
- Modify: `tasks/todo.md`

**Interfaces:**
- MCP tools accept Zod-validated JSON and return `CallToolResult` content with one bounded JSON payload.
- Tool handlers call the same core functions as the CLI; no handler may spawn a raw user-provided command.

- [ ] **Step 1: Write MCP schema tests**

  Assert valid `inspect_project`, `build_project`, and `get_artifact` arguments are accepted, unsupported template/version is rejected, a path outside the allowed root is rejected, and oversized output options are capped.

- [ ] **Step 2: Implement the stdio MCP server**

  Create an MCP server named `minecraft-agent`, register the six tools with descriptions and Zod input schemas, resolve the default allowed root from the current working directory, and connect with `StdioServerTransport`.

- [ ] **Step 3: Wire the MCP package script**

  Set `npm run mcp` to execute the built `dist/mcp.js`; keep stdout reserved for MCP protocol traffic and send diagnostics to stderr.

- [ ] **Step 4: Add OpenCode configuration instructions**

  Document a project-local MCP command using the absolute path to `tools/minecraft-agent/dist/mcp.js`, explain `npm install && npm run build`, and avoid writing a machine-specific absolute path into the repository config.

- [ ] **Step 5: Run MCP tests and protocol startup verification**

  Run `npm test -- --run tests/mcp.test.ts`, `npm run build`, and start `npm run mcp` with stdin closed. It must exit cleanly or wait for protocol input without writing non-protocol data to stdout.

### Task 7: Final integration, documentation, and review

**Files:**
- Modify: `tools/minecraft-agent/README.md`
- Modify: `README.md`
- Modify: `tasks/todo.md`

- [ ] **Step 1: Document the complete local workflow**

  Add installation, CLI commands, OpenCode MCP configuration, supported template, timeout behavior, artifact locations, and troubleshooting for Gradle dependency resolution.

- [ ] **Step 2: Run the complete verification matrix**

  Run from `tools/minecraft-agent`:

  ```bash
  npm test -- --run
  npm run build
  npm run cli -- inspect --project ../.. --json
  npm run cli -- artifact --project ../.. --json
  ```

  Then run `git diff --check` and inspect the final status. Run `./gradlew build --no-daemon --console=plain` with the documented timeout and report its actual result without converting a timeout into a false success.

- [ ] **Step 3: Mark the plan and review evidence**

  Record exact commands and outcomes in `tasks/todo.md`, including any unresolved Gradle/network precondition. Do not claim the end-to-end Minecraft test passed unless its process exited successfully and the logs show the mod loaded.

- [ ] **Step 4: Commit the implementation**

  Commit the tool and documentation as `feat: add local Minecraft agent CLI and MCP` after all package tests pass and the diff has been reviewed.
