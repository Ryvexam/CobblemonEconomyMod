import { chmod, mkdir, readdir, readFile, stat, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { join, relative } from "node:path";

export interface InitProjectOptions {
  loader: string;
  minecraftVersion: string;
  modId: string;
  packageName: string;
}

const templateRoot = fileURLToPath(new URL("../../templates/", import.meta.url));

function assertIdentifiers(options: InitProjectOptions): void {
  if (!/^[a-z0-9][a-z0-9_.-]*$/.test(options.modId)) {
    throw new Error(`Invalid mod ID: ${options.modId}`);
  }
  if (!options.packageName.split(".").every((part) => /^[A-Za-z_][A-Za-z0-9_]*$/.test(part))) {
    throw new Error(`Invalid Java package name: ${options.packageName}`);
  }
}

async function templateFiles(directory: string, root = directory): Promise<string[]> {
  const entries = await readdir(directory, { withFileTypes: true });
  const files: string[] = [];
  for (const entry of entries) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...await templateFiles(path, root));
    } else {
      files.push(relative(root, path));
    }
  }
  return files;
}

function substitute(value: string, options: InitProjectOptions): string {
  return value
    .replaceAll("__MOD_ID__", options.modId)
    .replaceAll("__PACKAGE__", options.packageName)
    .replaceAll("__PACKAGE_PATH__", options.packageName.replaceAll(".", "/"));
}

export async function initProject(target: string, options: InitProjectOptions): Promise<string> {
  if (options.loader !== "fabric" || options.minecraftVersion !== "1.21.1") {
    throw new Error("Only the Fabric 1.21.1 template is supported");
  }
  assertIdentifiers(options);

  const template = join(templateRoot, "fabric-1.21.1");
  const targetStats = await stat(target).catch(() => null);
  if (targetStats) {
    if (!targetStats.isDirectory()) throw new Error("Target already exists and is not a directory");
    if ((await readdir(target)).length > 0) throw new Error("Target directory is non-empty");
  } else {
    await mkdir(target, { recursive: true });
  }

  for (const templateFile of await templateFiles(template)) {
    const destination = join(target, substitute(templateFile, options));
    const source = await readFile(join(template, templateFile));
    const contents = templateFile.endsWith(".jar")
      ? source
      : substitute(source.toString("utf8"), options);
    await mkdir(join(destination, ".."), { recursive: true });
    await writeFile(destination, contents);
    if (templateFile === "gradlew") {
      await chmod(destination, 0o755);
    }
  }

  return target;
}
