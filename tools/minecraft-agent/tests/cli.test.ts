import { mkdir, mkdtemp, realpath, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { runCli, type CliDependencies, type CliIo } from "../src/cli.js";
import type { RunResult } from "../src/core/types.js";

const temporaryDirectories: string[] = [];

async function makeProject() {
  const root = await mkdtemp(join(tmpdir(), "minecraft-agent-cli-"));
  temporaryDirectories.push(root);
  await writeFile(join(root, "gradle.properties"), "minecraft_version=1.21.1\n");
  await writeFile(join(root, "build.gradle.kts"), 'plugins { id("fabric-loom") version "1.7-SNAPSHOT" }');
  await writeFile(join(root, "gradlew"), "#!/bin/sh\nexit 0\n");
  await mkdir(join(root, "src/main/resources"), { recursive: true });
  await writeFile(join(root, "src/main/resources/fabric.mod.json"), JSON.stringify({ id: "example-mod" }));
  return root;
}

function io(): CliIo & { output: string[]; errors: string[] } {
  const output: string[] = [];
  const errors: string[] = [];
  return { output, errors, stdout: (value) => output.push(value), stderr: (value) => errors.push(value) };
}

function result(status: RunResult["status"] = "success"): RunResult {
  return { runId: "test-run", status, phase: "packaging", exitCode: status === "success" ? 0 : 1, durationMs: 1, stdout: "BUILD SUCCESSFUL", stderr: "", errors: [], logFile: "/tmp/test-run.log" };
}

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })));
});

describe("minecraft-agent CLI", () => {
  it("prints inspect JSON", async () => {
    const project = await makeProject();
    const output = io();

    const code = await runCli(["inspect", "--project", project, "--json"], { allowedRoots: [project] }, output);

    expect(code).toBe(0);
    expect(JSON.parse(output.output.join(""))).toMatchObject({ loader: "fabric", minecraftVersion: "1.21.1", modIds: ["example-mod"] });
  });

  it("forwards build project and task and prints the run result", async () => {
    const project = await makeProject();
    const output = io();
    let received: { projectRoot: string; task: string } | undefined;
    const dependencies: Partial<CliDependencies> = {
      runGradle: async (options) => {
        received = { projectRoot: options.projectRoot, task: options.task };
        return result();
      }
    };

    const code = await runCli(["build", "--project", project, "--task", "check", "--json"], { allowedRoots: [project], ...dependencies }, output);

    expect(code).toBe(0);
    expect(received).toEqual({ projectRoot: await realpath(project), task: "check" });
    expect(JSON.parse(output.output.join(""))).toMatchObject({ status: "success", runId: "test-run" });
  });

  it("prints artifact hashes", async () => {
    const project = await makeProject();
    await mkdir(join(project, "build/libs"), { recursive: true });
    await writeFile(join(project, "build/libs/example.jar"), "jar");
    const output = io();

    const code = await runCli(["artifact", "--project", project, "--json"], { allowedRoots: [project] }, output);

    expect(code).toBe(0);
    expect(JSON.parse(output.output.join(""))[0]).toMatchObject({ fileName: "example.jar", size: 3 });
  });

  it("initializes a project and refuses a non-empty target", async () => {
    const root = await mkdtemp(join(tmpdir(), "minecraft-agent-init-"));
    temporaryDirectories.push(root);
    const target = join(root, "new-mod");
    const output = io();

    expect(await runCli(["init", target, "--loader", "fabric", "--mc", "1.21.1", "--mod-id", "new-mod", "--package", "com.example.newmod"], { allowedRoots: [root] }, output)).toBe(0);
    expect(await runCli(["init", target, "--loader", "fabric", "--mc", "1.21.1", "--mod-id", "new-mod", "--package", "com.example.newmod"], { allowedRoots: [root] }, output)).toBe(1);
    expect(output.errors.join(" ")).toMatch(/non-empty/i);
  });

  it("forwards the Minecraft test task and timeout", async () => {
    const project = await makeProject();
    const output = io();
    let received: { task: string; timeoutMs: number } | undefined;
    const dependencies: Partial<CliDependencies> & { runMinecraftTest: NonNullable<CliDependencies["runMinecraftTest"]> } = {
      runMinecraftTest: async (options) => {
        received = { task: options.task, timeoutMs: options.timeoutMs };
        return result();
      }
    };

    const code = await runCli(["test", "--project", project, "--task", "runGameTestServer", "--timeout", "321", "--json"], { allowedRoots: [project], ...dependencies }, output);

    expect(code).toBe(0);
    expect(received).toEqual({ task: "runGameTestServer", timeoutMs: 321 });
    expect(JSON.parse(output.output.join(""))).toMatchObject({ status: "success" });
  });

  it("rejects traversal in the logs run ID", async () => {
    const project = await makeProject();
    await mkdir(join(project, ".minecraft-agent/runs"), { recursive: true });
    await writeFile(join(project, ".minecraft-agent/runs/known.log"), "known log\n");
    const output = io();

    const code = await runCli(["logs", "--project", project, "--since", "../../outside", "--json"], { allowedRoots: [project] }, output);

    expect(code).toBe(1);
    expect(output.errors.join(" ")).toMatch(/run ID|not found/i);
  });

  it("rejects invalid timeout values before starting Gradle", async () => {
    const project = await makeProject();
    const output = io();
    const code = await runCli(["build", "--project", project, "--timeout", "NaN", "--json"], { allowedRoots: [project] }, output);

    expect(code).toBe(1);
    expect(output.errors.join(" ")).toMatch(/timeout/i);
  });
});
