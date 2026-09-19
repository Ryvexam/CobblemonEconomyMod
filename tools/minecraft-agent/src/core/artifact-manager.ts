import { createHash } from "node:crypto";
import { access, copyFile, mkdir, readdir, readFile, stat } from "node:fs/promises";
import { constants } from "node:fs";
import { basename, join, relative, resolve } from "node:path";
import type { ArtifactInfo } from "./types.js";

function assertSafeFileName(fileName: string): void {
  if (basename(fileName) !== fileName || !fileName.endsWith(".jar")) {
    throw new Error(`Invalid artifact file name: ${fileName}`);
  }
}

function assertInside(root: string, target: string): string {
  const absoluteRoot = resolve(root);
  const absoluteTarget = resolve(target);
  const pathFromRoot = relative(absoluteRoot, absoluteTarget);
  if (pathFromRoot === ".." || pathFromRoot.startsWith(`..${resolve("/")}`) || resolve(pathFromRoot) === resolve("/")) {
    throw new Error(`Artifact output is outside the project: ${target}`);
  }
  return absoluteTarget;
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

  const files = (await readdir(libs)).filter((file) => file.endsWith(".jar")).sort();
  return Promise.all(files.map((file) => artifactInfo(join(libs, file))));
}

export async function copyArtifact(projectRoot: string, fileName: string, outputRoot: string): Promise<ArtifactInfo> {
  assertSafeFileName(fileName);
  const source = join(projectRoot, "build", "libs", fileName);
  await access(source, constants.R_OK);
  const destinationRoot = assertInside(projectRoot, outputRoot);
  await mkdir(destinationRoot, { recursive: true });
  const destination = join(destinationRoot, fileName);
  await copyFile(source, destination);
  return artifactInfo(destination);
}
