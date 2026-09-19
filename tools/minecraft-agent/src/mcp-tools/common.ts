import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import { resolveProjectRoot, type ProjectPathError } from "../core/safety-policy.js";
import type { ArtifactInfo, ProjectInspection, RunResult } from "../core/types.js";
import type { GradleRunOptions } from "../core/gradle-runner.js";
import type { InitProjectOptions } from "../core/template-manager.js";
import type { MinecraftTestOptions } from "../core/minecraft-runner.js";
import { access } from "node:fs/promises";
import { dirname, resolve } from "node:path";

export interface McpRuntime {
  allowedRoots: string[];
  inspectProject: (root: string) => Promise<ProjectInspection>;
  runGradle: (options: GradleRunOptions) => Promise<RunResult>;
  runMinecraftTest: (options: MinecraftTestOptions) => Promise<RunResult>;
  initProject: (target: string, options: InitProjectOptions) => Promise<string>;
  listArtifacts: (root: string) => Promise<ArtifactInfo[]>;
  copyArtifact: (root: string, fileName: string, outputRoot: string) => Promise<ArtifactInfo>;
}

export const MAX_OUTPUT_LIMIT = 200_000;

export type McpHandler<T> = (args: T) => Promise<CallToolResult>;

export function jsonResult(value: unknown, outputLimit: number): CallToolResult {
  const serialized = JSON.stringify(value);
  if (serialized.length <= outputLimit) {
    return { content: [{ type: "text", text: serialized }] };
  }

  const previewLength = Math.max(0, outputLimit - 80);
  return {
    content: [{
      type: "text",
      text: JSON.stringify({ truncated: true, preview: serialized.slice(0, previewLength) })
    }],
    isError: false
  };
}

export function errorResult(error: unknown): CallToolResult {
  return {
    content: [{
      type: "text",
      text: error instanceof Error ? error.message : String(error)
    }],
    isError: true
  };
}

export function projectRoot(runtime: McpRuntime, project: string | undefined): string {
  return resolveProjectRoot(project, runtime.allowedRoots);
}

export async function projectTarget(runtime: McpRuntime, target: string): Promise<string> {
  try {
    await access(target);
    return resolveProjectRoot(target, runtime.allowedRoots);
  } catch {
    const parent = resolveProjectRoot(dirname(resolve(target)), runtime.allowedRoots);
    return resolve(parent, target.slice(dirname(resolve(target)).length + 1));
  }
}

export async function safely<T>(operation: () => Promise<T>, outputLimit: number): Promise<CallToolResult> {
  try {
    return jsonResult(await operation(), outputLimit);
  } catch (error) {
    return errorResult(error);
  }
}
