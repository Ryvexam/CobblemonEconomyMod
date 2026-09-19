import { spawn } from "node:child_process";
import { access, mkdir, writeFile } from "node:fs/promises";
import { constants } from "node:fs";
import { randomUUID } from "node:crypto";
import { join } from "node:path";
import type { Readable } from "node:stream";
import { BoundedOutput, classifyOutput } from "./log-parser.js";
import type { ProcessHandle } from "./gradle-runner.js";
import type { RunResult } from "./types.js";

export interface MinecraftTestOptions {
  projectRoot: string;
  timeoutMs: number;
  task: "runServer" | "runGameTestServer";
  port?: number;
  runRoot?: string;
  processFactory?: MinecraftProcessFactory;
}

export type MinecraftProcessFactory = (command: string, args: string[], cwd: string) => ProcessHandle;

const READY_PATTERN = /Done \([^)]*\)! For help, type "help"/;

function defaultProcessFactory(command: string, args: string[], cwd: string): ProcessHandle {
  const child = spawn(command, args, { cwd, shell: false, detached: process.platform !== "win32" });
  return Object.assign(child, {
    killTree: (signal: NodeJS.Signals = "SIGTERM") => {
      if (process.platform !== "win32" && child.pid) {
        try {
          return process.kill(-child.pid, signal);
        } catch {
          return child.kill(signal);
        }
      }
      return child.kill(signal);
    }
  }) as unknown as ProcessHandle;
}

async function findWrapper(projectRoot: string): Promise<string> {
  const unix = join(projectRoot, "gradlew");
  try {
    await access(unix, constants.X_OK);
    return unix;
  } catch {
    const windows = join(projectRoot, "gradlew.bat");
    await access(windows, constants.F_OK);
    return windows;
  }
}

function collectStream(stream: Readable | null, output: BoundedOutput, onChunk: (value: string) => void): void {
  stream?.on("data", (chunk: Buffer | string) => {
    const value = chunk.toString();
    output.append(value);
    onChunk(value);
  });
}

export async function runMinecraftTest(options: MinecraftTestOptions): Promise<RunResult> {
  if (options.task !== "runServer" && options.task !== "runGameTestServer") {
    throw new Error(`Minecraft test task is not allowed: ${options.task}`);
  }

  const wrapper = await findWrapper(options.projectRoot);
  const runRoot = options.runRoot ?? join(options.projectRoot, ".minecraft-agent", "runs");
  await mkdir(runRoot, { recursive: true });
  const runId = `${Date.now()}-${randomUUID().slice(0, 8)}`;
  const logFile = join(runRoot, `${runId}.log`);
  const startedAt = Date.now();
  const stdout = new BoundedOutput();
  const stderr = new BoundedOutput();
  const readinessOutput = new BoundedOutput(4_096);
  const processFactory = options.processFactory ?? defaultProcessFactory;
  const args = ["--no-daemon", "--console=plain", options.task];
  if (options.port !== undefined) args.push(`-PminecraftAgentPort=${options.port}`);
  const process = processFactory(wrapper, args, options.projectRoot);

  return new Promise<RunResult>((resolve) => {
    let ready = false;
    let timedOut = false;
    let settled = false;
    let shutdownTimer: NodeJS.Timeout | undefined;
    let forceTimer: NodeJS.Timeout | undefined;

    const finish = (exitCode: number | null) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      if (shutdownTimer) clearTimeout(shutdownTimer);
      if (forceTimer) clearTimeout(forceTimer);
      const stdoutText = stdout.toString();
      const stderrText = stderr.toString();
      const classification = classifyOutput(stdoutText, stderrText, options.task);
      const phase = classification.phase !== "server_startup" && classification.phase !== "unknown"
        ? classification.phase
        : ready ? "functional_test" : "server_startup";
      const result: RunResult = {
        runId,
        status: timedOut ? "timeout" : ready && exitCode === 0 ? "success" : "process_failure",
        phase: timedOut ? "unknown" : phase,
        exitCode: timedOut ? null : exitCode,
        durationMs: Date.now() - startedAt,
        stdout: stdoutText,
        stderr: stderrText,
        errors: classification.errors,
        logFile
      };
      void writeFile(logFile, `${stdout}${stderr ? `\n--- stderr ---\n${stderr}` : ""}`, "utf8").then(() => resolve(result));
    };

    const timeout = setTimeout(() => {
      timedOut = true;
      process.kill("SIGTERM");
      forceTimer = setTimeout(() => {
        (process.killTree ?? process.kill.bind(process))("SIGKILL");
        finish(null);
      }, 250);
    }, options.timeoutMs);

    const onOutput = (value: string) => {
      readinessOutput.append(value);
      if (!ready && READY_PATTERN.test(readinessOutput.toString())) {
        ready = true;
        if (process.stdin && !process.stdin.destroyed) {
          process.stdin.write("stop\n");
          shutdownTimer = setTimeout(() => process.kill("SIGINT"), 1_000);
        } else {
          process.kill("SIGINT");
        }
        forceTimer = setTimeout(() => {
          (process.killTree ?? process.kill.bind(process))("SIGKILL");
          finish(null);
        }, 3_000);
      }
    };
    collectStream(process.stdout, stdout, onOutput);
    collectStream(process.stderr, stderr, onOutput);
    process.on("close", (...args: unknown[]) => {
      const [exitCode] = args;
      finish(typeof exitCode === "number" ? exitCode : null);
    });
    process.on("error", (...args: unknown[]) => {
      const [error] = args;
      stderr.append(error instanceof Error ? error.message : String(error));
      finish(1);
    });
  });
}
