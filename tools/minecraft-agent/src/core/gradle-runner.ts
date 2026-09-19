import { spawn } from "node:child_process";
import { mkdir, access, writeFile } from "node:fs/promises";
import { constants } from "node:fs";
import { randomUUID } from "node:crypto";
import { join } from "node:path";
import type { Readable, Writable } from "node:stream";
import { BoundedOutput, classifyOutput } from "./log-parser.js";
import type { RunResult } from "./types.js";

const ALLOWED_TASKS = new Set([
  "assemble",
  "build",
  "check",
  "jar",
  "runClient",
  "runGameTestServer",
  "runServer",
  "tasks",
  "test"
]);

export interface ProcessHandle {
  stdout: Readable | null;
  stderr: Readable | null;
  stdin?: Writable | null;
  on(event: "close" | "error", listener: (...args: unknown[]) => void): ProcessHandle;
  kill(signal?: NodeJS.Signals): boolean;
  killTree?: (signal?: NodeJS.Signals) => boolean;
}

export type ProcessFactory = (command: string, args: string[], cwd: string) => ProcessHandle;

export interface GradleRunOptions {
  projectRoot: string;
  task: string;
  timeoutMs: number;
  runRoot: string;
  processFactory?: ProcessFactory;
}

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

async function findWrapper(projectRoot: string): Promise<{ command: string; args: string[] }> {
  const unix = join(projectRoot, "gradlew");
  try {
    await access(unix, constants.X_OK);
    return { command: unix, args: [] };
  } catch {
    const windows = join(projectRoot, "gradlew.bat");
    await access(windows, constants.F_OK);
    return { command: windows, args: [] };
  }
}

function collectStream(stream: Readable | null, output: BoundedOutput): void {
  stream?.on("data", (chunk: Buffer | string) => {
    output.append(chunk.toString());
  });
}

export async function runGradle(options: GradleRunOptions): Promise<RunResult> {
  if (!ALLOWED_TASKS.has(options.task)) {
    throw new Error(`Gradle task is not allowed: ${options.task}`);
  }

  const wrapper = await findWrapper(options.projectRoot);
  await mkdir(options.runRoot, { recursive: true });
  const runId = `${Date.now()}-${randomUUID().slice(0, 8)}`;
  const logFile = join(options.runRoot, `${runId}.log`);
  const startedAt = Date.now();
  const stdout = new BoundedOutput();
  const stderr = new BoundedOutput();
  const processFactory = options.processFactory ?? defaultProcessFactory;
  const process = processFactory(wrapper.command, ["--no-daemon", "--console=plain", options.task], options.projectRoot);

  collectStream(process.stdout, stdout);
  collectStream(process.stderr, stderr);

  return new Promise<RunResult>((resolve) => {
    let timedOut = false;
    let settled = false;
    let forceTimer: NodeJS.Timeout | undefined;
    const timeout = setTimeout(() => {
      timedOut = true;
      process.kill("SIGTERM");
      forceTimer = setTimeout(() => {
        (process.killTree ?? process.kill.bind(process))("SIGKILL");
        finish(null);
      }, 250);
    }, options.timeoutMs);

    const finish = (exitCode: number | null) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      if (forceTimer) clearTimeout(forceTimer);
      const stdoutText = stdout.toString();
      const stderrText = stderr.toString();
      const classification = classifyOutput(stdoutText, stderrText, options.task);
      const result: RunResult = {
        runId,
        status: timedOut ? "timeout" : exitCode === 0 ? "success" : "process_failure",
        phase: timedOut ? "unknown" : classification.phase,
        exitCode: timedOut ? null : exitCode,
        durationMs: Date.now() - startedAt,
        stdout: stdoutText,
        stderr: stderrText,
        errors: classification.errors,
        logFile
      };
      void writeFile(logFile, `${stdout}${stderr ? `\n--- stderr ---\n${stderr}` : ""}`, "utf8").then(() => resolve(result));
    };

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
