import { resolveProjectRoot } from "../core/safety-policy.js";
import type { CommandIo, CommandRuntime } from "./runtime.js";

export async function artifactCommand(runtime: CommandRuntime, io: CommandIo, project: string, output: string | undefined, json: boolean): Promise<void> {
  const root = resolveProjectRoot(project, runtime.allowedRoots);
  const artifacts = await runtime.listArtifacts(root);
  const result = output ? await Promise.all(artifacts.map((artifact) => runtime.copyArtifact(root, artifact.fileName, output))) : artifacts;
  io.stdout(json ? `${JSON.stringify(result)}\n` : `${JSON.stringify(result, null, 2)}\n`);
}
