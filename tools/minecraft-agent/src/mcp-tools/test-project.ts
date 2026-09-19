import { z } from "zod";
import { MIN_OUTPUT_LIMIT, MAX_OUTPUT_LIMIT, projectRoot, safely, type McpHandler, type McpRuntime } from "./common.js";

export const testProjectSchema = z.object({
  project: z.string().optional().default("."),
  task: z.enum(["runServer", "runGameTestServer"]).default("runServer"),
  timeoutMs: z.number().int().min(1_000).max(3_600_000).optional().default(600_000),
  port: z.number().int().min(1).max(65_535).optional(),
  outputLimit: z.number().int().min(MIN_OUTPUT_LIMIT).max(MAX_OUTPUT_LIMIT).optional().default(MAX_OUTPUT_LIMIT)
});

export function createTestProjectHandler(runtime: McpRuntime): McpHandler<z.infer<typeof testProjectSchema>> {
  return async (args) => safely(async () => runtime.runMinecraftTest({
    projectRoot: projectRoot(runtime, args.project),
    task: args.task,
    timeoutMs: args.timeoutMs,
    port: args.port,
    runRoot: `${projectRoot(runtime, args.project)}/.minecraft-agent/runs`
  }), args.outputLimit);
}
