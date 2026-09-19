import { z } from "zod";
import { MIN_OUTPUT_LIMIT, MAX_OUTPUT_LIMIT, projectRoot, safely, type McpHandler, type McpRuntime } from "./common.js";

const gradleTasks = ["assemble", "build", "check", "jar", "tasks", "test"] as const;

export const buildProjectSchema = z.object({
  project: z.string().optional().default("."),
  task: z.enum(gradleTasks).default("build"),
  timeoutMs: z.number().int().min(1_000).max(3_600_000).optional().default(600_000),
  outputLimit: z.number().int().min(MIN_OUTPUT_LIMIT).max(MAX_OUTPUT_LIMIT).optional().default(MAX_OUTPUT_LIMIT)
});

export function createBuildProjectHandler(runtime: McpRuntime): McpHandler<z.infer<typeof buildProjectSchema>> {
  return async (args) => safely(async () => runtime.runGradle({
    projectRoot: projectRoot(runtime, args.project),
    task: args.task,
    timeoutMs: args.timeoutMs,
    runRoot: `${projectRoot(runtime, args.project)}/.minecraft-agent/runs`
  }), args.outputLimit);
}
