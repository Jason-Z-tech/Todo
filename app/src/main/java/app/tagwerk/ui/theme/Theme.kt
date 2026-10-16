package app.tagwerk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.tagwerk.data.Settings
import app.tagwerk.data.Task
import app.tagwerk.data.TaskList

@Immutable
data class Palette(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    /** Pfirsich – die Hauptfarbe für Knöpfe und Auswahl. */
    val accent: Color,
    val accentSoft: Color,
    val accentStrong: Color,
    val onAccent: Color,
    val danger: Color,
    val dangerSoft: Color,
    val star: Color,
)

val LightPalette = Palette(
    isDark = false,
    background = Color(0xFFFBF6EF),
    card = Color(0xFFFFFDFA),
    ink = Color(0xFF3B3236),
    muted = Color(0xFF8E8287),
    line = Color(0xFFEDE3DA),
    chip = Color(0xFFF5EDE5),
    accent = Color(0xFFF4B6A0),
    accentSoft = Color(0xFFFBDCCF),
    accentStrong = Color(0xFFC8705A),
    onAccent = Color(0xFF3B3236),
    danger = Color(0xFFB4505F),
    dangerSoft = Color(0xFFF6D3D9),
    star = Color(0xFFE2B33C),
)

val DarkPalette = Palette(
    isDark = true,
    background = Color(0xFF1F1A1D),
    card = Color(0xFF2A2428),
    ink = Color(0xFFF3ECE6),
    muted = Color(0xFFA99DA3),
    line = Color(0xFF3B3338),
    chip = Color(0xFF332C31),
    accent = Color(0xFFF4B6A0),
    accentSoft = Color(0xFF4A3530),
    accentStrong = Color(0xFFF4B6A0),
    onAccent = Color(0xFF2B2124),
    danger = Color(0xFFF0A0AE),
    dangerSoft = Color(0xFF4A2A31),
    star = Color(0xFFF2D27A),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Einstellungen (Listen, Name, Farbe) für alle Bildschirme. */
val LocalSettings = staticCompositionLocalOf { Settings() }

object Tagwerk {
    val palette: Palette
        @Composable @ReadOnlyComposable get() = LocalPalette.current

    val settings: Settings
        @Composable @ReadOnlyComposable get() = LocalSettings.current
}

/**
 * Baut die Farbpalette rund um eine Hauptfarbe. Pfirsich behält die
 * handverlesenen Originaltöne; jede andere Farbe färbt Hintergrund,
 * Karten und Linien ganz leicht mit ein.
 */
fun paletteFor(accent: Color, dark: Boolean): Palette {
    if (accent == Color(Settings.DEFAULT_ACCENT)) return if (dark) DarkPalette else LightPalette
    val hsl = accent.toHsl()
    return if (!dark) {
        val base = LightPalette
        val strong = Hsl(hsl.hue, minOf(hsl.saturation, 0.55f), minOf(hsl.lightness, 0.46f)).toColor()
        base.copy(
            background = Color(0xFFFCFAF7).mix(accent, 0.07f),
            card = Color(0xFFFFFEFC).mix(accent, 0.025f),
            line = Color(0xFFEDE7E1).mix(accent, 0.14f),
            chip = Color(0xFFF6F2EE).mix(accent, 0.11f),
            accent = accent,
            accentSoft = accent.mix(Color.White, 0.55f),
            accentStrong = strong,
            onAccent = accent.readableInk(base.ink),
        )
    } else {
        val base = DarkPalette
        val strong = Hsl(hsl.hue, minOf(hsl.saturation, 0.8f), maxOf(hsl.lightness, 0.74f)).toColor()
        val background = Color(0xFF1E1B1D).mix(accent, 0.04f)
        base.copy(
            background = background,
            card = Color(0xFF292527).mix(accent, 0.05f),
            line = Color(0xFF3A3437).mix(accent, 0.08f),
            chip = Color(0xFF322D30).mix(accent, 0.07f),
            accent = accent,
            accentSoft = background.mix(accent, 0.3f),
            accentStrong = strong,
            onAccent = accent.readableInk(Color(0xFF2B2124)),
        )
    }
}

/** Handverlesene Schrifttöne der ursprünglichen Listen. */
private val presetDeep = mapOf(
    0xFFFBD3C2.toInt() to Color(0xFF9A5540),
    0xFFCFE0F0.toInt() to Color(0xFF3F5D7C),
    0xFFD4E6CC.toInt() to Color(0xFF4A6840),
    0xFFF6D0D8.toInt() to Color(0xFF8F4257),
    0xFFE0D6F0.toInt() to Color(0xFF5C4C7E),
    0xFFF8E8B0.toInt() to Color(0xFF7A6322),
)

/** Reiner Pastellton einer Liste, unabhängig vom Modus. */
fun TaskList.pastel(): Color = Color(color).asPastel()

private fun TaskList.deep(): Color = presetDeep[color] ?: pastel().deepTone()

/** Hintergrundfarbe einer Liste (zarter Pastellton). */
@Composable
@ReadOnlyComposable
fun TaskList.soft(): Color =
    if (Tagwerk.palette.isDark) pastel().copy(alpha = 0.22f) else pastel()

/** Schrift-/Symbolfarbe einer Liste (kräftiger Ton desselben Pastells). */
@Composable
@ReadOnlyComposable
fun TaskList.strong(): Color =
    if (Tagwerk.palette.isDark) pastel() else deep()

/** Die Liste, in der diese Aufgabe steht. */
@Composable
@ReadOnlyComposable
fun Task.list(): TaskList = Tagwerk.settings.list(listId)

@Composable
fun TagwerkTheme(
    dark: Boolean = isSystemInDarkTheme(),
    settings: Settings = Settings(),
    content: @Composable () -> Unit,
) {
    val accent = Color(settings.accent)
    val p = remember(accent, dark) { paletteFor(accent, dark) }
    val scheme = if (dark) {
        darkColorScheme(
            // accentStrong statt accent: bleibt auch bei dunklen eigenen Farben sichtbar (z. B. „OK“ in Dialogen)
            primary = p.accentStrong,
            onPrimary = p.accentStrong.readableInk(Color(0xFF2B2124)),
            primaryContainer = p.accentSoft,
            onPrimaryContainer = p.ink,
            secondaryContainer = p.accentSoft,
            onSecondaryContainer = p.ink,
            background = p.background,
            onBackground = p.ink,
            surface = p.card,
            onSurface = p.ink,
            onSurfaceVariant = p.muted,
            surfaceVariant = p.chip,
            surfaceContainer = p.card,
            surfaceContainerHigh = p.card,
            surfaceContainerHighest = p.chip,
            surfaceContainerLow = p.background,
            outline = p.line,
            outlineVariant = p.line,
            error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.accentStrong,
            onPrimary = Color.White,
            primaryContainer = p.accentSoft,
            onPrimaryContainer = p.ink,
            secondaryContainer = p.accentSoft,
            onSecondaryContainer = p.ink,
            background = p.background,
            onBackground = p.ink,
            surface = p.card,
            onSurface = p.ink,
            onSurfaceVariant = p.muted,
            surfaceVariant = p.chip,
            surfaceContainer = p.card,
            surfaceContainerHigh = p.card,
            surfaceContainerHighest = p.chip,
            surfaceContainerLow = p.background,
            outline = p.line,
            outlineVariant = p.line,
            error = p.danger,
        )
    }
    CompositionLocalProvider(LocalPalette provides p, LocalSettings provides settings) {
        MaterialTheme(colorScheme = scheme, typography = TagwerkTypography, content = content)
    }
}
