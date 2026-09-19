import { readdir, readFile } from "node:fs/promises";
import { join } from "node:path";
import { z } from "zod";
import { projectRoot, safely, type McpHandler, type McpRuntime } from "./common.js";

export const readLogsSchema = z.object({
  project: z.string().optional().default("."),
  runId: z.string().regex(/^[A-Za-z0-9-]+$/).optional(),
  outputLimit: z.number().int().min(1).max(200_000).optional().default(200_000)
});

export function createReadLogsHandler(runtime: McpRuntime): McpHandler<z.infer<typeof readLogsSchema>> {
  return async (args) => safely(async () => {
    const root = projectRoot(runtime, args.project);
    const runRoot = join(root, ".minecraft-agent", "runs");
    const files = (await readdir(runRoot).catch(() => [])).filter((file) => file.endsWith(".log")).sort();
    const selected = args.runId ? `${args.runId}.log` : files.at(-1);
    if (!selected || !files.includes(selected)) throw new Error("Requested run log was not found");
    return { runId: selected.slice(0, -4), contents: await readFile(join(runRoot, selected), "utf8") };
  }, args.outputLimit);
}
