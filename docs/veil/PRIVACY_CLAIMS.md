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
| "Messages and files go only through Tor; calls are the exception." | Same evidence as the relay claim, plus the call opt-in observed on devices (N7) | Calls are off by default; when turned on, call media does not go through Tor |
| "In a call, the other person does not see your IP address, but the call server does." | Relay-only ICE policy observed in a running call (connection type "relay"); N7 | Call content is end-to-end encrypted; the call server sees your IP and the timing. Never worded as anonymity |

## Calls (N7)

Never write "all traffic goes through Tor". The accurate form is "messages and files go only through Tor; calls (if you turn them on) are the exception". Call-related `veil_` strings (`veil_calls_*`, `veil_onb_terms_calls`, `veil_calls_disabled_text`) state limits and measured behaviour, not protections: they say the call does not go through Tor and that the call server sees the IP. Their device evidence (relay type in the call info, warnings shown at each step) is NOT TESTED.

## Alpha disclaimer (must appear in README and in-app)

Veil is an unaudited closed alpha. Do not rely on it where your safety depends on it.

## Review rule

Any change to this list, the README, or UI privacy indicators requires linking the evidence in the pull request description.

## In-app statements pending evidence

Audit of 2026-11-04 of all `veil_` strings in `MR/base` and `MR/tr` (the Turkish text is identical in both) and of the hardcoded Veil interface text. Hardcoded Veil text states no privacy property (the only literals are the debug design gallery and the external addresses).

Category counts: **25 allowed-with-evidence claims whose evidence is still missing** (listed below), **1 allowed-with-evidence claim backed by a static check** (`veil_new_intro`, "Rehber taranmaz": the manifest declares no contacts permission), **0 prohibited claims**, and the remaining keys are neutral descriptions (labels, actions, measured states such as "the SOCKS proxy answers", feature descriptions, honest limits such as `veil_onb_orbot_hint` and `veil_set_tor_note`). Two strings were stronger than reality and were softened on 2026-11-04: `veil_onb_story1_text` (said the identity "only stays on the device"; contacts also receive the profile) and `veil_onb_story3_text` (said chats have no common identifier; that holds only while incognito is on).

The app is not distributed, so the copy stays. **Distribution is NO-GO until each statement below is backed by the evidence in its row, or softened to what is measured** ([M1_RELEASE_REPORT.md](M1_RELEASE_REPORT.md)). Any change to these strings must keep the rules above (allowed list, limits paired, no prohibited wording).

| Key | Text (Turkish) | Claim | Required evidence |
|---|---|---|---|
| `veil_onb_story2_text` | Mesajlar ve dosyalar yalnızca Tor üzerinden gider; aramalar (açarsan) hariç. Tor çalışmıyorsa Veil hiçbir şey göndermez; doğrudan bağlantıya geçmez. | Tor-only, no fallback | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress). Add the Tor caveat (Tor does not protect against traffic correlation); the terms screen has only a general one |
| `veil_tor_unavailable_text` | Veil yalnızca Tor üzerinden bağlanır, doğrudan bağlanmaz. Orbot’u başlatın (SOCKS proxy 127.0.0.1:9050), ardından yeniden deneyin. Tor kullanılabilir olana kadar hiçbir şey gönderilmez veya alınmaz. | Tor-only; nothing sent or received while Tor is down | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress) |
| `veil_socks_down` | SOCKS proxy yanıt vermiyor: hiçbir şey gönderilmez veya alınmaz | nothing sent or received while the proxy is down | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress) |
| `veil_onb_terms_body` | Mesajların, ağdaki aktarma sunucuları üzerinden ve yalnızca Tor aracılığıyla iletilir. Devam etmek için ağın kullanım koşullarını kabul etmen gerekir. Hiçbir ağ tüm riskleri ortadan kaldıramaz. | messages travel only through Tor | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress) |
| `veil_list_tor_banner` | Tor yok — hiçbir şey gönderilmiyor. Orbot’u açtığında bekleyenler gönderilir. | nothing is sent without Tor; pending messages are sent after Orbot returns | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress), plus a queue test: messages composed while Tor is down are sent after it returns (background scenario, clean pair) |
| `veil_chat_tor_down` | Tor yok — hiçbir şey gönderilmiyor | nothing is sent without Tor | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress) |
| `veil_new_invite_route` | Bağlantı, Tor üzerinden röle sunucuları aracılığıyla kurulur. | the connection is made through Tor via relay servers | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress), recorded during pairing on devices |
| `veil_network_locked` | Veil yalnızca Tor üzerinden bağlanır; bu ayar değiştirilemez. | Tor-only cannot be changed | Unit tests of `VeilNetworkPolicy` exist; needed: the same observed in the running app (network settings read-only, `apiSetNetworkConfig` forces the policy) |
| `veil_set_tor_locked_note` | Veil yalnızca Tor üzerinden bağlanır; bu ayar değiştirilemez. | Tor-only cannot be changed | same as `veil_network_locked` |
| `veil_set_net_path_value` | Tor | connection path is Tor | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress) |
| `veil_set_net_direct_value` | Kapalı, açılamaz | direct connection is off and cannot be opened | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress) and the enforcement observed in the running app |
| `veil_set_net_no_tor_value` | Hiçbir şey gönderilmez | nothing is sent without Tor | VEIL-007/008: leak matrix in [NETWORK_EGRESS.md](NETWORK_EGRESS.md) PASS on real devices with Orbot (capture shows only loopback to the proxy; Tor off means no app egress) |
| `veil_set_tor_policy_ok` | Çekirdek yalnızca Tor ağ ayarlarıyla çalışıyor. | the core's active config is Tor-only (a config read, not a traffic measurement) | Active-config check (`VeilNetworkPolicy.isCompliant`) observed in the running app; wording must stay about the configuration |
| `veil_onb_story1_text` | Telefon numarası, e-posta ya da kullanıcı adı istemiyoruz. Veil’in seni tanıdığı bir kayıt yok; profilin cihazında saklanır ve yalnızca bağlandığın kişilerle paylaşılır. | no phone number, e-mail or account | VEIL-009 anonymous start test PASS (partial today: incognito observed on the joining side, invite cancel and block NOT TESTED) |
| `veil_onb_name_text` | Bu ad yalnızca cihazında durur ve bağlandığın kişilere gösterilir. Hesap açılmaz, sunucuya kayıt yapılmaz. | no account, no server registration | VEIL-009 test PASS |
| `veil_onb_story3_text` | Varsayılan olarak her bağlantıda rastgele bir takma kimlik kullanılır. Bunu ayarlardan kapatırsan seçtiğin ad bağlandığın kişilere görünür. | random identity per connection by default (incognito on) | VEIL-009: incognito default observed on a new connection and in a group join on devices; invite cancel and block cases |
| `veil_new_incognito_on` | Bu bağlantıda adın yerine rastgele bir takma kimlik görünür. | random identity on this connection | same as `veil_onb_story3_text` |
| `veil_grp_create_incognito_on` | Bu grupta gerçek adın yerine rastgele bir takma kimlik görünür. | random identity in this group | same as `veil_onb_story3_text`, group join |
| `veil_onb_ntf_text` | Yeni mesajlardan nasıl haberdar olacağını seç. Üç seçenekte de bildirimler bir push sunucusundan geçmez; uygulama mesajları kendisi alır. Sonradan ayarlardan değiştirebilirsin. | notifications do not pass through a push server | Code and manifest review of the FOSS flavor (no push service), plus VEIL-013 background notification run on devices |
| `veil_set_sum_db_encrypted` | Cihazda şifreli | chat database is encrypted on the device | VEIL-012 on devices and an audit of key handling (emulator E7/E8 only today). Needs the caveat: not protected on an unlocked or compromised device |
| `veil_verify_explain` | İki telefonda da aynı kodu görüyorsanız, bağlanırken davet linkini kimsenin araya girip değiştirmediğine dair güçlü bir işaret elde edersiniz. Kodlar farklıysa sohbete güvenmeyin ve yeni bir link ile baştan bağlanın. | equal codes are a strong sign that nobody changed the invitation link | Wrong-contact and key-change cases provoked and observed (E26 NOT TESTED); wording must stay at "sign", not proof |
| `veil_chat_state_sent` | Gönderildi | delivery state wording | Delivery-state semantics observed per state on devices (E12/E13 NOT TESTED) |
| `veil_chat_state_sent_partly` | Kısmen gönderildi | delivery state wording | same as `veil_chat_state_sent` |
| `veil_chat_state_delivered` | Teslim edildi | delivery state wording ("delivered" means the recipient's device acknowledged) | same as `veil_chat_state_sent` |
| `veil_chat_state_delivered_partly` | Kısmen teslim edildi | delivery state wording | same as `veil_chat_state_sent` |
| `veil_adv_migrate_note` | Arşiv şifrelenir ve Tor üzerinden yüklenir. | the migration archive is encrypted and its upload goes through the Tor proxy | two-phone migration with Orbot on, capture shows only loopback to the proxy; migration refused with Orbot off |
