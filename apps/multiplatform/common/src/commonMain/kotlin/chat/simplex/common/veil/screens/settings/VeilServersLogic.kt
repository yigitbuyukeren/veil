package chat.simplex.common.veil.screens.settings

/** One relay server as the read-only list shows it. */
data class VeilServerRow(val host: String, val onion: Boolean)

/**
 * Hosts of a server address (`smp://key@host1,host2:port`, `xftp://key@host`), one row per host, `.onion` ones marked.
 * Unparseable text gives no rows.
 */
fun veilServerRows(address: String): List<VeilServerRow> {
  val afterScheme = address.substringAfter("://", "")
  if (afterScheme.isEmpty()) return emptyList()
  val hosts = afterScheme.substringAfter('@').substringBefore('/').split(',')
  return hosts.map { it.trim().substringBefore(':') }.filter { it.isNotEmpty() }.map { VeilServerRow(it, it.endsWith(".onion")) }
}
