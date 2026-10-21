package com.d_ave.bowels.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.R
import com.d_ave.bowels.data.AccentKey

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

private data class Accent(val primary: Long, val secondary: Long, val tertiary: Long, val primaryFixedDim: Long, val secondaryFixed: Long)

private val accents = mapOf(
    AccentKey.Olive to Accent(0xFF56642B, 0xFF9F402D, 0xFF8C4B55, 0xFFBDCE89, 0xFFFFDAD3),
    AccentKey.Coral to Accent(0xFF8A5A2B, 0xFFBF5B3F, 0xFF9F402D, 0xFFE2C089, 0xFFFFD8D0),
    AccentKey.Rose to Accent(0xFF7A4D5B, 0xFFB65A6D, 0xFF8C4B55, 0xFFDBB2BC, 0xFFFFD9DD),
    AccentKey.Slate to Accent(0xFF41525E, 0xFF7D5C4C, 0xFF58657A, 0xFFA9BCC8, 0xFFE7DAD4),
)

fun buildPalette(dark: Boolean, accent: AccentKey): Palette {
    val a = accents.getValue(accent)
    return Palette(
        dark = dark,
        primary = Color(a.primary),
        secondary = Color(a.secondary),
        tertiary = Color(a.tertiary),
        primaryFixedDim = Color(a.primaryFixedDim),
        secondaryFixed = Color(a.secondaryFixed),
        primaryContainer = Color(0xFF8A9A5B),
        onPrimary = Color.White,
        background = Color(if (dark) 0xFF1B1D0E else 0xFFFBFBE2),
        onBackground = Color(if (dark) 0xFFF2F2D9 else 0xFF1B1D0E),
        onSurface = Color(if (dark) 0xFFF2F2D9 else 0xFF1B1D0E),
        onSurfaceVariant = Color(if (dark) 0xFFC7CCB1 else 0xFF46483C),
        outline = Color(if (dark) 0xFF969B84 else 0xFF76786B),
        surfaceContainerLowest = Color(if (dark) 0xFF2A2D18 else 0xFFFFFFFF),
        surfaceContainerLow = Color(if (dark) 0xFF252815 else 0xFFF5F5DC),
        surfaceContainer = Color(if (dark) 0xFF2D311C else 0xFFEFEFD7),
        surfaceContainerHigh = Color(if (dark) 0xFF343923 else 0xFFEAEAD1),
        surfaceContainerHighest = Color(if (dark) 0xFF3D422B else 0xFFE4E4CC),
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
        darkColorScheme(primary = palette.primary, secondary = palette.secondary, tertiary = palette.tertiary, background = palette.background, surface = palette.surfaceContainerLowest, onSurface = palette.onSurface, onBackground = palette.onBackground)
    } else {
        lightColorScheme(primary = palette.primary, secondary = palette.secondary, tertiary = palette.tertiary, background = palette.background, surface = palette.surfaceContainerLowest, onSurface = palette.onSurface, onBackground = palette.onBackground)
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
