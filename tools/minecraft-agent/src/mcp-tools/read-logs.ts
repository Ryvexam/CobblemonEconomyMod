import { open, readdir } from "node:fs/promises";
import { join } from "node:path";
import { z } from "zod";
import { MIN_OUTPUT_LIMIT, MAX_OUTPUT_LIMIT, projectRoot, safely, type McpHandler, type McpRuntime } from "./common.js";

export const readLogsSchema = z.object({
  project: z.string().optional().default("."),
  runId: z.string().regex(/^[A-Za-z0-9-]+$/).optional(),
  outputLimit: z.number().int().min(MIN_OUTPUT_LIMIT).max(MAX_OUTPUT_LIMIT).optional().default(MAX_OUTPUT_LIMIT)
});

export function createReadLogsHandler(runtime: McpRuntime): McpHandler<z.infer<typeof readLogsSchema>> {
  return async (args) => safely(async () => {
    const root = projectRoot(runtime, args.project);
    const runRoot = join(root, ".minecraft-agent", "runs");
    const files = (await readdir(runRoot).catch(() => [])).filter((file) => file.endsWith(".log")).sort();
    const selected = args.runId ? `${args.runId}.log` : files.at(-1);
    if (!selected || !files.includes(selected)) throw new Error("Requested run log was not found");
    const handle = await open(join(runRoot, selected), "r");
    try {
      const buffer = Buffer.alloc(args.outputLimit);
      const { bytesRead } = await handle.read(buffer, 0, buffer.length, 0);
      return { runId: selected.slice(0, -4), contents: buffer.subarray(0, bytesRead).toString("utf8"), truncated: bytesRead === buffer.length };
    } finally {
      await handle.close();
    }
  }, args.outputLimit);
}
