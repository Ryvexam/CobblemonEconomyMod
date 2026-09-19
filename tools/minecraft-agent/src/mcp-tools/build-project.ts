import { z } from "zod";
import { projectRoot, safely, type McpHandler, type McpRuntime } from "./common.js";

const gradleTasks = ["assemble", "build", "check", "clean", "jar", "tasks", "test"] as const;

export const buildProjectSchema = z.object({
  project: z.string().optional().default("."),
  task: z.enum(gradleTasks).default("build"),
  timeoutMs: z.number().int().min(1_000).max(3_600_000).optional().default(600_000),
  outputLimit: z.number().int().min(1).max(200_000).optional().default(200_000)
});

export function createBuildProjectHandler(runtime: McpRuntime): McpHandler<z.infer<typeof buildProjectSchema>> {
  return async (args) => safely(async () => runtime.runGradle({
    projectRoot: projectRoot(runtime, args.project),
    task: args.task,
    timeoutMs: args.timeoutMs,
    runRoot: `${projectRoot(runtime, args.project)}/.minecraft-agent/runs`
  }), args.outputLimit);
}
