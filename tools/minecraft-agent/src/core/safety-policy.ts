import { existsSync, realpathSync } from "node:fs";
import { isAbsolute, relative, resolve } from "node:path";

export class ProjectPathError extends Error {
  constructor(public readonly rejectedPath: string, message = "Project path is outside the allowed roots") {
    super(`${message}: ${rejectedPath}`);
    this.name = "ProjectPathError";
  }
}

function canonicalExistingPath(target: string): string {
  const absolute = resolve(target);
  if (!existsSync(absolute)) {
    throw new ProjectPathError(absolute, "Project path does not exist");
  }

  return realpathSync(absolute);
}

function isContained(target: string, root: string): boolean {
  const pathFromRoot = relative(root, target);
  return pathFromRoot === "" || (pathFromRoot !== ".." && !pathFromRoot.startsWith(`..${resolve("/")}`) && !isAbsolute(pathFromRoot));
}

export function assertAllowedPath(target: string, allowedRoots: string[]): string {
  if (allowedRoots.length === 0) {
    throw new ProjectPathError(resolve(target), "No allowed project roots configured");
  }

  const canonicalTarget = canonicalExistingPath(target);
  const canonicalRoots = allowedRoots.map((root) => canonicalExistingPath(root));
  if (!canonicalRoots.some((root) => isContained(canonicalTarget, root))) {
    throw new ProjectPathError(canonicalTarget);
  }

  return canonicalTarget;
}

export function resolveProjectRoot(project: string | undefined, allowedRoots: string[]): string {
  return assertAllowedPath(project ?? process.cwd(), allowedRoots);
}
