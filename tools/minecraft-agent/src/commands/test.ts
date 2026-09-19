import { resolveProjectRoot } from "../core/safety-policy.js";
import type { MinecraftTestOptions } from "../core/minecraft-runner.js";
import type { RunResult } from "../core/types.js";
import type { CommandIo, CommandRuntime } from "./runtime.js";

export async function testCommand(
  runtime: CommandRuntime,
  runMinecraftTest: (options: MinecraftTestOptions) => Promise<RunResult>,
  io: CommandIo,
  project: string,
  task: MinecraftTestOptions["task"],
  timeout: number,
  port: number | undefined,
  json: boolean
): Promise<void> {
  const root = resolveProjectRoot(project, runtime.allowedRoots);
  const result = await runMinecraftTest({
    projectRoot: root,
    task,
    timeoutMs: timeout,
    port,
    runRoot: `${root}/.minecraft-agent/runs`
  });
  io.stdout(json ? `${JSON.stringify(result)}\n` : `${JSON.stringify(result, null, 2)}\n`);
  if (result.status !== "success") throw new Error(`Minecraft test ${result.status} during ${result.phase}`);
}
