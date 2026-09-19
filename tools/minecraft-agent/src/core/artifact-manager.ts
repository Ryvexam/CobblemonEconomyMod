import { createHash } from "node:crypto";
import { access, copyFile, lstat, mkdir, readdir, readFile, realpath, stat } from "node:fs/promises";
import { constants } from "node:fs";
import { basename, isAbsolute, join, relative, resolve } from "node:path";
import type { ArtifactInfo } from "./types.js";

function assertSafeFileName(fileName: string): void {
  if (basename(fileName) !== fileName || !fileName.endsWith(".jar")) {
    throw new Error(`Invalid artifact file name: ${fileName}`);
  }
}

function isInside(root: string, target: string): boolean {
  const pathFromRoot = relative(root, target);
  return pathFromRoot === "" || (pathFromRoot !== ".." && !pathFromRoot.startsWith(`..${resolve("/")}`) && !isAbsolute(pathFromRoot));
}

function assertLexicallyInside(root: string, target: string): string {
  const absoluteRoot = resolve(root);
  const absoluteTarget = resolve(target);
  if (!isInside(absoluteRoot, absoluteTarget)) {
    throw new Error(`Artifact output is outside the project: ${target}`);
  }
  return absoluteTarget;
}

async function assertRealPathInside(root: string, target: string): Promise<string> {
  const absoluteRoot = await realpath(root);
  let existingAncestor = resolve(target);
  while (true) {
    try {
      const canonicalAncestor = await realpath(existingAncestor);
      if (!isInside(absoluteRoot, canonicalAncestor)) {
        throw new Error(`Artifact output is outside the project through a symlink: ${target}`);
      }
      return resolve(target);
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
      const parent = resolve(existingAncestor, "..");
      if (parent === existingAncestor) throw new Error(`Artifact output does not have an existing parent: ${target}`);
      existingAncestor = parent;
    }
  }
}

async function artifactInfo(file: string): Promise<ArtifactInfo> {
  const [metadata, contents] = await Promise.all([stat(file), readFile(file)]);
  return {
    fileName: basename(file),
    path: file,
    size: metadata.size,
    sha256: createHash("sha256").update(contents).digest("hex")
  };
}

export async function listArtifacts(projectRoot: string): Promise<ArtifactInfo[]> {
  const libs = join(projectRoot, "build", "libs");
  try {
    await access(libs, constants.R_OK);
  } catch {
    return [];
  }
  await assertRealPathInside(projectRoot, libs);
  const entries = await readdir(libs, { withFileTypes: true });
  const files = entries.filter((entry) => entry.isFile() && entry.name.endsWith(".jar")).map((entry) => entry.name).sort();
  return Promise.all(files.map((file) => artifactInfo(join(libs, file))));
}

export async function copyArtifact(projectRoot: string, fileName: string, outputRoot: string): Promise<ArtifactInfo> {
  assertSafeFileName(fileName);
  const source = join(projectRoot, "build", "libs", fileName);
  const sourceStat = await lstat(source);
  if (!sourceStat.isFile()) throw new Error(`Artifact is not a regular file: ${fileName}`);
  await assertRealPathInside(projectRoot, source);
  await access(source, constants.R_OK);
  const destinationRoot = await assertRealPathInside(projectRoot, assertLexicallyInside(projectRoot, outputRoot));
  await mkdir(destinationRoot, { recursive: true });
  const destination = join(destinationRoot, fileName);
  try {
    if ((await lstat(destination)).isSymbolicLink()) {
      throw new Error(`Artifact destination is a symlink: ${destination}`);
    }
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
  }
  await copyFile(source, destination);
  return artifactInfo(destination);
}
