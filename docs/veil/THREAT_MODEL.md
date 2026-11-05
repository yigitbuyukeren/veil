# Veil threat model (M1)

Status: draft for independent review. Nothing here has been verified on devices or audited. Statements marked **[ASSUMPTION]** are not yet proven; each maps to a testable criterion in the table at the end.

## 1. Scope

Veil M1 is a closed Android alpha built on the SimpleX stack (unchanged cryptography and protocol), with a Tor-only transport policy: every network connection to relays goes through Tor to `.onion` endpoints and the app fails closed when Tor is unavailable. There are no accounts, phone numbers or emails, and no Veil-operated servers.

Out of scope for M1: voice/video, files and media, backups, multi-device, iOS/desktop, public group discovery, paid features, new cryptography.

## 2. Assets

| Asset | Why it matters | Where it lives |
|---|---|---|
| Message content | Confidentiality, integrity | Device DB (encrypted), in transit E2EE, relay queues (ciphertext) |
| User IP address | Location / identity linkage | Device; hidden from relays and peers only if all egress is via Tor |
| Chat relationships (who talks to whom) | Social graph | Device DB; relay sees queue-level metadata only |
| Contact keys / ratchet state | Impersonation, decryption | Device DB |
| Group membership | Social graph | Device DB; other members |
| Local database key | Protects everything at rest | Device (key handling decided in VEIL-012) |
| Backups / exports | Bypass of at-rest protection | Not offered in M1 |
| Device metadata (model, OS, timezone, install time, notification timing) | Fingerprinting | Device, OS, push infrastructure (none planned) |

## 3. Adversaries

| ID | Adversary | Capabilities | Not assumed |
|---|---|---|---|
| A1 | Message relay operator | Sees queue IDs, ciphertext sizes, timing, connecting address (a Tor exit-less circuit endpoint for `.onion`), can drop/delay/log | Cannot read content (E2EE). Not trusted for availability |
| A2 | Global passive network observer | Sees traffic between user and Tor entry guard and at relay | Not assumed defeated by Tor: end-to-end timing correlation is possible |
| A3 | Malicious counterparty / group member | Receives everything sent to them, can invite others, spam, forward, screenshot, probe | Cannot be prevented from retaining content |
| A4 | Seized / stolen / compromised device | Reads storage; with root or malware reads memory and screen | Out of protection once unlocked or when malware runs |
| A5 | Malicious file/link | Parser exploits, deceptive links | M1 has no files; links are not auto-fetched |
| A6 | Supply chain (upstream, dependencies, build) | Malicious update or dependency | Mitigated only by pinning, review, CI scans |
| A7 | Local network attacker | Blocks Tor, injects, forces fallback | Must not cause clear-text fallback |

## 4. Data flow: who can see what

Calls are off by default. If the user turns them on, call media goes outside Tor through a call (TURN) server: the counterparty does not see the IP (relay-only), the call server sees the IP and the timing. See [TOR_ONLY.md](TOR_ONLY.md).

Mode "Tor-only" is Veil's only mode. "Clear" is shown only to document what Veil must never do.

| Observer | Message content | Sender/receiver IP | Who talks to whom | Message timing and size | Group membership |
|---|---|---|---|---|---|
| Relay operator (Tor-only) | No | No (sees Tor side only) [ASSUMPTION, VEIL-007/008] | Only queue-level, not identities | Yes | No (membership is end-to-end between members) |
| Relay operator (clear, forbidden) | No | **Yes** | Queue-level | Yes | No |
| Tor guard / ISP | No | Yes (sees that user uses Tor) | No | Volume and timing | No |
| Global observer | No | Tor use | Possible by timing correlation | Yes | Possible by correlation |
| Counterparty | Yes | No, if Tor-only | Yes (their own chats) | Yes | Members see each other's display names/IDs |
| Other group members | Group content | No | Group graph they belong to | Yes | Yes |
| Call server (TURN), only if the user turned calls on (outside Tor) | No (call content is end-to-end encrypted) | **Yes** (calls do not go through Tor) | That a call happens between two endpoints it relays | Yes | No |
| Device thief (locked, encrypted DB) | Not without key | n/a | Not without key | n/a | Not without key |
| Device thief (unlocked / malware) | Yes | Yes | Yes | Yes | Yes |

## 5. Protections matrix

| Feature | Protects against | Does not protect against |
|---|---|---|
| E2EE (upstream SimpleX) | A1 content reading, A2 content | Endpoint compromise, malicious peer retaining content |
| Per-contact identity, no account | Cross-chat linkage by A1/A3 | Peer-side correlation from message style, timing, or reuse of a link |
| Tor-only transport | IP disclosure to A1/A3, local ISP seeing relay | Timing correlation by A2 (Tor does not defend against end-to-end timing), Tor entry seeing Tor use, malware |
| Fail-closed | A7 forced downgrade | Availability (the app stops working without Tor) |
| Encrypted local DB | A4 offline theft | Unlocked device, rooted device, key extraction while running |
| Store-and-forward | Both parties never needing to be online together | Relay deleting queues, retention limits |
| No third-party SDKs/analytics | Telemetry-based deanonymization | OS-level/vendor telemetry outside the app |
| Pinned upstream + CI scans | Accidental supply chain drift | Compromised upstream at the pinned version |

## 6. Anonymity limits (normative)

- Veil is **not** "100% anonymous" and must never say so. See `PRIVACY_CLAIMS.md`.
- Tor does not resist an adversary that observes both ends; Veil adds no cover traffic.
- Anyone who has your contact link or group access can interact with you; incognito identities reduce linkage but do not prevent writing-style or behavioral identification.
- Orbot and the Android OS are part of the trusted computing base for network routing.
- Android may expose metadata (app install, process activity, notification channel timing) that Veil cannot remove.

## 7. M1 testable assumptions and acceptance criteria

| # | Assumption / criterion | Verified by | Status |
|---|---|---|---|
| S1 | All relay connections use `.onion` endpoints; no `.onion`-less relay address is accepted | VEIL-007 test + config check | NOT TESTED |
| S2 | With Tor/Orbot stopped, the app opens no sockets to relays and shows a clear offline state | VEIL-008 leak test | NOT TESTED |
| S3 | A packet capture on the device during a send/receive session shows only loopback or SOCKS traffic to Orbot, no direct DNS, HTTP or TCP to the internet | VEIL-008 | NOT TESTED |
| S4 | The app contains no analytics, crash-reporting or ads SDK | dependency review, VEIL-005 | NOT TESTED |
| S5 | No link previews, image or URL fetches happen outside Tor | VEIL-008 | NOT TESTED |
| S6 | First launch asks for no phone, email or account and creates no persistent global ID | VEIL-009 | NOT TESTED |
| S7 | Each contact/group connection uses distinct queues and, where selected, a chat-specific profile | VEIL-009 | NOT TESTED |
| S8 | Two real devices exchange E2EE 1:1 messages over Tor | VEIL-010 | NOT TESTED |
| S9 | Sender goes offline after relay acceptance; receiver gets the message later | VEIL-011 | NOT TESTED |
| S10 | Message state shown (sent / delivered) matches protocol facts | VEIL-011 | NOT TESTED |
| S11 | Chat history is stored in an encrypted DB and survives restart; the file is unreadable without the key | VEIL-012 | NOT TESTED |
| S12 | No message content or contact identifiers appear in logs or in the Android system log | VEIL-005/012/017 | NOT TESTED |
| S13 | Background connection survives reasonable doze/process death scenarios with documented limits | VEIL-013 | NOT TESTED |
| S14 | A 3-member and a 10-participant private group work, with membership and role rules enforced | VEIL-014/015 | NOT TESTED |
| S15 | The UI shows Tor/connection state truthfully and never claims anonymity or safety | VEIL-016 | NOT TESTED |
| S16 | Upstream crypto/protocol sources are unmodified relative to the pinned tag | VEIL-003/017 diff check | NOT TESTED |
| S17 | Release gate has an explicit GO/NO-GO decision recorded with evidence | VEIL-017 | NOT TESTED |

If any of S1, S2, S3 or S11 fail, the release decision is NO-GO.

## 8. Residual risks

Timing correlation; compromised device; malicious peer; upstream vulnerabilities; Orbot or Tor bugs; relay availability and retention policy; legal/terms changes (see `UPSTREAM_LICENSES.md`, `RELAY_USAGE.md`). No independent audit exists.
