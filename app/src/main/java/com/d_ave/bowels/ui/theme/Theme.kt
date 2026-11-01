package com.d_ave.bowels.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.R
import com.d_ave.bowels.data.AccentKey
import com.d_ave.bowels.data.BackgroundKey

@Immutable
data class Palette(
    val dark: Boolean,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val primaryFixedDim: Color,
    val secondaryFixed: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val error: Color = Color(0xFFBA1A1A),
)

private data class Accent(val primary: Long, val secondary: Long, val tertiary: Long, val primaryFixedDim: Long, val secondaryFixed: Long, val container: Long? = null)

private val accents = mapOf(
    AccentKey.Olive to Accent(0xFF56642B, 0xFF9F402D, 0xFF8C4B55, 0xFFBDCE89, 0xFFFFDAD3, 0xFF8A9A5B),
    AccentKey.Coral to Accent(0xFF8A5A2B, 0xFFBF5B3F, 0xFF9F402D, 0xFFE2C089, 0xFFFFD8D0, 0xFF8A9A5B),
    AccentKey.Rose to Accent(0xFF7A4D5B, 0xFFB65A6D, 0xFF8C4B55, 0xFFDBB2BC, 0xFFFFD9DD, 0xFF8A9A5B),
    AccentKey.Slate to Accent(0xFF41525E, 0xFF7D5C4C, 0xFF58657A, 0xFFA9BCC8, 0xFFE7DAD4, 0xFF8A9A5B),
    AccentKey.Ocean to Accent(0xFF1F5F7A, 0xFFC0603B, 0xFF3C6E8F, 0xFF9CCBE0, 0xFFFFDBCF),
    AccentKey.Forest to Accent(0xFF2E6B47, 0xFF8A5A2B, 0xFF4F6F52, 0xFFA6D4B4, 0xFFFFDDB8),
    AccentKey.Lavender to Accent(0xFF6750A4, 0xFFB5577A, 0xFF7D5260, 0xFFCFBDFE, 0xFFFFD8E4),
    AccentKey.Sunset to Accent(0xFFB4532A, 0xFF8E4585, 0xFFC0762B, 0xFFFFB693, 0xFFFAD8F4),
    AccentKey.Berry to Accent(0xFF8E2A55, 0xFF5C5BA8, 0xFFA34766, 0xFFFFB0CB, 0xFFE1E0FF),
    AccentKey.Mono to Accent(0xFF3A3A3A, 0xFF6B6B6B, 0xFF505050, 0xFFC6C6C6, 0xFFE2E2E2),
)

/** A swatch for the settings screen (the wallpaper accent has none of its own). */
fun accentSwatch(accent: AccentKey): Color = accents[accent]?.let { Color(it.primary) } ?: Color(0xFF6750A4)

private data class Surfaces(
    val background: Color, val lowest: Color, val low: Color, val container: Color, val high: Color, val highest: Color,
    val onSurface: Color, val onVariant: Color, val outline: Color,
)

/** The original cream look, kept exactly. */
private fun cream(dark: Boolean) = if (dark) Surfaces(
    Color(0xFF1B1D0E), Color(0xFF2A2D18), Color(0xFF252815), Color(0xFF2D311C), Color(0xFF343923), Color(0xFF3D422B),
    Color(0xFFF2F2D9), Color(0xFFC7CCB1), Color(0xFF969B84),
) else Surfaces(
    Color(0xFFFBFBE2), Color(0xFFFFFFFF), Color(0xFFF5F5DC), Color(0xFFEFEFD7), Color(0xFFEAEAD1), Color(0xFFE4E4CC),
    Color(0xFF1B1D0E), Color(0xFF46483C), Color(0xFF76786B),
)

/** Surfaces tinted by one hue: barely there in light mode, deep and calm in dark mode. */
private fun tinted(hue: Float, saturation: Float, dark: Boolean): Surfaces {
    fun c(l: Float, s: Float = saturation) = Color.hsl(hue, s, l)
    return if (dark) Surfaces(
        c(0.075f), c(0.13f), c(0.11f), c(0.15f), c(0.185f), c(0.225f),
        c(0.93f, saturation * 0.4f), c(0.78f, saturation * 0.35f), c(0.58f, saturation * 0.3f),
    ) else Surfaces(
        c(0.965f), c(0.995f), c(0.945f), c(0.925f), c(0.905f), c(0.88f),
        c(0.10f, saturation * 0.5f), c(0.30f, saturation * 0.4f), c(0.47f, saturation * 0.3f),
    )
}

private fun surfaces(background: BackgroundKey, dark: Boolean): Surfaces = when (background) {
    BackgroundKey.Cream -> cream(dark)
    BackgroundKey.Paper -> tinted(0f, 0f, dark)
    BackgroundKey.Sand -> tinted(35f, 0.38f, dark)
    BackgroundKey.Mist -> tinted(210f, 0.30f, dark)
    BackgroundKey.Blush -> tinted(345f, 0.38f, dark)
    BackgroundKey.Sage -> tinted(110f, 0.18f, dark)
    BackgroundKey.Dusk -> tinted(255f, 0.28f, dark)
    BackgroundKey.Black -> if (dark) Surfaces(
        Color(0xFF000000), Color(0xFF111111), Color(0xFF0A0A0A), Color(0xFF161616), Color(0xFF1C1C1C), Color(0xFF242424),
        Color(0xFFF1F1F1), Color(0xFFBDBDBD), Color(0xFF8A8A8A),
    ) else tinted(0f, 0f, false)
}

/** A swatch for the settings screen. */
fun backgroundSwatch(background: BackgroundKey, dark: Boolean): Color = when {
    background == BackgroundKey.Black -> Color.Black
    // The light backgrounds are all near-white; their deepest surface tells them apart.
    dark -> surfaces(background, true).high
    else -> lerp(surfaces(background, false).highest, surfaces(background, true).highest, 0.12f)
}

/**
 * [dynamic]: the wallpaper's colors (Android 12+), used when the accent is [AccentKey.Wallpaper].
 */
fun buildPalette(dark: Boolean, accent: AccentKey, background: BackgroundKey = BackgroundKey.Cream, dynamic: ColorScheme? = null): Palette {
    val s = surfaces(background, dark)
    val a = accents[accent]
    val primary: Color
    val secondary: Color
    val tertiary: Color
    val fixedDim: Color
    val secondaryFixed: Color
    if (a == null && dynamic != null) {
        // Material You gives a light primary in dark mode; the app's text and buttons expect the deep one.
        primary = if (dark) dynamic.primaryContainer else dynamic.primary
        secondary = if (dark) dynamic.secondaryContainer else dynamic.secondary
        tertiary = if (dark) dynamic.tertiaryContainer else dynamic.tertiary
        fixedDim = if (dark) dynamic.primary else dynamic.primaryContainer
        secondaryFixed = if (dark) dynamic.secondary else dynamic.secondaryContainer
    } else {
        val x = a ?: accents.getValue(AccentKey.Lavender)
        // On dark backgrounds the deep accent is hard to read as text; lift it toward its light tint.
        primary = if (dark) lerp(Color(x.primary), Color(x.primaryFixedDim), 0.45f) else Color(x.primary)
        secondary = Color(x.secondary)
        tertiary = Color(x.tertiary)
        fixedDim = Color(x.primaryFixedDim)
        secondaryFixed = Color(x.secondaryFixed)
    }
    return Palette(
        dark = dark,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        primaryFixedDim = fixedDim,
        secondaryFixed = secondaryFixed,
        primaryContainer = a?.container?.let { Color(it) } ?: lerp(primary, Color.White, 0.3f),
        onPrimary = Color.White,
        background = s.background,
        onBackground = s.onSurface,
        onSurface = s.onSurface,
        onSurfaceVariant = s.onVariant,
        outline = s.outline,
        surfaceContainerLowest = s.lowest,
        surfaceContainerLow = s.low,
        surfaceContainer = s.container,
        surfaceContainerHigh = s.high,
        surfaceContainerHighest = s.highest,
    )
}

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val LocalPalette = staticCompositionLocalOf { buildPalette(false, AccentKey.Olive) }

@Composable
fun BowelsTheme(palette: Palette, content: @Composable () -> Unit) {
    val base = TextStyle(fontFamily = Manrope)
    val scheme = if (palette.dark) {
        darkColorScheme(primary = palette.primary, secondary = palette.secondary, tertiary = palette.tertiary, background = palette.background, surface = palette.surfaceContainerLowest, onSurface = palette.onSurface, onBackground = palette.onBackground, surfaceContainerHigh = palette.surfaceContainerHigh, surfaceContainerHighest = palette.surfaceContainerHighest)
    } else {
        lightColorScheme(primary = palette.primary, secondary = palette.secondary, tertiary = palette.tertiary, background = palette.background, surface = palette.surfaceContainerLowest, onSurface = palette.onSurface, onBackground = palette.onBackground, surfaceContainerHigh = palette.surfaceContainerHigh, surfaceContainerHighest = palette.surfaceContainerHighest)
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(
            bodyLarge = base.copy(fontSize = 16.sp),
            bodyMedium = base.copy(fontSize = 14.sp),
            labelLarge = base.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            titleMedium = base.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
        ),
    ) {
        CompositionLocalProvider(LocalPalette provides palette, content = content)
    }
}
