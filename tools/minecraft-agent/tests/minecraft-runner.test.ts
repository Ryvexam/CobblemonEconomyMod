import { EventEmitter } from "node:events";
import { PassThrough } from "node:stream";
import { chmod, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { runMinecraftTest, type MinecraftProcessFactory } from "../src/core/minecraft-runner.js";

const temporaryDirectories: string[] = [];

async function makeProject() {
  const root = await mkdtemp(join(tmpdir(), "minecraft-agent-test-"));
  temporaryDirectories.push(root);
  await writeFile(join(root, "gradlew"), "#!/bin/sh\nexit 0\n");
  await chmod(join(root, "gradlew"), 0o755);
  return root;
}

function fakeFactory(setup: (stdout: PassThrough, stderr: PassThrough, events: EventEmitter, signals: NodeJS.Signals[]) => void): MinecraftProcessFactory {
  return () => {
    const stdout = new PassThrough();
    const stderr = new PassThrough();
    const events = new EventEmitter();
    const signals: NodeJS.Signals[] = [];
    setup(stdout, stderr, events, signals);
    return {
      stdout,
      stderr,
      on: (event, listener) => {
        events.on(event, listener);
        return this;
      },
      kill: (signal = "SIGTERM") => {
        signals.push(signal);
        if (signal !== "SIGKILL") setImmediate(() => events.emit("close", 0, signal));
        return true;
      }
    };
  };
}

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })));
});

describe("Minecraft lifecycle runner", () => {
  it("stops gracefully after the server reports readiness", async () => {
    const projectRoot = await makeProject();
    const result = await runMinecraftTest({
      projectRoot,
      task: "runServer",
      timeoutMs: 1_000,
      runRoot: join(projectRoot, ".runs"),
      processFactory: fakeFactory((stdout) => {
        setImmediate(() => {
          stdout.end('Done (1.234s)! For help, type "help"\n');
        });
      })
    });

    expect(result.status).toBe("success");
    expect(result.phase).toBe("functional_test");
    expect(result.stdout).toContain("Done");
    expect(await readFile(result.logFile, "utf8")).toContain("For help");
  });

  it("times out before readiness and preserves partial output", async () => {
    const projectRoot = await makeProject();
    const result = await runMinecraftTest({
      projectRoot,
      task: "runGameTestServer",
      timeoutMs: 10,
      runRoot: join(projectRoot, ".runs"),
      processFactory: fakeFactory((stdout) => {
        stdout.write("Loading Minecraft server...\n");
      })
    });

    expect(result.status).toBe("timeout");
    expect(result.exitCode).toBeNull();
    expect(result.stdout).toContain("Loading Minecraft server");
  });

  it("classifies a mod-loading exception as a failed test", async () => {
    const projectRoot = await makeProject();
    const result = await runMinecraftTest({
      projectRoot,
      task: "runServer",
      timeoutMs: 1_000,
      runRoot: join(projectRoot, ".runs"),
      processFactory: fakeFactory((stdout, stderr, events) => {
        setImmediate(() => {
          stderr.end("ModLoadingException: failed to load mod cobblemon-economy\n");
          events.emit("close", 1, null);
        });
      })
    });

    expect(result.status).toBe("process_failure");
    expect(result.phase).toBe("mod_loading");
    expect(result.errors.join(" ")).toMatch(/ModLoadingException/);
  });

  it("classifies Fabric dependency incompatibility as mod loading", async () => {
    const projectRoot = await makeProject();
    const result = await runMinecraftTest({
      projectRoot,
      task: "runServer",
      timeoutMs: 1_000,
      runRoot: join(projectRoot, ".runs"),
      processFactory: fakeFactory((stdout, stderr, events) => {
        setImmediate(() => {
          stdout.end("Mod resolution failed\nIncompatible mods found!\n");
          stderr.end("FormattedException: Some of your mods are incompatible with the game or each other!\n");
          events.emit("close", 1, null);
        });
      })
    });

    expect(result.status).toBe("process_failure");
    expect(result.phase).toBe("mod_loading");
  });

  it("rejects tasks outside the Minecraft test allowlist", async () => {
    const projectRoot = await makeProject();

    await expect(runMinecraftTest({
      projectRoot,
      task: "build" as "runServer",
      timeoutMs: 100,
      runRoot: join(projectRoot, ".runs")
    })).rejects.toThrow(/not allowed/i);
  });
});
