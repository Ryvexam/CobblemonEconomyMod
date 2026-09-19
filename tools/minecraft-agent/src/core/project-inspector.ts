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

function propertyValue(properties: string, key: string): string | null {
  const line = properties.match(new RegExp(`^\\s*${key.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}\\s*=\\s*(.+?)\\s*$`, "m"));
  return line?.[1] ?? null;
}

function collectModIds(fabricJson: string): string[] {
  if (!fabricJson) return [];
  try {
    const parsed = JSON.parse(fabricJson) as { id?: unknown };
    return typeof parsed.id === "string" && parsed.id.length > 0 ? [parsed.id] : [];
  } catch {
    return [];
  }
}

export async function inspectProject(root: string): Promise<ProjectInspection> {
  const [properties, buildGradle, buildGradleKts, fabricJson, files] = await Promise.all([
    readIfPresent(join(root, "gradle.properties")),
    readIfPresent(join(root, "build.gradle")),
    readIfPresent(join(root, "build.gradle.kts")),
    readIfPresent(join(root, "src/main/resources/fabric.mod.json")),
    collectFiles(root)
  ]);

  const buildFiles = `${buildGradle}\n${buildGradleKts}`;
  const detected = detectLoader({
    fabric: /fabric-loom|net\.fabricmc\.fabric-loader|fabric\.mod\.json/.test(buildFiles) || Boolean(fabricJson),
    forge: /net\.minecraftforge|forgegradle|mods\.toml/.test(buildFiles),
    neoforge: /net\.neoforged|neoforge\.mods\.toml/.test(buildFiles)
  });
  const diagnostics = [...detected.diagnostics];
  const gradleWrapper = await exists(join(root, "gradlew"))
    ? join(root, "gradlew")
    : await exists(join(root, "gradlew.bat")) ? join(root, "gradlew.bat") : null;
  if (!gradleWrapper) diagnostics.push("Gradle wrapper is missing.");

  return {
    root,
    loader: detected.loader,
    minecraftVersion: propertyValue(properties, "minecraft_version"),
    javaVersion: propertyValue(properties, "java_version") ?? propertyValue(properties, "org.gradle.java.home"),
    modIds: collectModIds(fabricJson),
    gradleWrapper,
    gradleTasks: [],
    sourceFiles: files.filter((file) => file.startsWith("src/")),
    diagnostics
  };
}
