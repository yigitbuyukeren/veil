# Veil vision

Veil is a messaging app that asks for no account, knows no route except Tor, and does not claim more than it can show. This document states the idea, the decisions behind it and what is deliberately left out. Current progress is not tracked here; see [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md). The visual and interface rules are in [DESIGN.md](DESIGN.md).

## Main idea

**Veil: a messaging app that asks for no account, knows no way out except Tor, and claims no more than it can measure.**

## Three pillars

1. **Tor is the only route.** All traffic goes through Orbot to `.onion` addresses. When Tor is not available nothing is sent (fail-closed). No setting is left that bypasses this rule. How it is enforced is described in [TOR_ONLY.md](TOR_ONLY.md).
2. **A separate identity for every conversation.** No phone number, no e-mail, no username. Every connection is opened with a random profile by default (incognito). See [PRIVACY_DEFAULTS.md](PRIVACY_DEFAULTS.md).
3. **An honest interface.** The screen shows only what can be measured. Claims such as "you are safe" or a green shield do not appear. Every privacy statement comes from the allowed list in [PRIVACY_CLAIMS.md](PRIVACY_CLAIMS.md) and is paired with its limits.

## How Veil differs from SimpleX

Locked defaults and a design of its own. The cryptography, the protocol and the Haskell core stay with upstream and are not modified. Veil is an app for using the SimpleX network and says so (see [RELAY_USAGE.md](RELAY_USAGE.md)).

## Core decision (2026-11-04)

The native core is **not built from source**. Veil ships the official prebuilt `libsimplex.so` and `libsupport.so` from the SimpleX Chat v7.0.3 release, verified by SHA-256 and by the release GPG signatures; CI enforces this. Anything that would require a change inside the core is not done. It is solved in the app layer instead (for example, hiding the core-created "Ask SimpleX Team" contact in the interface rather than removing it from the core). The reasoning and consequences are in [NATIVE_CORE.md](NATIVE_CORE.md).

## Target user

Someone who cares about privacy, is not technical, and speaks Turkish. A claim such as "safe for people at high risk" is not made until an independent security review has been done. Until then Veil is an unaudited closed alpha ([../../SECURITY.md](../../SECURITY.md)).

## Out of scope

- Voice and video calls (stay disabled until a separate anonymity and media-routing analysis and real-device tests exist).
- Desktop, iOS, remote desktop connection and moving the app to another device (hidden in the interface for now).
- Anything that needs a change to the SimpleX core, cryptography or protocol.
- Group voice/video calls, public group discovery, paid features, unbounded public relay usage (see [ROADMAP.md](ROADMAP.md), "Excluded for now").
