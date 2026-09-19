import { spawn } from "node:child_process";
import { mkdir, access, writeFile } from "node:fs/promises";
import { constants } from "node:fs";
import { randomUUID } from "node:crypto";
import { join } from "node:path";
import type { Readable } from "node:stream";
import { boundOutput, classifyOutput } from "./log-parser.js";
import type { RunResult } from "./types.js";

const ALLOWED_TASKS = new Set([
  "assemble",
  "build",
  "check",
  "clean",
  "runClient",
  "runGameTestServer",
  "runServer",
  "tasks",
  "test"
]);

export interface ProcessHandle {
  stdout: Readable | null;
  stderr: Readable | null;
  on(event: "close" | "error", listener: (...args: unknown[]) => void): ProcessHandle;
  kill(signal?: NodeJS.Signals): boolean;
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
  return spawn(command, args, { cwd, shell: false }) as unknown as ProcessHandle;
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

function collectStream(stream: Readable | null, chunks: string[]): void {
  stream?.on("data", (chunk: Buffer | string) => {
    chunks.push(chunk.toString());
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
  const stdoutChunks: string[] = [];
  const stderrChunks: string[] = [];
  const processFactory = options.processFactory ?? defaultProcessFactory;
  const process = processFactory(wrapper.command, ["--no-daemon", "--console=plain", options.task], options.projectRoot);

  collectStream(process.stdout, stdoutChunks);
  collectStream(process.stderr, stderrChunks);

  return new Promise<RunResult>((resolve) => {
    let timedOut = false;
    let settled = false;
    const timeout = setTimeout(() => {
      timedOut = true;
      process.kill("SIGTERM");
    }, options.timeoutMs);

    const finish = (exitCode: number | null) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      const stdout = boundOutput(stdoutChunks.join(""));
      const stderr = boundOutput(stderrChunks.join(""));
      const classification = classifyOutput(stdout, stderr, options.task);
      const result: RunResult = {
        runId,
        status: timedOut ? "timeout" : exitCode === 0 ? "success" : "process_failure",
        phase: classification.phase,
        exitCode: timedOut ? null : exitCode,
        durationMs: Date.now() - startedAt,
        stdout,
        stderr,
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
      stderrChunks.push(error instanceof Error ? error.message : String(error));
      finish(1);
    });
  });
}
