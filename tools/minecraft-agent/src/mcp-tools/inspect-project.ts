import { z } from "zod";
import { MIN_OUTPUT_LIMIT, MAX_OUTPUT_LIMIT, projectRoot, safely, type McpHandler, type McpRuntime } from "./common.js";

export const inspectProjectSchema = z.object({
  project: z.string().optional().default("."),
  outputLimit: z.number().int().min(MIN_OUTPUT_LIMIT).max(MAX_OUTPUT_LIMIT).optional().default(MAX_OUTPUT_LIMIT)
});

export function createInspectProjectHandler(runtime: McpRuntime): McpHandler<z.infer<typeof inspectProjectSchema>> {
  return async (args) => safely(async () => runtime.inspectProject(projectRoot(runtime, args.project)), args.outputLimit);
}
