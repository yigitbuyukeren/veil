package chat.simplex.common.veil.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Debug-only component gallery. Labels are hardcoded on purpose (not user-facing, not localised).
 * Reachable from Settings > Help & support when [veilIsDebugBuild] is true.
 */

@Composable
fun VeilGalleryScreen() {
  val colors = VeilTheme.colors
  val type = VeilTheme.type
  Column(
    Modifier.fillMaxSize().background(colors.night).verticalScroll(rememberScrollState()).statusBarsPadding().padding(vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp),
  ) {
    VeilTopBar(
      title = "Veil design gallery",
      subtitle = {
        TorRing(TorRingState.ProxyAnswers, 14.dp)
        BasicText("debug build only", style = type.caption.copy(color = colors.ink3))
      }
    )
    Column(Modifier.padding(horizontal = VeilGutter), verticalArrangement = Arrangement.spacedBy(22.dp)) {
      GalleryColors()
      GalleryType()
      GalleryButtons()
      GalleryChips()
      GalleryInputs()
      GallerySurfaces()
      GalleryPerde()
      GalleryAvatars()
      GalleryRings()
      GalleryBubbles()
      GalleryMotion()
    }
    Spacer(Modifier.height(40.dp))
  }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    BasicText(title.uppercase(), style = VeilTheme.type.caption.copy(color = VeilTheme.colors.ink3, letterSpacing = 1.2.sp))
    content()
  }
}

@Composable
private fun Swatch(name: String, color: Color) {
  Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(Modifier.size(48.dp).clip(VeilTheme.shapes.button).background(color).border(1.dp, VeilTheme.colors.line2, VeilTheme.shapes.button))
    BasicText(name, style = VeilTheme.type.meta.copy(color = VeilTheme.colors.ink2))
  }
}

@Composable
private fun GalleryColors() = Section("Colors") {
  val c = VeilTheme.colors
  Row(Modifier.horizontalScroll2(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Swatch("night", c.night); Swatch("night2", c.night2); Swatch("night3", c.night3)
    Swatch("ink", c.ink); Swatch("ink2", c.ink2); Swatch("ink3", c.ink3)
  }
  Row(Modifier.horizontalScroll2(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Swatch("sheet1", c.sheet1); Swatch("sheet2", c.sheet2); Swatch("sheet3", c.sheet3)
    Swatch("line", c.line); Swatch("line2", c.line2)
  }
  Row(Modifier.horizontalScroll2(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Swatch("mint", c.mint); Swatch("iris", c.iris); Swatch("rose", c.rose); Swatch("warn", c.warn); Swatch("err", c.err)
  }
  Box(Modifier.fillMaxWidth().height(28.dp).clip(VeilTheme.shapes.pill).background(c.gradient))
}

@Composable
private fun Modifier.horizontalScroll2(): Modifier = this.horizontalScroll(rememberScrollState())

@Composable
private fun GalleryType() = Section("Type") {
  val t = VeilTheme.type
  val ink = VeilTheme.colors.ink
  fun TextStyle.c() = copy(color = ink)
  BasicText("Düşünceli gönderi ğĞ ıİ şŞ çÇ öÖ üÜ", style = t.display.c())
  BasicText("title — Başlık 22", style = t.title.c())
  BasicText("sheetTitle — Sohbeti sil 21", style = t.sheetTitle.c())
  BasicText("rowTitle — Elif Kaya 16", style = t.rowTitle.c())
  BasicText("label — Mert Şahin 12.5", style = t.label.c())
  BasicText("body — Yarın akşam görüşelim mi? Inter 15", style = t.body.c())
  BasicText("bodyLarge — Mesaj yaz 16", style = t.bodyLarge.c())
  BasicText("caption — 12 / meta — 11", style = t.caption.copy(color = VeilTheme.colors.ink2))
}

@Composable
private fun GalleryButtons() = Section("Buttons") {
  VeilButton("Birincil", onClick = {})
  VeilButton("İkincil", onClick = {}, style = VeilButtonStyle.Secondary)
  VeilButton("Tehlikeli", onClick = {}, style = VeilButtonStyle.Danger)
  VeilButton("Devre dışı", onClick = {}, enabled = false)
  VeilButton("Devre dışı ikincil", onClick = {}, style = VeilButtonStyle.Secondary, enabled = false)
}

@Composable
private fun GalleryChips() = Section("Chips") {
  Row(Modifier.horizontalScroll2(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    VeilChip("Varsayılan")
    VeilChip("Gradyan", style = VeilChipStyle.Gradient)
    VeilChip("Kesikli", style = VeilChipStyle.Dashed)
    VeilChip("Uyarı", style = VeilChipStyle.Warning)
  }
}

@Composable
private fun GalleryInputs() = Section("Switch and text field") {
  var a by remember { mutableStateOf(true) }
  var b by remember { mutableStateOf(false) }
  Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
    VeilSwitch(a, { a = it }); VeilSwitch(b, { b = it }); VeilSwitch(true, {}, enabled = false)
  }
  var text by remember { mutableStateOf("") }
  VeilTextField(text, { text = it }, placeholder = "Mesaj yaz (odaklanınca gradyan çerçeve)")
}

@Composable
private fun GallerySurfaces() = Section("Surface, card, sheet") {
  VeilCard { BasicText("VeilCard, Low layer", style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink)) }
  VeilCard(layer = VeilLayer.Mid) { BasicText("VeilCard, Mid layer", style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink)) }
  VeilSurface(Modifier.fillMaxWidth().height(48.dp), layer = VeilLayer.High) {
    BasicText("VeilSurface High", Modifier.align(Alignment.Center), style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink))
  }
  VeilSheet {
    BasicText("Sheet title", style = VeilTheme.type.sheetTitle.copy(color = VeilTheme.colors.ink))
    Spacer(Modifier.height(10.dp))
    BasicText("28dp top radius, grabber, top-lit wash.", style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink2))
  }
}

@Composable
private fun GalleryPerde() = Section("Perde (veil / reveal)") {
  var revealed by remember { mutableStateOf(false) }
  var held by remember { mutableStateOf(false) }
  Perde(revealed = revealed || held, Modifier.fillMaxWidth().perdeHold(onTap = { revealed = !revealed }) { held = it }) {
    BasicText(
      "Gizli ileti önizlemesi: buluşma saat 21:00, kapıdan gir.",
      Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      style = VeilTheme.type.body.copy(color = VeilTheme.colors.ink2)
    )
  }
  BasicText("tap toggles, press and hold reveals. blur supported: $veilBlurSupported", style = VeilTheme.type.caption.copy(color = VeilTheme.colors.ink3))
  Perde(revealed = revealed, Modifier.fillMaxWidth().height(90.dp), shape = VeilTheme.shapes.card, blurRadius = 14.dp) {
    Box(Modifier.fillMaxSize().background(VeilTheme.colors.gradient))
  }
}

@Composable
private fun GalleryAvatars() = Section("MaskAvatar") {
  val seeds = listOf("Elif Kaya", "Mert Şahin", "Ayşe", "Can", "Zeynep Demir", "Ömer")
  Row(Modifier.horizontalScroll2(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    seeds.forEach { MaskAvatar(it, 56.dp) }
  }
  Row(Modifier.horizontalScroll2().padding(end = 6.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
    seeds.forEach { MaskAvatar(it, 56.dp, incognito = true) }
  }
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    MaskAvatar("Elif Kaya", 24.dp); MaskAvatar("Elif Kaya", 36.dp); MaskAvatar("Elif Kaya", 48.dp)
    MaskAvatar("Elif Kaya", 36.dp, incognito = true); MaskAvatar("Elif Kaya", 96.dp, incognito = true)
  }
}

@Composable
private fun GalleryRings() = Section("TorRing") {
  TorRingState.values().forEach { s ->
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
      TorRing(s, 24.dp); TorRing(s, 40.dp)
      BasicText(
        torRingLabel(s),
        style = VeilTheme.type.caption.copy(color = if (s == TorRingState.Unavailable) VeilTheme.colors.warn else VeilTheme.colors.ink2)
      )
    }
  }
}

@Composable
private fun GalleryBubbles() = Section("Bubbles") {
  val ink = VeilTheme.colors.ink
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    VeilBubble(outgoing = false, Modifier.widthIn(max = 280.dp)) { BasicText("Selam, nasılsın? Katlanmış köşe sol üstte.", style = VeilTheme.type.body.copy(color = ink)) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
      VeilBubble(outgoing = true, Modifier.widthIn(max = 280.dp)) { BasicText("İyiyim, sen? Burada sağ üstte.", style = VeilTheme.type.body.copy(color = ink)) }
    }
  }
}

@Composable
private fun GalleryMotion() = Section("Motion") {
  val c = VeilTheme.colors
  BasicText("reduce motion: ${VeilTheme.reduceMotion}; lift ${VeilTheme.motion.lift(VeilTheme.reduceMotion)} ms", style = VeilTheme.type.caption.copy(color = c.ink2))
  Box(Modifier.fillMaxWidth().height(40.dp).veilGrain(0.12f).background(c.night3))
}
