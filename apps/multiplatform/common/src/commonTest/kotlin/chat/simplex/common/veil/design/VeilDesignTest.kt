package chat.simplex.common.veil.design

import androidx.compose.ui.geometry.Size
import kotlin.test.*

// ./gradlew :common:testDebugUnitTest --tests 'chat.simplex.common.veil.design.*'
class VeilDesignTest {
  // Expected values were produced by running the prototype's own hash / rng / avatar sequence in node
  // (scratch script expected.js); see the task notes. 3 fixed seeds: two normal masks and one incognito mask.
  private class Case(val name: String, val inc: Boolean, val hash: Long, val rng: List<Double>, val describe: String)

  private val cases = listOf(
    Case(
      "Elif Kaya", false, 1998835423L,
      listOf(0.08838235447183251, 0.5636511067859828, 0.0026290290988981724),
      "grad=31,202;blob=10.074,21.140,19.439;L=4;arc=10.625,15.442,19.074,5.539,0.301,324;orb=27.549,10.354,11.769;" +
        "tri=31.939,37.536,10.300,41.103,19.293,19.501,0.628;tri=15.713,8.756,53.862,27.317,14.866,46.133,0.378"
    ),
    Case(
      "Mert Şahin", false, 1085930913L,
      listOf(0.43864560569636524, 0.565484639024362, 0.5059157230425626),
      "grad=157,203;blob=24.166,16.671,14.522;L=4;arc=39.763,26.973,16.335,6.556,0.343,155;orb=11.562,31.658,14.211;" +
        "tri=56.426,35.349,22.318,56.781,23.917,15.534,0.443;tri=6.274,17.919,31.714,-13.927,48.109,16.507,0.543"
    ),
    Case(
      "Elif Kaya", true, 3012687659L,
      listOf(0.2500948568340391, 0.4313189110253006, 0.9842662778683007),
      "hex=21.001,23.176,59;pat=36;bar=23.512,5.399,18;dots=28.099,42.246;25.010,31.932;7.429,14.589;24.712,16.587;43.949,7.099"
    ),
  )

  @Test
  fun fnvHashMatchesPrototype() {
    for (c in cases) assertEquals(c.hash, fnv1a((if (c.inc) "~" else "") + c.name), "hash of ${c.name} inc=${c.inc}")
  }

  @Test
  fun rngSequenceMatchesPrototype() {
    for (c in cases) {
      val r = maskRng(c.name, c.inc)
      for (expected in c.rng) assertEquals(expected, r(), 0.0, "rng of ${c.name} inc=${c.inc}")
    }
  }

  @Test
  fun maskSpecMatchesPrototype() {
    for (c in cases) assertEquals(c.describe, generateMaskSpec(c.name, c.inc).describe(), "mask of ${c.name} inc=${c.inc}")
  }

  @Test
  fun maskSpecIsDeterministicAndSeedSensitive() {
    assertEquals(generateMaskSpec("Ayşe", false), generateMaskSpec("Ayşe", false))
    assertNotEquals(generateMaskSpec("Ayşe", false), generateMaskSpec("Ayşa", false))
    // incognito uses a different seed ("~" prefix) and a different family
    assertTrue(generateMaskSpec("Ayşe", true).incognito)
    assertEquals(5, generateMaskSpec("Ayşe", true).dots.size)
  }

  @Test
  fun incomingBubbleHasCutTopLeftCorner() {
    val ops = foldBubbleOps(Size(200f, 80f), outgoing = false, radius = 18f, fold = 15f)
    assertEquals(BubbleOp.MoveTo(15f, 0f), ops.first())
    // the path comes back down the left edge to (0, fold) and closes diagonally back to (fold, 0)
    assertTrue(ops.contains(BubbleOp.LineTo(0f, 15f)))
    assertEquals(BubbleOp.Close, ops.last())
    // top-left is never rounded, no operation touches (0,0)
    assertFalse(ops.any { it is BubbleOp.LineTo && it.x == 0f && it.y == 0f })
    // the other three corners are rounded
    assertEquals(3, ops.count { it is BubbleOp.Corner })
  }

  @Test
  fun outgoingBubbleHasCutTopRightCorner() {
    val ops = foldBubbleOps(Size(200f, 80f), outgoing = true, radius = 18f, fold = 15f)
    val cutStart = ops.indexOf(BubbleOp.LineTo(185f, 0f))
    assertTrue(cutStart >= 0)
    assertEquals(BubbleOp.LineTo(200f, 15f), ops[cutStart + 1], "diagonal cut right after the top edge")
    assertFalse(ops.any { it is BubbleOp.LineTo && it.x == 200f && it.y == 0f })
    assertEquals(3, ops.count { it is BubbleOp.Corner })
  }

  @Test
  fun bubbleRadiiAreClampedForTinyBubbles() {
    val ops = foldBubbleOps(Size(10f, 10f), outgoing = false, radius = 18f, fold = 15f)
    assertEquals(BubbleOp.MoveTo(5f, 0f), ops.first())
    assertTrue(ops.filterIsInstance<BubbleOp.Corner>().all { it.r <= 5f })
  }

  @Test
  fun reduceMotionDurations() {
    val m = VeilMotion()
    assertEquals(280, m.lift(reduceMotion = false))
    assertEquals(0, m.lift(reduceMotion = true))
    assertEquals(2400, m.duration(m.ringBreatheMillis, reduceMotion = false))
    assertEquals(0, m.duration(m.ringBreatheMillis, reduceMotion = true))
    // Android: animator duration scale 0 means reduce, anything else does not
    assertTrue(VeilMotion.reduceMotionForAnimatorScale(0f))
    assertFalse(VeilMotion.reduceMotionForAnimatorScale(1f))
    assertFalse(VeilMotion.reduceMotionForAnimatorScale(0.5f))
  }

  @Test
  fun torRingLabelsAreExactTurkishTexts() {
    assertEquals("Proxy yanıt veriyor", torRingLabel(TorRingState.ProxyAnswers))
    assertEquals("Bağlanıyor", torRingLabel(TorRingState.Connecting))
    assertEquals("Tor yok — hiçbir şey gönderilmiyor", torRingLabel(TorRingState.Unavailable))
  }

  @Test
  fun cssGradientLineCoversTheBoxCorners() {
    // 90deg = left to right across the middle
    val (s, e) = cssGradientLine(Size(100f, 40f), 90f)
    assertEquals(0f, s.x, 0.001f); assertEquals(20f, s.y, 0.001f)
    assertEquals(100f, e.x, 0.001f); assertEquals(20f, e.y, 0.001f)
  }
}
