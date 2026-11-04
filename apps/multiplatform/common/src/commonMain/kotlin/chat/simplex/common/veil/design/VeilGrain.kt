package chat.simplex.common.veil.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*

private const val GRAIN_SIZE = 96

/**
 * Tileable monochrome noise (deterministic, mulberry32 seeded), generated once on first use and cached for the process.
 * Replaces the prototype's SVG feTurbulence texture.
 */
val veilGrainBitmap: ImageBitmap by lazy {
  val bmp = ImageBitmap(GRAIN_SIZE, GRAIN_SIZE)
  val canvas = Canvas(bmp)
  val paint = Paint()
  val next = mulberry32(0x5EED)
  for (y in 0 until GRAIN_SIZE) {
    for (x in 0 until GRAIN_SIZE) {
      val v = next()
      paint.color = Color(v.toFloat(), v.toFloat(), v.toFloat(), 1f)
      canvas.drawRect(Rect(x.toFloat(), y.toFloat(), x + 1f, y + 1f), paint)
    }
  }
  bmp
}

internal val veilGrainBrush: Brush by lazy { ShaderBrush(ImageShader(veilGrainBitmap, TileMode.Repeated, TileMode.Repeated)) }

/**
 * Draws a fine film grain over the content (prototype: opacity 0.06 overlay on screens, 0.2 inside the veil), scaled
 * by the theme's `grainScale` (half as strong in Sis). Decorative: it draws only and adds no semantics.
 * Cheap: one tiled shader fill, no per-frame allocation.
 */
fun Modifier.veilGrain(alpha: Float = 0.06f, blendMode: BlendMode = BlendMode.Overlay): Modifier = composed {
  // grain is lighter on light themes (VeilColors.grainScale)
  val effective = alpha * VeilTheme.colors.grainScale
  Modifier.drawWithCache {
    onDrawWithContent {
      drawContent()
      drawRect(brush = veilGrainBrush, alpha = effective, blendMode = blendMode)
    }
  }
}
