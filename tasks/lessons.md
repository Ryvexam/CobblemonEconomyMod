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
