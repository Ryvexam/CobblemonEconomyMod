import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
import { inspectProject } from "./core/project-inspector.js";
import { runGradle } from "./core/gradle-runner.js";
import { runMinecraftTest } from "./core/minecraft-runner.js";
import { initProject } from "./core/template-manager.js";
import { listArtifacts, copyArtifact } from "./core/artifact-manager.js";
import type { McpRuntime } from "./mcp-tools/common.js";
import { inspectProjectSchema, createInspectProjectHandler } from "./mcp-tools/inspect-project.js";
import { createProjectSchema, createCreateProjectHandler } from "./mcp-tools/create-project.js";
import { buildProjectSchema, createBuildProjectHandler } from "./mcp-tools/build-project.js";
import { testProjectSchema, createTestProjectHandler } from "./mcp-tools/test-project.js";
import { readLogsSchema, createReadLogsHandler } from "./mcp-tools/read-logs.js";
import { getArtifactSchema, createGetArtifactHandler } from "./mcp-tools/get-artifact.js";

export function createDefaultMcpRuntime(): McpRuntime {
  return {
    allowedRoots: [resolve(process.cwd())],
    inspectProject,
    runGradle,
    runMinecraftTest,
    initProject,
    listArtifacts,
    copyArtifact
  };
}

export function createMcpServer(runtime: McpRuntime = createDefaultMcpRuntime()): McpServer {
  const server = new McpServer({ name: "minecraft-agent", version: "0.1.0" });

  server.registerTool("inspect_project", {
    title: "Inspect Minecraft project",
    description: "Inspect loader, Minecraft version, source files, and diagnostics without modifying the project.",
    inputSchema: inspectProjectSchema.shape
  }, createInspectProjectHandler(runtime));

  server.registerTool("create_project", {
    title: "Create Minecraft project",
    description: "Create a new supported Fabric 1.21.1 project under an allowed local root.",
    inputSchema: createProjectSchema.shape
  }, createCreateProjectHandler(runtime));

  server.registerTool("build_project", {
    title: "Build Minecraft project",
    description: "Run one allowlisted Gradle task with bounded output and timeout handling.",
    inputSchema: buildProjectSchema.shape
  }, createBuildProjectHandler(runtime));

  server.registerTool("test_project", {
    title: "Test Minecraft project",
    description: "Start an allowlisted Minecraft server task, detect readiness, and stop it gracefully.",
    inputSchema: testProjectSchema.shape
  }, createTestProjectHandler(runtime));

  server.registerTool("read_logs", {
    title: "Read Minecraft agent logs",
    description: "Read the latest or selected bounded local run log.",
    inputSchema: readLogsSchema.shape
  }, createReadLogsHandler(runtime));

  server.registerTool("get_artifact", {
    title: "Get Minecraft artifact",
    description: "List or copy a generated JAR artifact with SHA-256 metadata.",
    inputSchema: getArtifactSchema.shape
  }, createGetArtifactHandler(runtime));

  return server;
}

export async function startMcpServer(runtime?: McpRuntime): Promise<void> {
  const server = createMcpServer(runtime);
  await server.connect(new StdioServerTransport());
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  startMcpServer().catch((error) => {
    process.stderr.write(`${error instanceof Error ? error.stack ?? error.message : String(error)}\n`);
    process.exitCode = 1;
  });
}
