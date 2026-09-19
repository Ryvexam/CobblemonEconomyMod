import { describe, expect, it } from "vitest";
import { packageName } from "../src/index.js";

describe("minecraft-agent package", () => {
  it("exposes the stable package name", () => {
    expect(packageName).toBe("minecraft-agent");
  });
});
