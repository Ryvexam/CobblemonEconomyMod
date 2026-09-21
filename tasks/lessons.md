# Lessons learned

## Public repository confidentiality

- This repository is public on GitHub.
- Never add, commit, or publish tokens, passwords, private keys, credentials,
  personal data, production databases, server logs, crash reports, private
  logs, or machine-specific confidential paths. This includes logs containing
  UUIDs, IP addresses, player names, tokens, absolute home paths, or modpack
  details that should remain private.
- Keep credentials exclusively in Forgejo/GitHub secrets or local untracked
  environment files.
- If logs are needed for documentation, publish only a deliberately sanitized
  excerpt or a reproducible summary, never the raw log file.
- Before finishing a change, inspect the diff and repository status for
  accidental confidential material.

## Pixel-art texture scaling

- To resize a pixel-art texture without cutting it, scale the complete source
  image uniformly around its visual center; do not pass a smaller destination
  height to a blit call that uses the same width/height arguments for the
  source region.

## Minecraft GUI fill colors

- `GuiGraphics.fill()` expects `AARRGGBB`; status colors stored as `RRGGBB`
  need an explicit opaque alpha channel before being used for pixel icons.

## Release version intent

- Do not infer a version bump from post-release commits. Keep the configured
  version and tag plan requested by the maintainer unless they explicitly ask
  for the next release number.

## Quest preview assets

- Do not invent a pixel-art crop when an existing entity skin already has a
  renderer. Render the existing model for entity previews and use an empty
  fallback instead of displaying a misleading barrier item.

- `LivingEntityRenderer` decides name-tag visibility independently of
  `Player.shouldShowName()`. For client-only player previews, use a team with
   `Team.Visibility.NEVER` when the label must be removed completely.

## Quest prerequisites

- A server-side prerequisite check is not enough: the board snapshot must expose the missing prerequisite state to the client before the player attempts acceptance.
- Keep prerequisite status distinct from generic one-time locks, and include the prerequisite quest names in the hover tooltip so the player can act on the requirement.

## Player-facing changelog

- Keep the public changelog focused on changes that affect players: gameplay, UI, content, compatibility, and player-visible fixes.
- Do not list CI/CD, release automation, database migrations, or other implementation details in player-facing release notes.

## Forgejo artifacts

- Forgejo runners cannot use the standard `actions/upload-artifact@v4` implementation; use Forgejo's patched `https://code.forgejo.org/forgejo/upload-artifact@v4` action instead.

## CurseForge release API

- CurseForge's Core API uses a separate `x-api-key` credential; the configured `CURSEFORGE_TOKEN` is accepted by the legacy API with `X-Api-Token`, so use legacy game versions plus the public file listing unless a Core key is explicitly configured.
- Send upload metadata as a multipart form string (or file contents), never as a multipart file attachment; changelog semicolons can otherwise corrupt inline `curl -F` parsing.
- Check every paginated file response before deciding that a release version is missing, otherwise an old release can be uploaded twice.
- CurseForge mod uploads require at least one environment-group version; for Minecraft mods include the Client (`9638`) and/or Server (`9639`) IDs in addition to the Minecraft and loader IDs.
- CurseForge publication is asynchronous: the public file list can remain behind for hours. Treat a successful upload request as accepted and do not retry the same tag merely because the file is not visible yet.
- Fabric entrypoint order is not sufficient for optional integrations: subscribe through Impactor's shared event bus before its service provider is registered, and do not choose a configured provider until the per-world config has loaded.
