# Minecraft Agent

Local CLI and MCP tooling for creating, building, testing, and packaging
Minecraft mods. The first supported template is Fabric 1.21.1.

## Development

```bash
npm install
npm run build
npm test
npm run cli -- inspect --project ../..
npm run mcp
```

The CLI and MCP server share the same local engine. They never deploy to a
remote server in the MVP.
