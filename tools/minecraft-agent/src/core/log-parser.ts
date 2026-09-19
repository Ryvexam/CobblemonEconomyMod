import type { RunPhase } from "./types.js";

export const MAX_STREAM_OUTPUT = 200_000;

export function boundOutput(output: string): string {
  return output.length <= MAX_STREAM_OUTPUT ? output : output.slice(0, MAX_STREAM_OUTPUT);
}

function relevantErrors(output: string): string[] {
  return output
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .filter((line) => /could not resolve|could not find|failure:|compilation failed|error:|exception|mixin apply failed|failed/i.test(line))
    .slice(0, 20);
}

export function classifyOutput(stdout: string, stderr: string, task: string): { phase: RunPhase; errors: string[] } {
  const combined = `${stdout}\n${stderr}`;
  const errors = relevantErrors(combined);

  if (/could not resolve|could not find/i.test(combined)) {
    return { phase: "dependency_resolution", errors };
  }
  if (/mixin apply failed|mod loading|failed to load mod/i.test(combined)) {
    return { phase: "mod_loading", errors };
  }
  if (/compilation failed|java compilation|error:\s/i.test(combined)) {
    return { phase: "compilation", errors };
  }
  if (/failure:\s*build failed|configuration failed|script compilation/i.test(combined)) {
    return { phase: "gradle_configuration", errors };
  }

  if (/runserver|runclient|gameruntest/i.test(task)) {
    return { phase: "server_startup", errors };
  }
  if (/build|remap|jar/i.test(task) && /build successful/i.test(combined)) {
    return { phase: "packaging", errors };
  }

  return { phase: "unknown", errors: errors.length > 0 ? errors : relevantErrors(combined || "unknown process failure") };
}
