package app.tagwerk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.tagwerk.data.Category

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

object Tagwerk {
    val palette: Palette
        @Composable @ReadOnlyComposable get() = LocalPalette.current
}

private data class CategoryTone(val pastel: Color, val deep: Color)

private fun Category.tone(): CategoryTone = when (this) {
    Category.PERSONAL -> CategoryTone(Color(0xFFFBD3C2), Color(0xFF9A5540))
    Category.WORK -> CategoryTone(Color(0xFFCFE0F0), Color(0xFF3F5D7C))
    Category.SHOPPING -> CategoryTone(Color(0xFFD4E6CC), Color(0xFF4A6840))
    Category.HEALTH -> CategoryTone(Color(0xFFF6D0D8), Color(0xFF8F4257))
    Category.HOME -> CategoryTone(Color(0xFFE0D6F0), Color(0xFF5C4C7E))
    Category.IDEAS -> CategoryTone(Color(0xFFF8E8B0), Color(0xFF7A6322))
}

/** Hintergrundfarbe einer Liste (zarter Pastellton). */
@Composable
@ReadOnlyComposable
fun Category.soft(): Color =
    if (Tagwerk.palette.isDark) tone().pastel.copy(alpha = 0.22f) else tone().pastel

/** Schrift-/Symbolfarbe einer Liste (kräftiger Ton desselben Pastells). */
@Composable
@ReadOnlyComposable
fun Category.strong(): Color =
    if (Tagwerk.palette.isDark) tone().pastel else tone().deep

/** Reiner Pastellton, unabhängig vom Modus. */
fun Category.pastel(): Color = tone().pastel

@Composable
fun TagwerkTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = p.accent,
            onPrimary = p.onAccent,
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
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = TagwerkTypography, content = content)
    }
}
