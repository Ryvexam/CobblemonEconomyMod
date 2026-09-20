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
