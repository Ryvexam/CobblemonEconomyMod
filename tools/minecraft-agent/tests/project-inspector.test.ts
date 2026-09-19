import { chmod, mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { inspectProject } from "../src/core/project-inspector.js";

const temporaryDirectories: string[] = [];

async function makeProject(files: Record<string, string>) {
  const root = await mkdtemp(join(tmpdir(), "minecraft-agent-project-"));
  temporaryDirectories.push(root);

  for (const [relativePath, contents] of Object.entries(files)) {
    const file = join(root, relativePath);
    await mkdir(join(file, ".."), { recursive: true });
    await writeFile(file, contents);
  }

  if (files.gradlew !== undefined) {
    await chmod(join(root, "gradlew"), 0o755);
  }

  return root;
}

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })));
});

describe("project inspection", () => {
  it("detects a Fabric 1.21.1 project and its mod id", async () => {
    const root = await makeProject({
      "gradle.properties": "minecraft_version=1.21.1\norg.gradle.java.home=21\n",
      "build.gradle.kts": 'plugins { id("fabric-loom") version "1.7-SNAPSHOT" }\ndependencies { modImplementation("net.fabricmc:fabric-loader:0.16.5") }',
      gradlew: "#!/bin/sh\nexit 0\n",
      "src/main/resources/fabric.mod.json": JSON.stringify({ id: "example-mod", entrypoints: { main: ["example.Mod"] } })
    });

    const inspection = await inspectProject(root);

    expect(inspection.loader).toBe("fabric");
    expect(inspection.minecraftVersion).toBe("1.21.1");
    expect(inspection.javaVersion).toBe("21");
    expect(inspection.modIds).toEqual(["example-mod"]);
    expect(inspection.gradleWrapper).toBe(join(root, "gradlew"));
    expect(inspection.sourceFiles).toContain("src/main/resources/fabric.mod.json");
  });

  it("reports diagnostics for an empty project", async () => {
    const root = await makeProject({});

    const inspection = await inspectProject(root);

    expect(inspection.loader).toBe("unknown");
    expect(inspection.minecraftVersion).toBeNull();
    expect(inspection.diagnostics.join(" ")).toMatch(/loader/i);
    expect(inspection.diagnostics.join(" ")).toMatch(/Gradle wrapper/);
  });

  it("does not silently choose Fabric when Forge markers conflict", async () => {
    const root = await makeProject({
      "build.gradle": 'plugins { id "net.minecraftforge.gradle" }\ndependencies { minecraft "net.minecraftforge:forge:1.20.1" }',
      "build.gradle.kts": 'plugins { id("fabric-loom") version "1.7-SNAPSHOT" }',
      "gradle.properties": "minecraft_version=1.21.1\n"
    });

    const inspection = await inspectProject(root);

    expect(inspection.loader).toBe("unknown");
    expect(inspection.diagnostics.join(" ")).toMatch(/conflicting/i);
  });

  it("reports missing Minecraft metadata and inferred Gradle tasks", async () => {
    const root = await makeProject({
      "build.gradle.kts": 'plugins { id("fabric-loom") version "1.7-SNAPSHOT" }\ntasks.register("qualityCheck") {}'
    });

    const inspection = await inspectProject(root);

    expect(inspection.minecraftVersion).toBeNull();
    expect(inspection.diagnostics.join(" ")).toMatch(/minecraft version/i);
    expect(inspection.gradleTasks).toContain("qualityCheck");
  });

  it("reports malformed Fabric metadata instead of silently ignoring it", async () => {
    const root = await makeProject({
      "gradle.properties": "minecraft_version=1.21.1\n",
      "build.gradle.kts": 'plugins { id("fabric-loom") version "1.7-SNAPSHOT" }',
      "src/main/resources/fabric.mod.json": "{not-json"
    });

    const inspection = await inspectProject(root);

    expect(inspection.diagnostics.join(" ")).toMatch(/fabric\.mod\.json.*malformed/i);
  });
});
