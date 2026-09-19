import { Command } from "commander";
import { fileURLToPath } from "node:url";
import { resolve } from "node:path";
import { inspectProject } from "./core/project-inspector.js";
import { runGradle } from "./core/gradle-runner.js";
import { listArtifacts, copyArtifact } from "./core/artifact-manager.js";
import { initProject } from "./core/template-manager.js";
import { artifactCommand } from "./commands/artifact.js";
import { buildCommand } from "./commands/build.js";
import { initCommand } from "./commands/init.js";
import { inspectCommand } from "./commands/inspect.js";
import { logsCommand } from "./commands/logs.js";
import type { CommandIo, CommandRuntime } from "./commands/runtime.js";

export interface CliIo {
  stdout: (value: string) => void;
  stderr: (value: string) => void;
}

export type CliDependencies = Partial<CommandRuntime>;

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
    .action(async (options: { project: string; task: string; timeout: string; json?: boolean }) => buildCommand(runtime, commandIo, options.project, options.task, Number(options.timeout), Boolean(options.json)));

  program.command("logs")
    .option("--project <path>", "Minecraft project path", ".")
    .option("--since <run-id>", "Read one run by ID")
    .option("--json", "Print JSON")
    .action(async (options: { project: string; since?: string; json?: boolean }) => logsCommand(runtime, commandIo, options.project, options.since, Boolean(options.json)));

  program.command("artifact")
    .option("--project <path>", "Minecraft project path", ".")
    .option("--output <directory>", "Copy artifacts to an allowed directory")
    .option("--json", "Print JSON")
    .action(async (options: { project: string; output?: string; json?: boolean }) => artifactCommand(runtime, commandIo, options.project, options.output, Boolean(options.json)));

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
