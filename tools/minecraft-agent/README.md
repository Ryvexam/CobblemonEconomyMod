# Minecraft Agent

Local CLI and MCP tooling for creating, building, testing, and packaging
Minecraft mods. The first supported template is Fabric 1.21.1.

## Development

```bash
npm install
npm run build
npm test
npm run --silent cli -- inspect --project ../..
npm run mcp
```

The CLI and MCP server share the same local engine. They never deploy to a
remote server in the MVP.

## CLI commands

```bash
# Inspect an existing project without changing it
npm run --silent cli -- inspect --project ../.. --json

# Build with an allowlisted Gradle task and a bounded timeout
npm run --silent cli -- build --project ../.. --task build --timeout 600000

# List generated JARs and SHA-256 hashes
npm run --silent cli -- artifact --project ../.. --json

# Create a new Fabric 1.21.1 project
npm run --silent cli -- init ./mods/example --loader fabric --mc 1.21.1 \
  --mod-id example --package com.example.mod
```

`init` includes the Gradle wrapper and refuses to overwrite a non-empty
directory. Project and artifact paths must remain under the configured local
allowed roots. Build logs are stored in `.minecraft-agent/runs/`.

The `runServer` and game-test commands require a compatible Java runtime and
the Minecraft dependencies to be available locally; they may download Gradle
dependencies on their first run.

## OpenCode MCP integration

Build the server once from this directory:

```bash
npm install
npm run build
```

Add a project-local MCP entry using the absolute path printed by `pwd` (do not
commit a machine-specific path):

```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "servers": {
      "minecraft-agent": {
        "type": "local",
        "command": ["node", "/absolute/path/to/CobblemonEconomyMod/tools/minecraft-agent/dist/mcp.js"],
        "cwd": "/absolute/path/to/CobblemonEconomyMod"
      }
    }
  }
}
```

The server exposes `inspect_project`, `create_project`, `build_project`,
`test_project`, `read_logs`, and `get_artifact`. It only accepts local paths
under its current working directory and never exposes arbitrary shell commands.
