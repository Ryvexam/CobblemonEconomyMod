import { Command } from "commander";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
import { inspectProject } from "./core/project-inspector.js";
import { runGradle } from "./core/gradle-runner.js";
import { listArtifacts, copyArtifact } from "./core/artifact-manager.js";
import { initProject } from "./core/template-manager.js";
import { runMinecraftTest } from "./core/minecraft-runner.js";
import { artifactCommand } from "./commands/artifact.js";
import { buildCommand } from "./commands/build.js";
import { initCommand } from "./commands/init.js";
import { inspectCommand } from "./commands/inspect.js";
import { logsCommand } from "./commands/logs.js";
import { testCommand } from "./commands/test.js";
import type { CommandIo, CommandRuntime } from "./commands/runtime.js";

export interface CliIo {
  stdout: (value: string) => void;
  stderr: (value: string) => void;
}

export interface CliDependencies extends Partial<CommandRuntime> {
  runMinecraftTest?: typeof runMinecraftTest;
}

function positiveInteger(value: string, label: string, maximum: number): number {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1 || parsed > maximum) {
    throw new Error(`${label} must be an integer between 1 and ${maximum}`);
  }
  return parsed;
}

function defaultRuntime(overrides: CliDependencies): CommandRuntime {
  const workingDirectory = process.cwd();
  const repositoryRoot = resolve(workingDirectory, "../..");
  return {
    allowedRoots: overrides.allowedRoots ?? [workingDirectory, repositoryRoot],
    inspectProject: overrides.inspectProject ?? inspectProject,
    runGradle: overrides.runGradle ?? runGradle,
    listArtifacts: overrides.listArtifacts ?? listArtifacts,
    copyArtifact: overrides.copyArtifact ?? copyArtifact,
    initProject: overrides.initProject ?? initProject
  };
}

export async function runCli(argv: string[], overrides: CliDependencies = {}, io: CliIo = {
  stdout: (value) => process.stdout.write(value),
  stderr: (value) => process.stderr.write(value)
}): Promise<number> {
  const runtime = defaultRuntime(overrides);
  const minecraftTest = overrides.runMinecraftTest ?? runMinecraftTest;
  const commandIo: CommandIo = io;
  const program = new Command()
    .name("minecraft-agent")
    .description("Local Minecraft project inspection, build, test, and artifact tooling")
    .showHelpAfterError();

  program.configureOutput({ writeOut: io.stdout, writeErr: io.stderr });
  program.exitOverride();

  program.command("inspect")
    .option("--project <path>", "Minecraft project path", ".")
    .option("--json", "Print JSON")
    .action(async (options: { project: string; json?: boolean }) => inspectCommand(runtime, commandIo, options.project, Boolean(options.json)));

  program.command("build")
    .option("--project <path>", "Minecraft project path", ".")
    .option("--task <task>", "Allowlisted Gradle task", "build")
    .option("--timeout <milliseconds>", "Maximum execution time", "600000")
    .option("--json", "Print JSON")
    .action(async (options: { project: string; task: string; timeout: string; json?: boolean }) => buildCommand(runtime, commandIo, options.project, options.task, positiveInteger(options.timeout, "Timeout", 3_600_000), Boolean(options.json)));

  program.command("logs")
    .option("--project <path>", "Minecraft project path", ".")
    .option("--since <run-id>", "Read one run by ID")
    .option("--limit <characters>", "Maximum log characters", "200000")
    .option("--json", "Print JSON")
    .action(async (options: { project: string; since?: string; limit: string; json?: boolean }) => logsCommand(runtime, commandIo, options.project, options.since, positiveInteger(options.limit, "Log limit", 200_000), Boolean(options.json)));

  program.command("artifact")
    .option("--project <path>", "Minecraft project path", ".")
    .option("--output <directory>", "Copy artifacts to an allowed directory")
    .option("--json", "Print JSON")
    .action(async (options: { project: string; output?: string; json?: boolean }) => artifactCommand(runtime, commandIo, options.project, options.output, Boolean(options.json)));

  program.command("test")
    .option("--project <path>", "Minecraft project path", ".")
    .option("--task <task>", "Minecraft test task", "runServer")
    .option("--timeout <milliseconds>", "Maximum execution time", "600000")
    .option("--port <port>", "Dedicated server port")
    .option("--json", "Print JSON")
    .action(async (options: { project: string; task: string; timeout: string; port?: string; json?: boolean }) => testCommand(
      runtime,
      minecraftTest,
      commandIo,
      options.project,
      options.task as "runServer" | "runGameTestServer",
      positiveInteger(options.timeout, "Timeout", 3_600_000),
      options.port === undefined ? undefined : positiveInteger(options.port, "Port", 65_535),
      Boolean(options.json)
    ));

  program.command("init <path>")
    .option("--loader <loader>", "Loader", "fabric")
    .option("--mc <version>", "Minecraft version", "1.21.1")
    .requiredOption("--mod-id <id>", "Mod identifier")
    .requiredOption("--package <package>", "Java package name")
    .option("--json", "Print JSON")
    .action(async (path: string, options: { loader: string; mc: string; modId: string; package: string; json?: boolean }) => initCommand(runtime, commandIo, path, options.loader, options.mc, options.modId, options.package, Boolean(options.json)));

  try {
    await program.parseAsync(["node", "minecraft-agent", ...argv]);
    return 0;
  } catch (error) {
    io.stderr(`${error instanceof Error ? error.message : String(error)}\n`);
    return 1;
  }
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  runCli(process.argv.slice(2)).then((code) => {
    process.exitCode = code;
  });
}
