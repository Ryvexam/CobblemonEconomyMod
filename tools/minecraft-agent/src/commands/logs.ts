import { readdir, readFile } from "node:fs/promises";
import { join } from "node:path";
import { resolveProjectRoot } from "../core/safety-policy.js";
import type { CommandIo, CommandRuntime } from "./runtime.js";

export async function logsCommand(runtime: CommandRuntime, io: CommandIo, project: string, runId: string | undefined, limit: number, json: boolean): Promise<void> {
  const root = resolveProjectRoot(project, runtime.allowedRoots);
  const runs = join(root, ".minecraft-agent", "runs");
  const files = (await readdir(runs).catch(() => [])).filter((file) => file.endsWith(".log")).sort();
  if (runId && !/^[A-Za-z0-9-]+$/.test(runId)) throw new Error("Invalid run ID");
  const selected = runId ? `${runId}.log` : files.at(-1);
  if (!selected || !files.includes(selected)) throw new Error("Requested run log was not found");
  const contents = (await readFile(join(runs, selected), "utf8")).slice(0, limit);
  const truncated = contents.length === limit;
  io.stdout(json
    ? `${JSON.stringify({ runId: selected.replace(/\.log$/, ""), contents, truncated })}\n`
    : `${contents}${truncated ? "\n[log truncated]\n" : contents.endsWith("\n") ? "" : "\n"}`);
}
