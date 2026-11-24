## Summary

<!-- What changed and why. Link the issue. -->

Fixes #

## Type

- [ ] App code (Kotlin)
- [ ] Strings (`MR/base` and `MR/tr`)
- [ ] Documentation or scripts
- [ ] CI

## Checks

- [ ] `bash scripts/ci/docs-check.sh`
- [ ] `bash scripts/ci/strings-check.sh`
- [ ] `bash scripts/ci/policy-check.sh`
- [ ] `bash scripts/ci/egress-check.sh`
- [ ] Android build and unit tests (see `docs/veil/ANDROID_BUILD.md`)

Run result: <!-- unit test / emulator / device model and Android version / NOT TESTED -->

## Rules

- [ ] Tor-only policy is unchanged; the change adds no egress path and no clear-network fallback.
- [ ] Calls, if touched, remain opt-in, off by default and outside Tor, with the existing warnings (or not applicable).
- [ ] Upstream changes carry a `// Veil:` mark; no gate was removed or bypassed.
- [ ] No core, crypto or protocol changes; no `.so`, keystore, `local.properties`, secrets or user data.
- [ ] `veil_` strings exist in both base and tr; the SimpleX count did not grow.
- [ ] Status claims are backed by a recorded run or marked NOT TESTED.
- [ ] Commits are signed off (`-s`).
