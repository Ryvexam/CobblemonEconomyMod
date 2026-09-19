import { z } from "zod";
import { projectRoot, safely, type McpHandler, type McpRuntime } from "./common.js";

export const getArtifactSchema = z.object({
  project: z.string().optional().default("."),
  fileName: z.string().regex(/^[A-Za-z0-9_.-]+\.jar$/).optional(),
  outputRoot: z.string().optional(),
  outputLimit: z.number().int().min(1).max(200_000).optional().default(200_000)
});

export function createGetArtifactHandler(runtime: McpRuntime): McpHandler<z.infer<typeof getArtifactSchema>> {
  return async (args) => safely(async () => {
    const root = projectRoot(runtime, args.project);
    const artifacts = await runtime.listArtifacts(root);
    if (!args.fileName) return artifacts;
    const selected = artifacts.find((artifact) => artifact.fileName === args.fileName);
    if (!selected) throw new Error(`Artifact not found: ${args.fileName}`);
    return args.outputRoot ? runtime.copyArtifact(root, selected.fileName, args.outputRoot) : selected;
  }, args.outputLimit);
}
