import { describe, expect, it } from "vitest";
import { boundOutput, classifyOutput } from "../src/core/log-parser.js";

describe("Gradle log classification", () => {
  it("classifies dependency resolution failures before other patterns", () => {
    const result = classifyOutput("Could not resolve all files for configuration ':compileClasspath'.", "", "build");

    expect(result.phase).toBe("dependency_resolution");
    expect(result.errors[0]).toContain("Could not resolve");
  });

  it("classifies a Gradle configuration failure", () => {
    const result = classifyOutput("FAILURE: Build failed with an exception.\n* Where:", "", "build");

    expect(result.phase).toBe("gradle_configuration");
  });

  it("classifies compilation failures", () => {
    const result = classifyOutput("Compilation failed; see the compiler error output for details.", "", "build");

    expect(result.phase).toBe("compilation");
  });

  it("classifies mixin loading failures", () => {
    const result = classifyOutput("Mixin apply failed for com.example.ExampleMixin", "", "runServer");

    expect(result.phase).toBe("mod_loading");
  });

  it("keeps unknown process errors as unknown", () => {
    const result = classifyOutput("A custom task failed for an unknown reason", "", "build");

    expect(result.phase).toBe("unknown");
    expect(result.errors).toEqual(["A custom task failed for an unknown reason"]);
  });

  it("does not turn timeout output into a compilation error", () => {
    const result = classifyOutput("Process timed out after 1000ms", "", "build");

    expect(result.phase).not.toBe("compilation");
  });

  it("bounds each output stream to 200000 characters", () => {
    const output = boundOutput("x".repeat(210_000));

    expect(output).toHaveLength(200_000);
  });
});
