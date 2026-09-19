import { access, readFile, readdir } from "node:fs/promises";
import { constants } from "node:fs";
import { basename, join, relative } from "node:path";
import type { Loader, ProjectInspection } from "./types.js";

async function readIfPresent(file: string): Promise<string> {
  try {
    return await readFile(file, "utf8");
  } catch {
    return "";
  }
}

async function exists(file: string): Promise<boolean> {
  try {
    await access(file, constants.F_OK);
    return true;
  } catch {
    return false;
  }
}

async function collectFiles(root: string, directory = root): Promise<string[]> {
  const entries = await readdir(directory, { withFileTypes: true });
  const files: string[] = [];
  for (const entry of entries) {
    if ([".git", ".gradle", "build", "node_modules", "run"].includes(entry.name)) {
      continue;
    }
    const file = join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...await collectFiles(root, file));
    } else {
      files.push(relative(root, file));
    }
  }
  return files.sort();
}

function detectLoader(markers: Record<Exclude<Loader, "unknown">, boolean>): { loader: Loader; diagnostics: string[] } {
  const detected = (Object.entries(markers) as Array<[Exclude<Loader, "unknown">, boolean]>)
    .filter(([, present]) => present)
    .map(([loader]) => loader);
  if (detected.length > 1) {
    return { loader: "unknown", diagnostics: ["Conflicting loader markers detected."] };
  }
  return { loader: detected[0] ?? "unknown", diagnostics: detected.length === 0 ? ["Loader could not be detected."] : [] };
}

function propertyValues(properties: string, key: string): string[] {
  const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return [...properties.matchAll(new RegExp(`^\\s*${escapedKey}\\s*=\\s*(.+?)\\s*$`, "gm"))].map((match) => match[1]);
}

function collectModIds(fabricJson: string): { ids: string[]; malformed: boolean } {
  if (!fabricJson) return { ids: [], malformed: false };
  try {
    const parsed = JSON.parse(fabricJson) as { id?: unknown };
    return { ids: typeof parsed.id === "string" && parsed.id.length > 0 ? [parsed.id] : [], malformed: false };
  } catch {
    return { ids: [], malformed: true };
  }
}

function inferGradleTasks(buildFiles: string, loader: Loader): string[] {
  const tasks = new Set(["assemble", "build", "check", "jar", "tasks", "test"]);
  for (const match of buildFiles.matchAll(/tasks\.(?:register|create|named)\s*(?:<[^>]+>)?\s*\(\s*["']([^"']+)["']/g)) {
    tasks.add(match[1]);
  }
  for (const match of buildFiles.matchAll(/\btask\s+([A-Za-z][A-Za-z0-9_-]*)/g)) {
    tasks.add(match[1]);
  }
  if (loader === "fabric") {
    tasks.add("runClient");
    tasks.add("runServer");
  }
  return [...tasks].sort();
}

export async function inspectProject(root: string): Promise<ProjectInspection> {
  const fabricJsonPath = join(root, "src/main/resources/fabric.mod.json");
  const [properties, buildGradle, buildGradleKts, fabricJson, fabricJsonPresent, files] = await Promise.all([
    readIfPresent(join(root, "gradle.properties")),
    readIfPresent(join(root, "build.gradle")),
    readIfPresent(join(root, "build.gradle.kts")),
    readIfPresent(join(root, "src/main/resources/fabric.mod.json")),
    exists(fabricJsonPath),
    collectFiles(root)
  ]);

  const buildFiles = `${buildGradle}\n${buildGradleKts}`;
  const detected = detectLoader({
    fabric: /fabric-loom|net\.fabricmc\.fabric-loader|fabric\.mod\.json/.test(buildFiles) || Boolean(fabricJson),
    forge: /net\.minecraftforge|forgegradle|mods\.toml/.test(buildFiles),
    neoforge: /net\.neoforged|neoforge\.mods\.toml/.test(buildFiles)
  });
  const diagnostics = [...detected.diagnostics];
  const minecraftVersions = propertyValues(properties, "minecraft_version");
  const distinctMinecraftVersions = [...new Set(minecraftVersions)];
  if (distinctMinecraftVersions.length === 0) diagnostics.push("Minecraft version is missing from gradle.properties.");
  if (distinctMinecraftVersions.length > 1) diagnostics.push("Conflicting Minecraft versions detected.");
  const metadata = collectModIds(fabricJson);
  if (fabricJsonPresent && metadata.malformed) diagnostics.push("fabric.mod.json is malformed.");
  const gradleWrapper = await exists(join(root, "gradlew"))
    ? join(root, "gradlew")
    : await exists(join(root, "gradlew.bat")) ? join(root, "gradlew.bat") : null;
  if (!gradleWrapper) diagnostics.push("Gradle wrapper is missing.");

  return {
    root,
    loader: detected.loader,
    minecraftVersion: distinctMinecraftVersions[0] ?? null,
    javaVersion: propertyValues(properties, "java_version")[0]
      ?? propertyValues(properties, "org.gradle.java.home")[0]
      ?? buildFiles.match(/(?:VERSION_|release\.set\()\s*([0-9]+)/)?.[1]
      ?? null,
    modIds: metadata.ids,
    gradleWrapper,
    gradleTasks: inferGradleTasks(buildFiles, detected.loader),
    sourceFiles: files.filter((file) => file.startsWith("src/")),
    diagnostics
  };
}
