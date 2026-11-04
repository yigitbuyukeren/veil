# Veil roadmap

## M1 — Tor-only Android closed alpha (this issue package)

**F0 Foundation:** VEIL-001 threat model; 002 legal and relay conditions; 003 pinned upstream; 004 FOSS Android build; 005 CI; 006 package identity.

**F1 Private 1:1 messaging:** VEIL-007 Tor-only relay connection; 008 fail-closed/egress; 009 incognito onboarding; 010 E2EE device-to-device; 011 offline delivery; 012 encrypted persistence; 013 background reliability.

**F2 Small private groups + release gate:** VEIL-014 groups; 015 roles/invites; 016 usable alpha UI; 017 security validation / GO-NO-GO.

**Required M1 proof:** 2 actual Android devices; .onion/Tor-only message flow; sender off / receiver later on; persisted chat history; 3-member group, 10-participant load test; network leak tests. Untested behavior must not be called secure.

## M2 — Usable public beta (NOT in this issue package)

- File/photo/voice messages with metadata minimization.
- Encrypted backups and device transfer with recovery tests.
- Better chat search and pinning, moderation hardening, notifications and battery tuning.
- Design polish, localization, accessibility and separate independent external security review.

## M3 — Evaluated extensions (NOT in this issue package)

- 1:1 voice/video calls only after separate anonymity and media-routing threat model and successful real-device tests.
- iOS and desktop support; multi-device without creating an account correlation identifier.
- Self-hosted relay discovery and fallback for operator independence.

Excluded for now: group voice/video calls, public group discovery, paid features, custom crypto, unbounded public relay usage.
