# Veil

**Privacy-first, accountless Android messaging** — closed alpha in development.

Veil is an open-source Android client using the SimpleX messaging stack with Tor-only relay connectivity as its security policy. It ships the official prebuilt SimpleX core (verified by hash and signature) and changes only the app layer. The first target is a **closed Android alpha**, not a finished or audited product.

## What M1 aims to prove

- No phone number or email needed; no central cross-chat user identifier.
- End-to-end encrypted one-to-one messaging through Tor `.onion` relay endpoints.
- Offline store-and-forward: sender may disconnect after relay acceptance; receiver retrieves later.
- Persistent locally encrypted chat history.
- Private small-group chats, invitations and basic membership controls.
- Fail-closed: no automatic clear-network fallback when Tor is unavailable.

## Security disclaimer

Veil is an unaudited closed alpha. Do not rely on it where your safety depends on it.

Tor and end-to-end encryption are **not a guarantee of 100% anonymity**. Endpoint compromise, traffic correlation, malicious participants, device metadata and other risks remain. The Android implementation and the complete set of network egress paths require testing. No release should claim security guarantees without evidence.

## Current state

Veil is an unaudited closed alpha in development. The Tor-only network policy is implemented in the app layer (fail-closed start gate, enforced at a single choke point, network settings locked). It has unit tests and earlier emulator runs; the latest changes are not verified on a device, there is no real-device verification and no independent security audit. The current release decision is NO-GO. [docs/veil/DEVELOPMENT_STATUS.md](docs/veil/DEVELOPMENT_STATUS.md) is the single status source.

## Source & licenses

- Upstream: https://github.com/simplex-chat/simplex-chat
- Upstream Android build: https://github.com/simplex-chat/simplex-chat/blob/stable/apps/multiplatform/README.md
- Public server terms: https://simplex.chat/privacy/
- Upstream code is AGPLv3; licenses and copyright attribution of forked material must be preserved. The upstream brand/logos may be protected separately.

## Documentation

Veil's own documents are in [docs/veil/](docs/veil/); the rest of `docs/` is upstream's.

- Direction: [vision](docs/veil/VISION.md), [design](docs/veil/DESIGN.md), [roadmap](docs/veil/ROADMAP.md).
- Status and release decision: [development status](docs/veil/DEVELOPMENT_STATUS.md), [M1 release report](docs/veil/M1_RELEASE_REPORT.md).
- Security: [SECURITY.md](SECURITY.md), [threat model](docs/veil/THREAT_MODEL.md), [privacy claims policy](docs/veil/PRIVACY_CLAIMS.md), [Tor-only transport](docs/veil/TOR_ONLY.md), [network egress](docs/veil/NETWORK_EGRESS.md).
- Build and sources: [Android build](docs/veil/ANDROID_BUILD.md), [native core decision](docs/veil/NATIVE_CORE.md), [upstream base](docs/veil/UPSTREAM.md), [SOURCES.md](SOURCES.md).
- Licenses: [upstream licenses](docs/veil/UPSTREAM_LICENSES.md), [third-party notices](docs/THIRD_PARTY_NOTICES.md).
