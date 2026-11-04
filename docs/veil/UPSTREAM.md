# Upstream base

| Field | Value |
|---|---|
| Source | https://github.com/simplex-chat/simplex-chat |
| Release | `v7.0.3` (stable), published 2026-09-26 |
| Commit | `b3907c9de7a2c596075762bf05ba3e1d2532aff1` (committed 2026-09-26T14:28:35Z) |
| Local tag | `upstream-v7.0.3` |
| License | AGPL-3.0 (see `LICENSE`, `docs/UPSTREAM_LICENSES.md`) |
| Imported | 2026-11-04, by merging the tag with `--allow-unrelated-histories`; full upstream history and authorship are preserved |

Verify the base: `git merge-base --is-ancestor b3907c9de7a2c596075762bf05ba3e1d2532aff1 HEAD` (exit 0), and `git diff upstream-v7.0.3 HEAD -- src apps/multiplatform/common` shows Veil's own changes only.

## Differences from upstream in the import

- `README.md` is Veil's (upstream's remains in history).
- `.github/` removed (workflows, actions, issue templates, funding, codeowners): upstream release, CLA and website automation must not run in this repository. Veil CI is added in VEIL-005.
- `.gitignore` extended with secret, signing, local-config and build-output patterns.
- `docs/CONTRIBUTING.md`: one upstream paragraph about third-party tooling removed.
- `docs/` holds Veil documents. No cryptography, protocol or core source file was changed by the import.

## Remote

```
git remote add upstream https://github.com/simplex-chat/simplex-chat.git
```

## Manual update procedure

1. Read upstream release notes and security advisories for the new stable tag.
2. `git fetch upstream tag vX.Y.Z`
3. Create a branch, `git merge vX.Y.Z`; resolve conflicts, keeping Veil's Tor-only policy changes.
4. Review the diff of cryptography/protocol sources; they should come only from upstream.
5. Run the build and the release-gate checks (VEIL-004/005/008), update this file (tag, SHA, date) and `docs/UPSTREAM_LICENSES.md`.
6. Re-check the relay conditions in `docs/RELAY_USAGE.md`. The relay terms require protocol specs not older than one year, so update upstream at least that often.

## Secret scan note

`tests/fixtures/tls/server.key` is an upstream public test fixture (not a Veil secret). VEIL-005 secret scanning must allow-list it explicitly.
