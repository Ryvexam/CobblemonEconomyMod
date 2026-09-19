import { access } from "node:fs/promises";
import { constants } from "node:fs";
import { dirname, resolve } from "node:path";
import { assertAllowedPath, resolveProjectRoot } from "../core/safety-policy.js";
import type { CommandIo, CommandRuntime } from "./runtime.js";

async function resolveInitTarget(target: string, allowedRoots: string[]): Promise<string> {
  try {
    await access(target, constants.F_OK);
    return resolveProjectRoot(target, allowedRoots);
  } catch {
    const parent = assertAllowedPath(dirname(resolve(target)), allowedRoots);
    return resolve(parent, target.slice(dirname(resolve(target)).length + 1));
  }
}

export async function initCommand(runtime: CommandRuntime, io: CommandIo, target: string, loader: string, minecraftVersion: string, modId: string, packageName: string, json: boolean): Promise<void> {
  const destination = await resolveInitTarget(target, runtime.allowedRoots);
  const created = await runtime.initProject(destination, { loader, minecraftVersion, modId, packageName });
  io.stdout(json ? `${JSON.stringify({ path: created })}\n` : `Created ${created}\n`);
}
