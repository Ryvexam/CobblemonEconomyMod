import { readdir, readFile } from "node:fs/promises";
import { join } from "node:path";
import { resolveProjectRoot } from "../core/safety-policy.js";
import type { CommandIo, CommandRuntime } from "./runtime.js";

export async function logsCommand(runtime: CommandRuntime, io: CommandIo, project: string, runId: string | undefined, json: boolean): Promise<void> {
  const root = resolveProjectRoot(project, runtime.allowedRoots);
  const runs = join(root, ".minecraft-agent", "runs");
  const files = (await readdir(runs).catch(() => [])).filter((file) => file.endsWith(".log")).sort();
  const selected = runId ? `${runId}.log` : files.at(-1);
  if (!selected) throw new Error("No agent run logs found");
  const contents = await readFile(join(runs, selected), "utf8");
  io.stdout(json ? `${JSON.stringify({ runId: selected.replace(/\.log$/, ""), contents })}\n` : contents.endsWith("\n") ? contents : `${contents}\n`);
}
