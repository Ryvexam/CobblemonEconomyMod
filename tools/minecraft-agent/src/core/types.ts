export type Loader = "fabric" | "forge" | "neoforge" | "unknown";

export interface ProjectInspection {
  root: string;
  loader: Loader;
  minecraftVersion: string | null;
  javaVersion: string | null;
  modIds: string[];
  gradleWrapper: string | null;
  gradleTasks: string[];
  sourceFiles: string[];
  diagnostics: string[];
}

export type RunPhase =
  | "dependency_resolution"
  | "gradle_configuration"
  | "compilation"
  | "server_startup"
  | "mod_loading"
  | "functional_test"
  | "packaging"
  | "unknown";

export type RunStatus = "success" | "process_failure" | "timeout";

export interface RunResult {
  runId: string;
  status: RunStatus;
  phase: RunPhase;
  exitCode: number | null;
  durationMs: number;
  stdout: string;
  stderr: string;
  errors: string[];
  logFile: string;
}

export interface ArtifactInfo {
  fileName: string;
  path: string;
  size: number;
  sha256: string;
}
