import { mkdir, mkdtemp, realpath, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import {
  assertAllowedPath,
  ProjectPathError,
  resolveProjectRoot
} from "../src/core/safety-policy.js";

const temporaryDirectories: string[] = [];

async function makeTemporaryDirectory() {
  const directory = await mkdtemp(join(tmpdir(), "minecraft-agent-safety-"));
  temporaryDirectories.push(directory);
  return directory;
}

afterEach(async () => {
  const { rm } = await import("node:fs/promises");
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })));
});

describe("project path safety", () => {
  it("accepts an existing nested project", async () => {
    const root = await makeTemporaryDirectory();
    const project = join(root, "projects", "example");
    await mkdir(project, { recursive: true });

    expect(resolveProjectRoot(project, [root])).toBe(await realpath(project));
  });

  it("rejects a sibling escape", async () => {
    const root = await makeTemporaryDirectory();
    const outside = join(root, "..", "outside");
    expect(() => assertAllowedPath(outside, [root])).toThrow(ProjectPathError);
  });

  it("rejects a path-prefix collision", async () => {
    const root = await makeTemporaryDirectory();
    const allowed = join(root, "allowed");
    const sibling = join(root, "allowed-other");
    await mkdir(allowed, { recursive: true });
    await mkdir(sibling, { recursive: true });

    expect(() => assertAllowedPath(sibling, [allowed])).toThrow(ProjectPathError);
  });

  it("rejects a missing project root", async () => {
    const root = await makeTemporaryDirectory();
    const missing = join(root, "missing");
    await writeFile(join(root, "marker.txt"), "root");

    expect(() => resolveProjectRoot(missing, [root])).toThrow(ProjectPathError);
  });
});
