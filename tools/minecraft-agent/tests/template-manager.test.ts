import { mkdir, mkdtemp, readFile, rm, stat, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { initProject } from "../src/core/template-manager.js";

const temporaryDirectories: string[] = [];

async function makeRoot() {
  const root = await mkdtemp(join(tmpdir(), "minecraft-agent-template-"));
  temporaryDirectories.push(root);
  return root;
}

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })));
});

describe("template manager", () => {
  it("creates a Fabric 1.21.1 project with substituted identifiers", async () => {
    const root = await makeRoot();
    const target = join(root, "example-mod");

    const created = await initProject(target, { loader: "fabric", minecraftVersion: "1.21.1", modId: "example-mod", packageName: "com.example.mod" });

    expect(created).toBe(target);
    expect(await readFile(join(target, "gradlew"), "utf8")).toContain("gradle/wrapper/gradle-wrapper.jar");
    expect((await stat(join(target, "gradlew"))).mode & 0o111).not.toBe(0);
    expect(await readFile(join(target, "gradle/wrapper/gradle-wrapper.properties"), "utf8")).toContain("gradle-8.10.2-bin.zip");
    const wrapperJar = await readFile(join(target, "gradle/wrapper/gradle-wrapper.jar"));
    expect(wrapperJar.subarray(0, 4).toString("hex")).toBe("504b0304");
    expect(wrapperJar.byteLength).toBe(46_175);
    expect(await readFile(join(target, "gradle.properties"), "utf8")).toContain("mod_id=example-mod");
    expect(await readFile(join(target, "src/main/java/com/example/mod/ExampleMod.java"), "utf8")).toContain("package com.example.mod");
    expect(await readFile(join(target, "src/main/resources/fabric.mod.json"), "utf8")).toContain('"id": "example-mod"');
  });

  it("rejects invalid Java identifiers and unsupported templates", async () => {
    const root = await makeRoot();

    await expect(initProject(join(root, "invalid"), { loader: "fabric", minecraftVersion: "1.21.1", modId: "not valid", packageName: "com.example.mod" })).rejects.toThrow(/mod id/i);
    await expect(initProject(join(root, "unsupported"), { loader: "forge", minecraftVersion: "1.20.1", modId: "forge_mod", packageName: "com.example.mod" })).rejects.toThrow(/supported/i);
  });

  it("refuses to write into a non-empty target", async () => {
    const root = await makeRoot();
    const target = join(root, "existing");
    await mkdir(target, { recursive: true });
    await writeFile(join(target, "keep.txt"), "keep");

    await expect(initProject(target, { loader: "fabric", minecraftVersion: "1.21.1", modId: "existing_mod", packageName: "com.example.mod" })).rejects.toThrow(/non-empty/i);
  });
});
