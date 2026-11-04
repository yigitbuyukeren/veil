package chat.simplex.common.veil.screens.chat

import kotlin.test.*

class VeilVerifyTest {
  private val code = "12345 67890 11111 22222 33333 44444 55555 66666 77777 88888 99999 00000"

  @Test
  fun twelveGroupsOfFiveInThreeRowsOfFour() {
    val rows = securityCodeRows(code)
    assertEquals(3, rows.size)
    assertTrue(rows.all { it.size == 4 })
    assertEquals(listOf("12345", "67890", "11111", "22222"), rows[0])
    assertEquals(listOf("77777", "88888", "99999", "00000"), rows[2])
    assertTrue(securityCodeGroups(code).all { it.length == 5 })
  }

  @Test
  fun codeWithoutSeparatorsIsCutIntoFives() {
    val groups = securityCodeGroups("1234567890123")
    assertEquals(listOf("12345", "67890", "123"), groups)
  }

  @Test
  fun whitespaceIsIgnored() {
    assertEquals(listOf("11111", "22222"), securityCodeGroups("  11111\n22222  "))
    assertEquals(listOf("11111"), securityCodeGroups("11111"))
    assertTrue(securityCodeGroups("   ").isEmpty())
  }

  @Test
  fun shortLastRowKeepsItsGroups() {
    val rows = securityCodeRows("11111 22222 33333 44444 55555")
    assertEquals(listOf(4, 1), rows.map { it.size })
  }
}
