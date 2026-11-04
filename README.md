# Veil

**Privacy-first, accountless Android messaging** — development plan.

Veil is a planned open-source Android client using the SimpleX messaging stack with Tor-only relay connectivity as its security policy. The first target is a **closed Android alpha**, not a finished or audited product.

## What M1 aims to prove

- No phone number or email needed; no central cross-chat user identifier.
- End-to-end encrypted one-to-one messaging through Tor `.onion` relay endpoints.
- Offline store-and-forward: sender may disconnect after relay acceptance; receiver retrieves later.
- Persistent locally encrypted chat history.
- Private small-group chats, invitations and basic membership controls.
- Fail-closed: no automatic clear-network fallback when Tor is unavailable.

## Security disclaimer

Tor and end-to-end encryption are **not a guarantee of 100% anonymity**. Endpoint compromise, traffic correlation, malicious participants, device metadata and other risks remain. The Android implementation and the complete set of network egress paths require testing. No release should claim security guarantees without evidence.

## Current state

Planning / Issues only. No Veil implementation, independent security audit or successful Android device verification yet.

## Source & licenses

- Upstream: https://github.com/simplex-chat/simplex-chat
- Upstream Android build: https://github.com/simplex-chat/simplex-chat/blob/stable/apps/multiplatform/README.md
- Public server terms: https://simplex.chat/privacy/
- Upstream code is AGPLv3; licenses and copyright attribution of forked material must be preserved. The upstream brand/logos may be protected separately.

See [ROADMAP.md](ROADMAP.md) and [SECURITY.md](SECURITY.md).

Veil is 100% anonymous.
