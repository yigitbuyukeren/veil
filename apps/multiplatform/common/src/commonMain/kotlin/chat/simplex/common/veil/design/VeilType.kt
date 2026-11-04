package chat.simplex.common.veil.design

import androidx.compose.material.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.Inter

/**
 * Veil type scale: Bricolage Grotesque for display and titles, Inter for everything else.
 * Colours are not baked in; text picks up the content colour (or pass `color = VeilTheme.colors.ink2`).
 */
@Immutable
class VeilType(
  val displayFont: FontFamily,
  val bodyFont: FontFamily,
) {
  /** Hero titles (onboarding story): 38/1.02 bold. */
  val display = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 39.sp, letterSpacing = (-0.01).em)
  /** Top bar title: 22 bold. */
  val title = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em)
  /** Sheet title: 21 bold. */
  val sheetTitle = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 25.sp, letterSpacing = (-0.01).em)
  /** List row name / FAB label: 16 semibold. */
  val rowTitle = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.01).em)
  /** Small display label (sender name in a group bubble): 12.5 semibold. */
  val label = TextStyle(fontFamily = displayFont, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, letterSpacing = (-0.01).em)
  /** Body: 15/1.45. */
  val body = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.75.sp)
  /** Large body (inputs, sheet actions): 16. */
  val bodyLarge = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
  /** Button label: 15.5 semibold. */
  val button = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, lineHeight = 20.sp)
  /** Captions, subtitles, timestamps: 12. */
  val caption = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
  /** Chip text: 11.5 medium. */
  val chip = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 14.sp)
  /** Message meta (time, ticks): 11. */
  val meta = TextStyle(fontFamily = bodyFont, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp)

  companion object {
    val Default by lazy { VeilType(BricolageGrotesque, Inter) }
  }
}

/**
 * Material typography used by the whole app (including not-yet-redesigned upstream screens):
 * upstream sizes, but headings use the Veil display face.
 */
val VeilMaterialTypography: Typography by lazy {
  val d = BricolageGrotesque
  Typography(
    h1 = TextStyle(fontFamily = d, fontWeight = FontWeight.Bold, fontSize = 33.5.sp),
    h2 = TextStyle(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    h3 = TextStyle(fontFamily = d, fontWeight = FontWeight.SemiBold, fontSize = 18.5.sp),
    h4 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 17.5.sp),
    body1 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    body2 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 18.sp),
  )
}
