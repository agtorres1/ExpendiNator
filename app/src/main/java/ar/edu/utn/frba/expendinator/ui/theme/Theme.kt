package ar.edu.utn.frba.expendinator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class ExpendiColors(
    val dark: Boolean,
    val textSoft: Color,
    val accent: Color,
    val primaryButton: Color,
    val onPrimaryButton: Color,
    val warning: Color,
    val warningContainer: Color,
    val warningBorder: Color,
    val onWarningContainer: Color
) {
    val categoryBackgroundAlpha: Float get() = if (dark) .14f else .10f
}

internal val DarkTokens = ExpendiColors(
    true, Color(0xFFC3CAD5), Mint, Mint, OnMint, Color(0xFFFFB86B),
    Color(0xFFFFB86B).copy(alpha = .10f), Color(0xFFFFB86B).copy(alpha = .35f),
    Color(0xFFFFD3A3)
)

internal val LightTokens = ExpendiColors(
    false, Color(0xFF3D4655), Color(0xFF0E8A5F), Mint, OnMint, Color(0xFFB4530F),
    Color(0xFFFFF4E8), Color(0xFFF5C99A), Color(0xFF8A4A0B)
)

val LocalExpendiColors = staticCompositionLocalOf { DarkTokens }

object ExpendiTheme {
    val colors: ExpendiColors @Composable get() = LocalExpendiColors.current
    val typography: ExpendiTypography get() = ExpendiTypography
}

internal val DarkScheme = darkColorScheme().withPalette(
    DarkTokens, DarkBackground, DarkSurface, DarkText, DarkOutline, Color(0xFF9AA3B2)
)
internal val LightScheme = lightColorScheme().withPalette(
    LightTokens, LightBackground, LightSurface, LightText, LightOutline, Color(0xFF5B6576)
)

private fun ColorScheme.withPalette(
    tokens: ExpendiColors, background: Color, surface: Color, text: Color,
    outline: Color, muted: Color
) = copy(
    primary = tokens.accent, onPrimary = tokens.onPrimaryButton,
    primaryContainer = tokens.primaryButton, onPrimaryContainer = tokens.onPrimaryButton,
    secondary = tokens.accent, onSecondary = tokens.onPrimaryButton,
    secondaryContainer = outline, onSecondaryContainer = text,
    tertiary = tokens.accent, onTertiary = tokens.onPrimaryButton,
    tertiaryContainer = outline, onTertiaryContainer = text,
    background = background, onBackground = text, surface = surface, onSurface = text,
    surfaceVariant = outline, onSurfaceVariant = muted, outline = outline, outlineVariant = outline,
    surfaceTint = surface, surfaceBright = surface, surfaceDim = surface,
    surfaceContainer = surface, surfaceContainerLowest = surface, surfaceContainerLow = surface,
    surfaceContainerHigh = surface, surfaceContainerHighest = surface,
    error = tokens.warning, onError = tokens.onPrimaryButton,
    errorContainer = tokens.warningContainer, onErrorContainer = tokens.onWarningContainer
)

@Composable
internal fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun ExpendinatorTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = themeMode.isDark()
    CompositionLocalProvider(LocalExpendiColors provides if (dark) DarkTokens else LightTokens) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = Typography,
            shapes = Shapes(
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(20.dp),
                extraLarge = RoundedCornerShape(24.dp)
            ),
            content = content
        )
    }
}
