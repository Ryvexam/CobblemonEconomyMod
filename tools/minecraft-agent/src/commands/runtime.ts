import type { ArtifactInfo, ProjectInspection, RunResult } from "../core/types.js";
import type { GradleRunOptions } from "../core/gradle-runner.js";
import type { InitProjectOptions } from "../core/template-manager.js";

export interface CommandRuntime {
  allowedRoots: string[];
  inspectProject: (root: string) => Promise<ProjectInspection>;
  runGradle: (options: GradleRunOptions) => Promise<RunResult>;
  listArtifacts: (root: string) => Promise<ArtifactInfo[]>;
  copyArtifact: (root: string, fileName: string, outputRoot: string) => Promise<ArtifactInfo>;
  initProject: (target: string, options: InitProjectOptions) => Promise<string>;
}

export interface CommandIo {
  stdout: (value: string) => void;
  stderr: (value: string) => void;
}

export function print(value: unknown, json: boolean, io: CommandIo): void {
  io.stdout(json ? `${JSON.stringify(value)}\n` : `${typeof value === "string" ? value : JSON.stringify(value, null, 2)}\n`);
}
