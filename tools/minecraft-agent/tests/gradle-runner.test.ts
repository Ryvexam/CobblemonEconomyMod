import { EventEmitter } from "node:events";
import { PassThrough } from "node:stream";
import { chmod, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { runGradle, type ProcessFactory } from "../src/core/gradle-runner.js";

const temporaryDirectories: string[] = [];

async function makeProject() {
  const root = await mkdtemp(join(tmpdir(), "minecraft-agent-gradle-"));
  temporaryDirectories.push(root);
  await writeFile(join(root, "gradlew"), "#!/bin/sh\nexit 0\n");
  await chmod(join(root, "gradlew"), 0o755);
  return root;
}

function fakeFactory(setup: (stdout: PassThrough, stderr: PassThrough, events: EventEmitter) => void): ProcessFactory {
  return () => {
    const stdout = new PassThrough();
    const stderr = new PassThrough();
    const events = new EventEmitter();
    setup(stdout, stderr, events);
    return {
      stdout,
      stderr,
      on: (event, listener) => {
        events.on(event, listener);
        return this;
      },
      kill: () => {
        events.emit("close", null, "SIGTERM");
        return true;
      }
    };
  };
}

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })));
});

describe("Gradle runner", () => {
  it("returns a successful result and persists the log", async () => {
    const projectRoot = await makeProject();
    const result = await runGradle({
      projectRoot,
      task: "build",
      timeoutMs: 1_000,
      runRoot: join(projectRoot, ".runs"),
      processFactory: fakeFactory((stdout, stderr, events) => {
        setImmediate(() => {
          stdout.end("BUILD SUCCESSFUL in 1s\n");
          stderr.end();
          events.emit("close", 0, null);
        });
      })
    });

    expect(result.status).toBe("success");
    expect(result.exitCode).toBe(0);
    expect(result.stdout).toContain("BUILD SUCCESSFUL");
    expect(await readFile(result.logFile, "utf8")).toContain("BUILD SUCCESSFUL");
  });

  it("returns a classified process failure", async () => {
    const projectRoot = await makeProject();
    const result = await runGradle({
      projectRoot,
      task: "build",
      timeoutMs: 1_000,
      runRoot: join(projectRoot, ".runs"),
      processFactory: fakeFactory((stdout, stderr, events) => {
        setImmediate(() => {
          stdout.end("Compilation failed\n");
          stderr.end("error: invalid source\n");
          events.emit("close", 1, null);
        });
      })
    });

    expect(result.status).toBe("process_failure");
    expect(result.exitCode).toBe(1);
    expect(result.phase).toBe("compilation");
  });

  it("returns timeout with partial output instead of compilation failure", async () => {
    const projectRoot = await makeProject();
    const result = await runGradle({
      projectRoot,
      task: "build",
      timeoutMs: 10,
      runRoot: join(projectRoot, ".runs"),
      processFactory: fakeFactory((stdout) => {
        stdout.write("Resolving dependencies...\n");
      })
    });

    expect(result.status).toBe("timeout");
    expect(result.exitCode).toBeNull();
    expect(result.phase).not.toBe("compilation");
    expect(result.stdout).toContain("Resolving dependencies");
  });

  it("forces an unresponsive process to end and keeps timeout phase unknown", async () => {
    const projectRoot = await makeProject();
    const signals: NodeJS.Signals[] = [];
    const stdout = new PassThrough();
    const stderr = new PassThrough();
    const events = new EventEmitter();
    const result = await runGradle({
      projectRoot,
      task: "build",
      timeoutMs: 10,
      runRoot: join(projectRoot, ".runs"),
      processFactory: () => ({
        stdout,
        stderr,
        on: (event: "close" | "error", listener: (...args: unknown[]) => void) => {
          events.on(event, listener);
          return this;
        },
        kill: (signal: NodeJS.Signals = "SIGTERM") => {
          signals.push(signal);
          return true;
        }
      })
    });
    stdout.write("Compilation failed before the process became unresponsive\n");

    expect(result.status).toBe("timeout");
    expect(result.phase).toBe("unknown");
    expect(result.exitCode).toBeNull();
    expect(signals).toEqual(["SIGTERM", "SIGKILL"]);
  });
});
