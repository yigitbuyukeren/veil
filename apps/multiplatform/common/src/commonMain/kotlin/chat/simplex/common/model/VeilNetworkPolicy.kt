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

  /** True only when [cfg] already satisfies the policy. */
  fun isCompliant(cfg: NetCfg): Boolean = cfg == enforce(cfg)

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
