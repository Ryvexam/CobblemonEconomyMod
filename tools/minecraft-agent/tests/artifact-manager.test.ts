import { mkdir, mkdtemp, readFile, rm, symlink, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { copyArtifact, listArtifacts } from "../src/core/artifact-manager.js";

const temporaryDirectories: string[] = [];

async function makeProject() {
  const root = await mkdtemp(join(tmpdir(), "minecraft-agent-artifacts-"));
  temporaryDirectories.push(root);
  await mkdir(join(root, "build/libs"), { recursive: true });
  return root;
}

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })));
});

describe("artifact manager", () => {
  it("lists only jar artifacts with SHA-256 metadata", async () => {
    const projectRoot = await makeProject();
    await writeFile(join(projectRoot, "build/libs/example.jar"), "jar-content");
    await writeFile(join(projectRoot, "build/libs/example-sources.jar"), "sources");
    await writeFile(join(projectRoot, "build/libs/readme.txt"), "not an artifact");
    await mkdir(join(projectRoot, "build/libs/not-a-file.jar"));

    const artifacts = await listArtifacts(projectRoot);

    expect(artifacts).toHaveLength(2);
    expect(artifacts[0]).toMatchObject({ fileName: "example-sources.jar", size: 7 });
    expect(artifacts[1]).toMatchObject({ fileName: "example.jar", size: 11 });
    expect(artifacts[0].sha256).toMatch(/^[a-f0-9]{64}$/);
  });

  it("copies a selected artifact and rejects traversal", async () => {
    const projectRoot = await makeProject();
    const outputRoot = join(projectRoot, "artifacts");
    await writeFile(join(projectRoot, "build/libs/example.jar"), "jar-content");

    const artifact = await copyArtifact(projectRoot, "example.jar", outputRoot);
    expect(await readFile(artifact.path, "utf8")).toBe("jar-content");
    await expect(copyArtifact(projectRoot, "../secret.jar", outputRoot)).rejects.toThrow(/artifact/i);
  });

  it("rejects an output directory symlink that escapes the project", async () => {
    const projectRoot = await makeProject();
    const outsideRoot = await mkdtemp(join(tmpdir(), "minecraft-agent-artifacts-outside-"));
    temporaryDirectories.push(outsideRoot);
    const linkedOutput = join(projectRoot, "linked-output");
    await symlink(outsideRoot, linkedOutput, "dir");
    await writeFile(join(projectRoot, "build/libs/example.jar"), "jar-content");

    await expect(copyArtifact(projectRoot, "example.jar", linkedOutput)).rejects.toThrow(/outside|symlink/i);
    await expect(readFile(join(outsideRoot, "example.jar"), "utf8")).rejects.toThrow();
  });

  it("rejects an artifact directory symlink that escapes the project", async () => {
    const projectRoot = await makeProject();
    const outsideRoot = await mkdtemp(join(tmpdir(), "minecraft-agent-artifacts-libs-outside-"));
    temporaryDirectories.push(outsideRoot);
    await rm(join(projectRoot, "build/libs"), { recursive: true, force: true });
    await symlink(outsideRoot, join(projectRoot, "build/libs"), "dir");
    await writeFile(join(outsideRoot, "escaped.jar"), "outside");

    await expect(listArtifacts(projectRoot)).rejects.toThrow(/outside|symlink/i);
  });
});
