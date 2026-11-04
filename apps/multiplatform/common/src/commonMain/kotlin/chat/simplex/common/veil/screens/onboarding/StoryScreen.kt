package chat.simplex.common.veil.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.platform.BackHandler
import chat.simplex.common.veil.design.*
import chat.simplex.res.MR
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class StoryText(val title: StringResource, val text: StringResource)

private val story = listOf(
  StoryText(MR.strings.veil_onb_story1_title, MR.strings.veil_onb_story1_text),
  StoryText(MR.strings.veil_onb_story2_title, MR.strings.veil_onb_story2_text),
  StoryText(MR.strings.veil_onb_story3_title, MR.strings.veil_onb_story3_text),
)

/**
 * Three-page story ("Hesap yok", "Yalnızca Tor", "Her sohbete ayrı kimlik"). Moving between pages lowers the [Perde]
 * over the page, swaps the content under it and lifts it again (280 ms each way, instant with reduce-motion).
 */
@Composable
internal fun StoryScreen(onDone: () -> Unit) {
  val colors = VeilTheme.colors
  var page by rememberSaveable { mutableStateOf(0) }
  var revealed by remember { mutableStateOf(true) }
  val scope = rememberCoroutineScope()
  val liftMs = VeilTheme.motion.lift(VeilTheme.reduceMotion)

  fun goTo(target: Int) {
    if (!revealed) return
    if (liftMs == 0) {
      page = target
      return
    }
    scope.launch {
      revealed = false
      delay(liftMs.toLong())
      page = target
      revealed = true
    }
  }

  BackHandler(enabled = storyPageBefore(page) != null) { storyPageBefore(page)?.let { goTo(it) } }

  OnbScaffold(
    onBack = storyPageBefore(page)?.let { prev -> { goTo(prev) } },
    bottom = {
      val last = storyPageAfter(page) == null
      VeilButton(
        text = stringResource(if (last) MR.strings.veil_onb_begin else MR.strings.veil_onb_next),
        onClick = { storyPageAfter(page)?.let { goTo(it) } ?: onDone() },
      )
    },
  ) {
    Perde(
      revealed = revealed,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      shape = VeilTheme.shapes.card,
      blurRadius = 22.dp,
    ) {
      Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          StoryArt(page)
        }
        Column(Modifier.padding(horizontal = 8.dp).heightIn(min = 190.dp)) {
          BasicText(stringResource(story[page].title), style = VeilTheme.type.display.copy(color = colors.ink))
          Spacer(Modifier.height(12.dp))
          BasicText(
            stringResource(story[page].text),
            Modifier.widthIn(max = 340.dp),
            style = VeilTheme.type.bodyLarge.copy(fontSize = 16.5.sp, lineHeight = 24.sp, color = colors.ink2)
          )
        }
      }
    }
    Spacer(Modifier.height(14.dp))
    StoryDots(page)
  }
}

@Composable
private fun StoryDots(page: Int) {
  val colors = VeilTheme.colors
  val ms = VeilTheme.motion.lift(VeilTheme.reduceMotion)
  val description = stringResource(MR.strings.veil_onb_step_of).format(page + 1, STORY_PAGES)
  Row(
    Modifier.padding(horizontal = 8.dp).semantics { contentDescription = description },
    horizontalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    for (i in 0 until STORY_PAGES) {
      val w by animateDpAsState(if (i == page) 44.dp else 18.dp, tween(ms, easing = VeilTheme.motion.easeOut))
      Box(
        Modifier
          .width(w)
          .height(5.dp)
          .clip(VeilTheme.shapes.pill)
          .then(if (i == page) Modifier.background(colors.gradient) else Modifier.background(colors.sheet3))
      )
    }
  }
}

@Composable
private fun StoryArt(page: Int) {
  val reduce = VeilTheme.reduceMotion
  val transition = rememberInfiniteTransition(label = "storyFloat")
  val float by transition.animateFloat(
    0f, 1f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float"
  )
  val lift = if (reduce) 0f else -8f * float
  Box(Modifier.graphicsLayer { translationY = lift.dp.toPx() }.clearAndSetSemantics { }) {
    when (page) {
      0 -> NoAccountArt()
      1 -> TorRingsArt()
      else -> SeparateIdentitiesArt()
    }
  }
}

/** Page 1: three overlapping masks, "?", "Hesap", "Yok". */
@Composable
private fun NoAccountArt() {
  val sizes = listOf(98, 124, 98)
  val seeds = listOf("story-?", "story-Hesap", "story-Yok")
  val xs = listOf(0, 72, 170)
  Box(Modifier.size(width = 268.dp, height = 124.dp).graphicsLayer { scaleX = 1.1f; scaleY = 1.1f }) {
    for (k in 0..2) {
      MaskAvatar(
        seeds[k], sizes[k].dp,
        modifier = Modifier
          .align(Alignment.CenterStart)
          .offset(x = xs[k].dp, y = if (k == 1) (-14).dp else 0.dp)
          .rotate(((k - 1) * 10).toFloat())
      )
    }
  }
}

/** Page 2: concentric rings turning slowly at different speeds, a small dot in the middle. */
@Composable
private fun TorRingsArt() {
  val colors = VeilTheme.colors
  val reduce = VeilTheme.reduceMotion
  val transition = rememberInfiniteTransition(label = "storyRings")
  val angles = (0..3).map { k ->
    transition.animateFloat(
      0f, if (k % 2 == 1) -360f else 360f,
      infiniteRepeatable(tween((40 - k * 7) * 1000, easing = LinearEasing)), label = "ring$k"
    )
  }
  val radii = listOf(120f, 92f, 64f, 38f)
  Canvas(Modifier.size(270.dp)) {
    val u = size.minDimension / 270f
    val c = Offset(135f * u, 135f * u)
    val brush = colors.gradientFor(size, 135f)
    for (k in 0..3) {
      val stroke = Stroke(
        width = (3f - k * 0.4f) * u,
        cap = StrokeCap.Round,
        pathEffect = if (k % 2 == 1) PathEffect.dashPathEffect(floatArrayOf(4f * u, 7f * u)) else null,
      )
      withTransform({ rotate(if (reduce) 0f else angles[k].value, c) }) {
        drawCircle(brush, radius = radii[k] * u, center = c, alpha = 0.35f + k * 0.2f, style = stroke)
      }
    }
    drawCircle(brush, radius = 14f * u, center = c)
  }
}

/** Page 3: three masks, one of them an incognito (dashed hexagon) mask. */
@Composable
private fun SeparateIdentitiesArt() {
  Box(Modifier.size(width = 290.dp, height = 230.dp)) {
    Box(Modifier.offset(8.dp, 40.dp).rotate(-8f)) { MaskAvatar("Elif Kaya", 108.dp) }
    Box(Modifier.offset(150.dp, 10.dp).rotate(7f)) { MaskAvatar("Kadıköy Bisiklet", 100.dp) }
    Box(Modifier.offset(70.dp, 100.dp).rotate(-2f)) { MaskAvatar("Kuzgun", 112.dp, incognito = true) }
  }
}
