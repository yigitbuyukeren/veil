# Privacy claims policy

Veil may only publish a privacy or security claim that is (a) on the allowed list, (b) backed by a passing test or documented evidence recorded in the release report, and (c) paired with its limits. Anything else is prohibited.

## Prohibited wording

- "100% anonymous", "completely anonymous", "untraceable", "unhackable", "military-grade", "NSA-proof", "guaranteed private/secure".
- Any claim about protection from a global observer, a compromised device, or a malicious contact.
- Describing alpha builds as audited, safe for high-risk users, or production-ready.

## Allowed claims (when evidence exists)

| Claim | Evidence required | Required caveat |
|---|---|---|
| "No phone number, email or account is required." | VEIL-009 test | — |
| "Messages are end-to-end encrypted using the SimpleX protocol." | Upstream protocol unchanged (VEIL-003/017 diff) | The recipient can keep what you send |
| "Veil connects to relays only through Tor and does not fall back to a direct connection." | VEIL-007, 008 leak tests PASS | Tor does not protect against traffic correlation |
| "Chat history is stored encrypted on your device." | VEIL-012 | Not protected on an unlocked or compromised device |
| "Messages can be delivered while the recipient is offline." | VEIL-011 | Relays may drop queued messages |
| "Veil is an app for using the SimpleX network." | Always shown (relay conditions) | — |

## Alpha disclaimer (must appear in README and in-app)

Veil is an unaudited closed alpha. Do not rely on it where your safety depends on it.

## Review rule

Any change to this list, the README, or UI privacy indicators requires linking the evidence in the pull request description.
