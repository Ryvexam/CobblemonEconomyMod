import { resolveProjectRoot } from "../core/safety-policy.js";
import type { CommandIo, CommandRuntime } from "./runtime.js";

export async function inspectCommand(runtime: CommandRuntime, io: CommandIo, project: string, json: boolean): Promise<void> {
  const root = resolveProjectRoot(project, runtime.allowedRoots);
  const inspection = await runtime.inspectProject(root);
  io.stdout(json ? `${JSON.stringify(inspection)}\n` : `${JSON.stringify(inspection, null, 2)}\n`);
}
