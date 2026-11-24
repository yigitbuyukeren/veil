# Contributing to Veil

Veil is an independent Android client for the SimpleX network with a Tor-only messaging policy. It is an unaudited closed alpha: the release decision is **NO-GO**, and [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md) is the single status source. Contributions are welcome, but they are evidence-gated: compiling is not proof that something works, and no behavior is called working without a recorded run.

Read this page before opening an issue or a pull request. Upstream's [contributing guide](../CONTRIBUTING.md) still applies to code structure and style. By participating you agree to the [code of conduct](../CODE_OF_CONDUCT.md).

## Start with an issue

- Open an issue first: describe the problem, the proposed change and the evidence you can provide. Wait for an answer before writing a large patch.
- Small fixes (a typo, a broken link, a failing check) can go straight to a pull request.
- Security problems do not go to public issues. Follow [SECURITY.md](../../SECURITY.md).
- Never paste messages, contact names, invite links, addresses, keys, passwords or database contents into an issue, a pull request, a screenshot or a log. Censor device identifiers and anything that identifies a person.

## What is welcome now

The bottleneck is device evidence, not features. Useful without a phone:

- Documentation fixes and the English fallback review of `MR/base` strings.
- Turkish wording and translation review.
- Emulator scenarios in `scripts/emulator/` and the checks in `scripts/ci/`.
- Accessibility review (TalkBack, large text, 48 dp targets) and screenshots for [DESIGN.md](DESIGN.md).
- Reviewing the in-app statements in [PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md) against recorded evidence.

**Blocked until two real Android devices and Orbot are available:** pairing and messages over Tor, offline delivery, the leak matrix, 3- and 10-member groups, background and Doze behavior. [ROADMAP.md](ROADMAP.md) lists them with acceptance gates. An emulator run does not replace device evidence; keep the state NOT TESTED.

Out of scope for now (see "Excluded for now" in [ROADMAP.md](ROADMAP.md)): group calls, public group discovery, paid features, custom cryptography, changes to the SimpleX core or building it from source, and unbounded public relay usage.

## Rules that are not negotiable

1. **Tor-only policy.** Messages and files leave the app only through the SOCKS proxy to Tor `.onion` relays. No direct connection, no clear-network fallback, no new egress path. The policy is enforced at `ChatController.apiSetNetworkConfig`; the network settings in the UI stay read-only.
2. **Calls stay an opt-in outside Tor.** Off by default, confirmed for every outgoing call and every accept, relay-only ICE. Do not soften the warnings, labels or the relay policy.
3. **`// Veil:` gates.** Changes to upstream files carry a `// Veil:` comment. Do not remove or bypass a gate, and do not change upstream behavior without one. Upstream screens stay reachable only where the design allows it ([DESIGN.md](DESIGN.md)).
4. **No core, crypto or protocol changes.** The native core is the official prebuilt library, verified by hash and signature ([NATIVE_CORE.md](NATIVE_CORE.md)). Never commit `.so` files, keystores, `local.properties`, `.env` files, tokens or user data.
5. **No unevidenced claims.** Missing evidence means NOT TESTED, and an issue stays open until independent review. The wording rules are in [PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md); `docs-check.sh` and `strings-check.sh` fail on prohibited statements.
6. **Strings.** Every `veil_` key exists in both `MR/base` and `MR/tr`, and neither may be empty. Turkish is primary, English is the fallback. SimpleX branding may not grow: the ratchet in `scripts/ci/strings-simplex-baseline.txt` only goes down.
7. **Status documents.** Change a state only with a recorded test; put the result in the evidence document ([EMULATOR_TESTS.md](EMULATOR_TESTS.md), [NETWORK_EGRESS.md](NETWORK_EGRESS.md)), not in a new claim.

## Pull requests

- Branch from `main`, keep the change focused, and reference the issue.
- Follow the existing commit style, for example `feat(veil): ...`, `fix(veil): ...`, `docs(veil): ...`.
- Sign off every commit (`git commit -s`). Contributions are accepted under AGPL-3.0, inbound = outbound; there is no CLA.
- Run the checks that apply:

  ```
  bash scripts/ci/docs-check.sh
  bash scripts/ci/strings-check.sh
  bash scripts/ci/policy-check.sh
  bash scripts/ci/egress-check.sh
  ```

  For the Android build and unit tests, see [ANDROID_BUILD.md](ANDROID_BUILD.md). Use JDK 17 and `JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"`; a Turkish locale breaks resource generation.
- In the pull request, state what you ran and where: unit test, emulator or a named device. "Builds" is not behavior evidence. If you could not run it, write NOT TESTED.
- A push to `main` runs Veil CI. Do not force push a shared branch.

## Review

The maintainer reviews every change. Expect requests for evidence rather than opinions, and expect a "not now" for work that belongs to a later milestone. Issues are closed only after the evidence is recorded, not when the code merges.
