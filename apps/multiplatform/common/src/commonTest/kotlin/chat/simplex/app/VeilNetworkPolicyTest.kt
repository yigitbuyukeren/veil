package chat.simplex.app

import chat.simplex.common.model.*
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.app.VeilNetworkPolicyTest'
class VeilNetworkPolicyTest {
  private fun permissive() = NetCfg.defaults.copy(
    socksProxy = null,
    socksMode = SocksMode.Onion,
    hostMode = HostMode.Public,
    requiredHostMode = false,
    smpProxyMode = SMPProxyMode.Always,
    smpProxyFallback = SMPProxyFallback.Allow,
    smpWebPortServers = SMPWebPortServers.All
  )

  @Test
  fun enforceClosesEveryClearNetworkOption() {
    val c = VeilNetworkPolicy.enforce(permissive())
    assertEquals(":9050", c.socksProxy)
    assertEquals(SocksMode.Always, c.socksMode)
    assertEquals(HostMode.OnionViaSocks, c.hostMode)
    assertTrue(c.requiredHostMode)
    assertEquals(SMPProxyMode.Never, c.smpProxyMode)
    assertEquals(SMPProxyFallback.Prohibit, c.smpProxyFallback)
    assertEquals(SMPWebPortServers.Off, c.smpWebPortServers)
  }

  @Test
  fun permissiveConfigIsNotCompliantButEnforcedIs() {
    assertFalse(VeilNetworkPolicy.isCompliant(permissive()))
    assertFalse(VeilNetworkPolicy.isCompliant(NetCfg.defaults))
    assertTrue(VeilNetworkPolicy.isCompliant(VeilNetworkPolicy.enforce(NetCfg.defaults)))
  }

  @Test
  fun enforceKeepsTimeoutsAndOtherTuning() {
    val c = VeilNetworkPolicy.enforce(NetCfg.proxyDefaults.copy(rcvConcurrency = 3))
    assertEquals(3, c.rcvConcurrency)
  }

  private fun fakeProxy(reply: ByteArray?): Pair<ServerSocket, Int> {
    val ss = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
    thread(isDaemon = true) {
      try {
        ss.accept().use { s ->
          s.getInputStream().readNBytes(3)
          if (reply != null) { s.getOutputStream().write(reply); s.getOutputStream().flush() }
          Thread.sleep(300)
        }
      } catch (_: Exception) {}
    }
    return ss to ss.localPort
  }

  @Test
  fun readyWhenSocks5ProxyAnswersNoAuth() {
    val (ss, port) = fakeProxy(byteArrayOf(0x05, 0x00))
    try { assertTrue(VeilNetworkPolicy.torProxyReady("127.0.0.1", port, 2000)) } finally { ss.close() }
  }

  @Test
  fun notReadyWhenListenerIsNotSocks() {
    val (ss, port) = fakeProxy("HTTP".toByteArray())
    try { assertFalse(VeilNetworkPolicy.torProxyReady("127.0.0.1", port, 2000)) } finally { ss.close() }
  }

  @Test
  fun notReadyWhenNothingListens() {
    val port = ServerSocket(0).use { it.localPort } // freed immediately
    assertFalse(VeilNetworkPolicy.torProxyReady("127.0.0.1", port, 1000))
  }
}
