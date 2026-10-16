package app.tagwerk.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

/** Farbton (0–360), Sättigung und Helligkeit (je 0–1). */
data class Hsl(val hue: Float, val saturation: Float, val lightness: Float) {
    fun toColor(alpha: Float = 1f): Color = Color.hsl(
        hue.mod(360f),
        saturation.coerceIn(0f, 1f),
        lightness.coerceIn(0f, 1f),
        alpha,
    )
}

fun Color.toHsl(): Hsl {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val l = (max + min) / 2f
    val d = max - min
    if (d < 1e-6f) return Hsl(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when (max) {
        red -> 60f * (((green - blue) / d).mod(6f))
        green -> 60f * (((blue - red) / d) + 2f)
        else -> 60f * (((red - green) / d) + 4f)
    }
    return Hsl(h, s.coerceIn(0f, 1f), l)
}

fun Color.withLightness(lightness: Float): Color = toHsl().copy(lightness = lightness).toColor(alpha)

/** Mischt diese Farbe mit [other]; [amount] 0 = unverändert, 1 = [other]. */
fun Color.mix(other: Color, amount: Float): Color = lerp(this, other, amount.coerceIn(0f, 1f))

/** Gut lesbare Schriftfarbe auf dieser Farbe: dunkle Tinte oder Weiß. */
fun Color.readableInk(dark: Color, light: Color = Color.White): Color =
    if (luminance() > 0.36f) dark else light

/**
 * Macht aus einer beliebigen Farbe einen zarten Pastellton für Listen –
 * Farbton bleibt, Helligkeit wird angehoben, grelle Sättigung gedämpft.
 */
fun Color.asPastel(): Color {
    val hsl = toHsl()
    if (hsl.lightness >= 0.8f) return this
    return hsl.copy(saturation = minOf(hsl.saturation, 0.75f), lightness = 0.86f).toColor()
}

/** Kräftiger Ton desselben Pastells – für Schrift und Symbole auf hellem Grund. */
fun Color.deepTone(): Color {
    val hsl = toHsl()
    return Hsl(hsl.hue, (hsl.saturation * 0.55f).coerceIn(0.18f, 0.5f), 0.38f).toColor()
}
