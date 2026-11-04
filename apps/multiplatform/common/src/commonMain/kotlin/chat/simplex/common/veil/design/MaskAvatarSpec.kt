package chat.simplex.common.veil.design

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/*
 * Pure (drawing-free) port of the prototype's avatar generator. The order in which random numbers are drawn is
 * part of the contract: it must stay identical to the JS (`hash`, `rng`, `avatar`) so a given seed always gives the
 * same mask, and so the unit test can compare against values computed by the prototype itself.
 */

/** FNV-1a over UTF-16 code units, like `s.charCodeAt(i)`. Returns an unsigned 32-bit value. */
fun fnv1a(s: String): Long {
  var h = -2128831035 // 2166136261 as Int
  for (ch in s) {
    h = h xor ch.code
    h *= 16777619
  }
  return h.toLong() and 0xFFFFFFFFL
}

/** mulberry32 seeded with [seed] (`a|=0` semantics); each call yields a double in [0, 1). */
fun mulberry32(seed: Int): () -> Double {
  var a = seed
  return {
    a += 0x6D2B79F5
    var t = (a xor (a ushr 15)) * (1 or a)
    t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
    ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
  }
}

/** The rng the prototype uses for a given avatar: `rng(hash((inc ? "~" : "") + name))`. */
fun maskRng(seed: String, incognito: Boolean): () -> Double = mulberry32(fnv1a((if (incognito) "~" else "") + seed).toInt())

sealed class MaskLayer {
  /** Gradient dashed arc ("t === 0"). [dashFraction] is the visible share of the circumference. */
  data class Arc(val cx: Double, val cy: Double, val r: Double, val strokeWidth: Double, val dashFraction: Double, val rotation: Int) : MaskLayer()
  /** Translucent gradient triangle ("t === 1"). Points are absolute, in 48-unit space. */
  data class Triangle(val points: List<Pair<Double, Double>>, val opacity: Double) : MaskLayer()
  /** Two concentric thin rings ("t === 2"). */
  data class Orbit(val cx: Double, val cy: Double, val r: Double) : MaskLayer()
}

/** Everything needed to draw a mask in a 48x48 space. Normal and incognito variants use disjoint field groups. */
data class MaskSpec(
  val incognito: Boolean,
  // normal
  val gradRotA: Int = 0,
  val gradRotB: Int = 0,
  val blobX: Double = 0.0,
  val blobY: Double = 0.0,
  val blobR: Double = 0.0,
  val layers: List<MaskLayer> = emptyList(),
  // incognito
  val hexCx: Double = 0.0,
  val hexCy: Double = 0.0,
  val hexRot: Int = 0,
  val patternAngle: Int = 0,
  val barX: Double = 0.0,
  val barW: Double = 0.0,
  val barRot: Int = 0,
  val dots: List<Pair<Double, Double>> = emptyList(),
) {
  /** Stable text form used by tests (3 decimals); the prototype's JS produces the same string. */
  fun describe(): String = buildString {
    fun f(v: Double) = String.format(java.util.Locale.ROOT, "%.3f", v)
    if (!incognito) {
      append("grad=$gradRotA,$gradRotB;blob=${f(blobX)},${f(blobY)},${f(blobR)};L=${layers.size}")
      for (l in layers) when (l) {
        is MaskLayer.Arc -> append(";arc=${f(l.cx)},${f(l.cy)},${f(l.r)},${f(l.strokeWidth)},${f(l.dashFraction)},${l.rotation}")
        is MaskLayer.Triangle -> append(";tri=" + l.points.joinToString(",") { "${f(it.first)},${f(it.second)}" } + ",${f(l.opacity)}")
        is MaskLayer.Orbit -> append(";orb=${f(l.cx)},${f(l.cy)},${f(l.r)}")
      }
    } else {
      append("hex=${f(hexCx)},${f(hexCy)},$hexRot;pat=$patternAngle;bar=${f(barX)},${f(barW)},$barRot;dots=" + dots.joinToString(";") { "${f(it.first)},${f(it.second)}" })
    }
  }
}

/** Radii of the four incognito hexagons: `5 + k * 5.5`. */
fun incognitoHexRadius(k: Int): Double = 5 + k * 5.5

/** Hexagon vertex [j] (0..5) of radius [r] around ([cx], [cy]) rotated by [rotDeg]. */
fun hexVertex(cx: Double, cy: Double, r: Double, rotDeg: Int, j: Int): Pair<Double, Double> {
  val a = (rotDeg + j * 60) * PI / 180
  return (cx + cos(a) * r) to (cy + sin(a) * r)
}

/** Generates the mask for [name]; same input always gives the same output. */
fun generateMaskSpec(name: String, incognito: Boolean): MaskSpec {
  val r = maskRng(name, incognito)
  fun n(a: Double, b: Double) = a + r() * (b - a)
  fun fl(v: Double) = floor(v).toInt()
  if (!incognito) {
    val gA = fl(r() * 360)
    val gB = fl(r() * 360)
    val bx = n(10.0, 38.0)
    val by = n(10.0, 38.0)
    val br = n(14.0, 22.0)
    val count = 3 + fl(r() * 3)
    val layers = ArrayList<MaskLayer>(count)
    for (i in 0 until count) {
      val t = fl(r() * 3)
      val cx = n(8.0, 40.0)
      val cy = n(8.0, 40.0)
      val rr = n(7.0, 20.0)
      val rot = fl(r() * 360)
      when (t) {
        0 -> {
          val sw = n(3.0, 7.0)
          val frac = n(0.3, 0.65)
          layers.add(MaskLayer.Arc(cx, cy, rr, sw, frac, rot))
        }
        1 -> {
          val pts = (0..2).map { k ->
            val a = (rot + k * (110 + r() * 30)) * PI / 180
            (cx + cos(a) * rr * 1.3) to (cy + sin(a) * rr * 1.3)
          }
          layers.add(MaskLayer.Triangle(pts, n(0.35, 0.7)))
        }
        else -> layers.add(MaskLayer.Orbit(cx, cy, rr))
      }
    }
    return MaskSpec(false, gradRotA = gA, gradRotB = gB, blobX = bx, blobY = by, blobR = br, layers = layers)
  }
  val cx = n(18.0, 30.0)
  val cy = n(18.0, 30.0)
  val rot = fl(r() * 60)
  val pat = fl(n(20.0, 70.0))
  val barX = n(6.0, 40.0)
  val barW = n(5.0, 9.0)
  val barRot = fl(n(15.0, 40.0))
  val dots = (0 until 5).map { n(4.0, 44.0) to n(4.0, 44.0) }
  return MaskSpec(true, hexCx = cx, hexCy = cy, hexRot = rot, patternAngle = pat, barX = barX, barW = barW, barRot = barRot, dots = dots)
}
