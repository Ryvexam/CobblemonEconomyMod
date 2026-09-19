import { z } from "zod";
import { MIN_OUTPUT_LIMIT, MAX_OUTPUT_LIMIT, projectTarget, safely, type McpHandler, type McpRuntime } from "./common.js";

export const createProjectSchema = z.object({
  target: z.string().min(1),
  loader: z.literal("fabric"),
  minecraftVersion: z.literal("1.21.1"),
  modId: z.string().min(1),
  packageName: z.string().min(1),
  outputLimit: z.number().int().min(MIN_OUTPUT_LIMIT).max(MAX_OUTPUT_LIMIT).optional().default(MAX_OUTPUT_LIMIT)
});

export function createCreateProjectHandler(runtime: McpRuntime): McpHandler<z.infer<typeof createProjectSchema>> {
  return async (args) => safely(async () => {
    const target = await projectTarget(runtime, args.target);
    return runtime.initProject(target, {
      loader: args.loader,
      minecraftVersion: args.minecraftVersion,
      modId: args.modId,
      packageName: args.packageName
    });
  }, args.outputLimit);
}
