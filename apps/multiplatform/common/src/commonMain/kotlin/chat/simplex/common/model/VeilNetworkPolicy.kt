package chat.simplex.common.model

import java.net.InetSocketAddress
import java.net.Socket

/**
 * Veil network policy: relay traffic only through a local Tor SOCKS proxy (Orbot, 127.0.0.1:9050) to `.onion` hosts.
 * There is no clear-network fallback. See docs/TOR_ONLY.md.
 */
object VeilNetworkPolicy {
  const val SOCKS_HOST = "127.0.0.1"
  const val SOCKS_PORT = 9050

  /** Features whose traffic does not go through the SMP/XFTP core and cannot be routed through the policy proxy: off in M1. */
  const val LINK_PREVIEWS_ENABLED = false
  const val CALLS_ENABLED = false

  /**
   * Calls are an explicit user opt-in (default off): signalling goes through the core over Tor, but WebRTC media does not.
   * Every call-related surface reads this one function.
   */
  fun callsAllowed(optIn: Boolean): Boolean = CALLS_ENABLED || optIn
  fun callsAllowed(): Boolean = callsAllowed(ChatController.appPrefs.veilCallsOptIn.get())

  /** Calls always use a TURN relay so the other party never sees the user's IP; the user preference cannot weaken this. */
  @Suppress("UNUSED_PARAMETER")
  fun callRelayPolicy(userPreference: Boolean): Boolean = true

  /** Network settings that could weaken the policy are shown read-only in the UI (the enforcement itself is in [enforce]). */
  const val SETTINGS_LOCKED = true

  /** Overrides every user-visible network preference that could route traffic outside Tor. */
  fun enforce(cfg: NetCfg): NetCfg = cfg.copy(
    socksProxy = ":$SOCKS_PORT",
    socksMode = SocksMode.Always,
    hostMode = HostMode.OnionViaSocks,
    requiredHostMode = true,
    smpProxyMode = SMPProxyMode.Never,
    smpProxyFallback = SMPProxyFallback.Prohibit,
    smpWebPortServers = SMPWebPortServers.Off
  )

  /**
   * The core renders the proxy as `[auth@]host:port` (simplexmq `SocksProxyWithAuth`: empty auth = isolate by auth, "@" = no auth,
   * "user:pw@" = credentials; host is a `Show SockAddr` IP such as 127.0.0.1 or [::1], and may be omitted when typed in).
   * Only a loopback host on port 9050 without credentials is accepted.
   */
  private val COMPLIANT_PROXY = Regex("""^@?(127\.0\.0\.1|localhost|\[::1\])?:$SOCKS_PORT$""")

  fun isCompliantProxy(proxy: String?): Boolean = proxy != null && COMPLIANT_PROXY.matches(proxy)

  /** True only when [cfg] already satisfies the policy; the proxy string is compared by [isCompliantProxy] since the core may normalize it. */
  fun isCompliant(cfg: NetCfg): Boolean = isCompliantProxy(cfg.socksProxy) && cfg.copy(socksProxy = enforce(cfg).socksProxy) == enforce(cfg)

  /**
   * Readiness probe: the SOCKS5 proxy answers a no-auth greeting. This proves a SOCKS listener exists,
   * not that Tor has built circuits; circuit failures surface as connection errors, never as direct connections.
   */
  fun torProxyReady(host: String = SOCKS_HOST, port: Int = SOCKS_PORT, timeoutMs: Int = 3000): Boolean =
    try {
      Socket().use { s ->
        s.soTimeout = timeoutMs
        s.connect(InetSocketAddress(host, port), timeoutMs)
        s.getOutputStream().apply { write(byteArrayOf(0x05, 0x01, 0x00)); flush() }
        val reply = ByteArray(2)
        var n = 0
        while (n < 2) {
          val r = s.getInputStream().read(reply, n, 2 - n)
          if (r < 0) break
          n += r
        }
        n == 2 && reply[0] == 0x05.toByte() && reply[1] == 0x00.toByte()
      }
    } catch (e: Exception) {
      false
    }
}

/** Veil branding policy: no upstream promotion, fundraising or support-contact surfaces. */
object VeilBranding {
  const val UPSTREAM_PROMOTIONS = false

  /** The core adds an "Ask SimpleX Team" contact card to every new profile; hide it from contact lists. */
  const val UPSTREAM_TEAM_CARD_NAME = "Ask SimpleX Team"
  fun isUpstreamTeamCard(chat: Chat): Boolean =
    !UPSTREAM_PROMOTIONS && (chat.chatInfo as? ChatInfo.Direct)?.contact?.profile?.displayName == UPSTREAM_TEAM_CARD_NAME
}
