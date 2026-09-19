import { z } from "zod";
import { jsonResult, projectRoot, safely, type McpHandler, type McpRuntime } from "./common.js";

export const inspectProjectSchema = z.object({
  project: z.string().optional().default("."),
  outputLimit: z.number().int().min(1).max(200_000).optional().default(200_000)
});

export function createInspectProjectHandler(runtime: McpRuntime): McpHandler<z.infer<typeof inspectProjectSchema>> {
  return async (args) => safely(async () => runtime.inspectProject(projectRoot(runtime, args.project)), args.outputLimit);
}
