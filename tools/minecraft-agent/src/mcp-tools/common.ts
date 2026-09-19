import type { CallToolResult } from "@modelcontextprotocol/sdk/types.js";
import { assertLexicallyAllowedPath, resolveProjectRoot } from "../core/safety-policy.js";
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
export const MIN_OUTPUT_LIMIT = 64;

export type McpHandler<T> = (args: T) => Promise<CallToolResult>;

export function jsonResult(value: unknown, outputLimit: number): CallToolResult {
  const serialized = JSON.stringify(value);
  if (serialized.length <= outputLimit) {
    return { content: [{ type: "text", text: serialized }] };
  }

  let preview = serialized.slice(0, Math.max(0, outputLimit - 80));
  let truncatedPayload = JSON.stringify({ truncated: true, preview });
  while (truncatedPayload.length > outputLimit && preview.length > 0) {
    preview = preview.slice(0, Math.max(0, preview.length - 16));
    truncatedPayload = JSON.stringify({ truncated: true, preview });
  }
  if (truncatedPayload.length > outputLimit) truncatedPayload = JSON.stringify({ truncated: true });
  return {
    content: [{
      type: "text",
      text: truncatedPayload
    }],
    isError: false
  };
}

export function errorResult(error: unknown, outputLimit = MAX_OUTPUT_LIMIT): CallToolResult {
  const message = error instanceof Error ? error.message : String(error);
  const bounded = jsonResult({ error: message }, outputLimit);
  return {
    content: bounded.content,
    isError: true
  };
}

export function projectRoot(runtime: McpRuntime, project: string | undefined): string {
  return resolveProjectRoot(project, runtime.allowedRoots);
}

export async function projectTarget(runtime: McpRuntime, target: string): Promise<string> {
  const absoluteTarget = assertLexicallyAllowedPath(target, runtime.allowedRoots);
  try {
    await access(absoluteTarget);
    return resolveProjectRoot(absoluteTarget, runtime.allowedRoots);
  } catch {
    const parentPath = dirname(absoluteTarget);
    const parent = resolveProjectRoot(parentPath, runtime.allowedRoots);
    return resolve(parent, absoluteTarget.slice(parentPath.length + 1));
  }
}

export async function safely<T>(operation: () => Promise<T>, outputLimit: number): Promise<CallToolResult> {
  try {
    return jsonResult(await operation(), outputLimit);
  } catch (error) {
    return errorResult(error, outputLimit);
  }
}
