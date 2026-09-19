import { resolveProjectRoot } from "../core/safety-policy.js";
import type { CommandIo, CommandRuntime } from "./runtime.js";

export async function buildCommand(runtime: CommandRuntime, io: CommandIo, project: string, task: string, timeout: number, json: boolean): Promise<void> {
  const root = resolveProjectRoot(project, runtime.allowedRoots);
  const result = await runtime.runGradle({ projectRoot: root, task, timeoutMs: timeout, runRoot: `${root}/.minecraft-agent/runs` });
  io.stdout(json ? `${JSON.stringify(result)}\n` : `${JSON.stringify(result, null, 2)}\n`);
  if (result.status !== "success") throw new Error(`Gradle ${result.status} during ${result.phase}`);
}
