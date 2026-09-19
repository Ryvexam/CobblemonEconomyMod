import { spawn } from "node:child_process";
import { access, mkdir, writeFile } from "node:fs/promises";
import { constants } from "node:fs";
import { randomUUID } from "node:crypto";
import { join } from "node:path";
import type { Readable } from "node:stream";
import { boundOutput, classifyOutput } from "./log-parser.js";
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
  return spawn(command, args, { cwd, shell: false }) as unknown as ProcessHandle;
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

function collectStream(stream: Readable | null, chunks: string[], onChunk: (value: string) => void): void {
  stream?.on("data", (chunk: Buffer | string) => {
    const value = chunk.toString();
    chunks.push(value);
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
  const stdoutChunks: string[] = [];
  const stderrChunks: string[] = [];
  const processFactory = options.processFactory ?? defaultProcessFactory;
  const args = ["--no-daemon", "--console=plain", options.task];
  if (options.port !== undefined) args.push(`-PminecraftAgentPort=${options.port}`);
  const process = processFactory(wrapper, args, options.projectRoot);

  return new Promise<RunResult>((resolve) => {
    let ready = false;
    let timedOut = false;
    let settled = false;
    let shutdownTimer: NodeJS.Timeout | undefined;

    const finish = (exitCode: number | null) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      if (shutdownTimer) clearTimeout(shutdownTimer);
      const stdout = boundOutput(stdoutChunks.join(""));
      const stderr = boundOutput(stderrChunks.join(""));
      const classification = classifyOutput(stdout, stderr, options.task);
      const phase = classification.phase !== "server_startup" && classification.phase !== "unknown"
        ? classification.phase
        : ready ? "functional_test" : "server_startup";
      const result: RunResult = {
        runId,
        status: timedOut ? "timeout" : ready && exitCode === 0 ? "success" : "process_failure",
        phase,
        exitCode: timedOut ? null : exitCode,
        durationMs: Date.now() - startedAt,
        stdout,
        stderr,
        errors: classification.errors,
        logFile
      };
      void writeFile(logFile, `${stdout}${stderr ? `\n--- stderr ---\n${stderr}` : ""}`, "utf8").then(() => resolve(result));
    };

    const timeout = setTimeout(() => {
      timedOut = true;
      process.kill("SIGTERM");
      shutdownTimer = setTimeout(() => process.kill("SIGKILL"), 250);
    }, options.timeoutMs);

    const onOutput = (value: string) => {
      if (!ready && READY_PATTERN.test(value)) {
        ready = true;
        process.kill("SIGINT");
        shutdownTimer = setTimeout(() => process.kill("SIGKILL"), 3_000);
      }
    };
    collectStream(process.stdout, stdoutChunks, onOutput);
    collectStream(process.stderr, stderrChunks, onOutput);
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
