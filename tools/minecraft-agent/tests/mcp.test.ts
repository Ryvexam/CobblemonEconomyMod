import { mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { inspectProjectSchema, createInspectProjectHandler } from "../src/mcp-tools/inspect-project.js";
import { createProjectSchema } from "../src/mcp-tools/create-project.js";
import { buildProjectSchema } from "../src/mcp-tools/build-project.js";
import { testProjectSchema } from "../src/mcp-tools/test-project.js";
import { getArtifactSchema } from "../src/mcp-tools/get-artifact.js";
import { type McpRuntime } from "../src/mcp-tools/common.js";

const temporaryDirectories: string[] = [];

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })));
});

describe("MCP schemas", () => {
  it("accepts valid inspect, build, and artifact arguments", () => {
    expect(inspectProjectSchema.parse({ project: ".", outputLimit: 2_000 })).toMatchObject({ project: "." });
    expect(buildProjectSchema.parse({ project: ".", task: "build", timeoutMs: 10_000 })).toMatchObject({ task: "build" });
    expect(getArtifactSchema.parse({ project: ".", fileName: "mod.jar" })).toMatchObject({ fileName: "mod.jar" });
  });

  it("accepts only the supported template and Minecraft test tasks", () => {
    expect(createProjectSchema.safeParse({
      target: "new-mod",
      loader: "fabric",
      minecraftVersion: "1.21.1",
      modId: "new-mod",
      packageName: "com.example.mod"
    }).success).toBe(true);
    expect(createProjectSchema.safeParse({
      target: "new-mod",
      loader: "forge",
      minecraftVersion: "1.20.1",
      modId: "new-mod",
      packageName: "com.example.mod"
    }).success).toBe(false);
    expect(testProjectSchema.safeParse({ project: ".", task: "runGameTestServer" }).success).toBe(true);
    expect(testProjectSchema.safeParse({ project: ".", task: "build" }).success).toBe(false);
  });

  it("rejects oversized output requests", () => {
    expect(inspectProjectSchema.safeParse({ outputLimit: 200_001 }).success).toBe(false);
    expect(buildProjectSchema.safeParse({ task: "build", outputLimit: 0 }).success).toBe(false);
    expect(buildProjectSchema.safeParse({ task: "clean" }).success).toBe(false);
    expect(buildProjectSchema.safeParse({ task: "jar" }).success).toBe(true);
  });

  it("keeps truncated JSON responses within the requested limit", async () => {
    const root = await mkdtemp(join(tmpdir(), "minecraft-agent-mcp-output-"));
    temporaryDirectories.push(root);
    const runtime: McpRuntime = {
      allowedRoots: [root],
      inspectProject: async () => ({ root, loader: "fabric", minecraftVersion: "1.21.1", javaVersion: "21", modIds: ["x".repeat(500)], gradleWrapper: null, gradleTasks: [], sourceFiles: [], diagnostics: [] }),
      runGradle: async () => { throw new Error("unused"); },
      runMinecraftTest: async () => { throw new Error("unused"); },
      initProject: async () => { throw new Error("unused"); },
      listArtifacts: async () => [],
      copyArtifact: async () => { throw new Error("unused"); }
    };

    const result = await createInspectProjectHandler(runtime)({ project: root, outputLimit: 64 });

    expect(result.content[0].text.length).toBeLessThanOrEqual(64);
    expect(JSON.parse(result.content[0].text)).toMatchObject({ truncated: true });
  });
});

describe("MCP path policy", () => {
  it("returns a structured error for a project outside the allowed root", async () => {
    const root = await mkdtemp(join(tmpdir(), "minecraft-agent-mcp-"));
    temporaryDirectories.push(root);
    const outside = await mkdtemp(join(tmpdir(), "minecraft-agent-mcp-outside-"));
    temporaryDirectories.push(outside);
    await writeFile(join(outside, "gradle.properties"), "minecraft_version=1.21.1\n");

    const runtime: McpRuntime = {
      allowedRoots: [root],
      inspectProject: async () => { throw new Error("must not run"); },
      runGradle: async () => { throw new Error("must not run"); },
      runMinecraftTest: async () => { throw new Error("must not run"); },
      initProject: async () => { throw new Error("must not run"); },
      listArtifacts: async () => { throw new Error("must not run"); },
      copyArtifact: async () => { throw new Error("must not run"); }
    };

    const result = await createInspectProjectHandler(runtime)({ project: outside, outputLimit: 10_000 });

    expect(result.isError).toBe(true);
    expect(result.content[0]).toMatchObject({ type: "text" });
    expect(result.content[0].text).toMatch(/outside|allowed/i);
  });
});
