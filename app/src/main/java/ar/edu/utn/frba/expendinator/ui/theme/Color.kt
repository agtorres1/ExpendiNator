package ar.edu.utn.frba.expendinator.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb

internal val Mint = Color(0xFF5EE6A8)
internal val OnMint = Color(0xFF06210F)
internal val DarkBackground = Color(0xFF0E1116)
internal val DarkSurface = Color(0xFF171B22)
internal val DarkText = Color(0xFFF2F4F7)
internal val DarkOutline = Color(0xFF262C38)
internal val LightBackground = Color(0xFFF4F6F9)
internal val LightSurface = Color.White
internal val LightText = Color(0xFF10141B)
internal val LightOutline = Color(0xFFE3E7EE)

private val lightCategories = mapOf(
    0x9EC5FEL to Color(0xFF2F6FD6),
    0xFECBA1L to Color(0xFFC2621B),
    0xF3B0C3L to Color(0xFFC23B6B),
    0xFFD966L to Color(0xFF9A7400),
    0xE2C6FEL to Color(0xFF7A4FD0),
    0xA3CFBBL to Color(0xFF2E8B62)
)

fun ExpendiColors.category(stored: Long?): Color {
    if (stored == null) return accent
    val rgb = stored and 0xFFFFFFL
    val pastel = Color(rgb or 0xFF000000L)
    return if (dark) pastel else lightCategories[rgb]
        ?: Color(pastel.red * .55f, pastel.green * .55f, pastel.blue * .55f)
}

internal val CategoryPastels = listOf(
    0xFF9EC5FEL, 0xFFFECBA1L, 0xFFA3CFBBL, 0xFFF3B0C3L, 0xFFE2C6FEL, 0xFFFFD966L
)
internal val PreviewCategoryColor = CategoryPastels.first()
internal fun categoryArgb(red: Int, green: Int, blue: Int): Long =
    0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()

internal fun Color.readableOn(background: Color): Color {
    val backgroundLuminance = background.luminance()
    fun contrast(color: Color): Float {
        val foregroundLuminance = color.luminance()
        return (maxOf(foregroundLuminance, backgroundLuminance) + .05f) /
            (minOf(foregroundLuminance, backgroundLuminance) + .05f)
    }
    if (contrast(this) >= 4.5f) return this

    val target = if (contrast(Color.Black) > contrast(Color.White)) Color.Black else Color.White
    var lower = 0f
    var upper = 1f
    repeat(10) {
        val mix = (lower + upper) / 2f
        if (contrast(lerp(this, target, mix)) >= 4.5f) upper = mix else lower = mix
    }
    return lerp(this, target, upper)
}

internal fun ExpendiColors.accentText(background: Color): Color = accent.readableOn(background)

internal val ToastSurface = DarkSurface.toArgb()
internal val ToastText = DarkText.toArgb()
internal val ToastWarning: Int get() = DarkTokens.warning.toArgb()
internal val ToastSuccess = Mint.toArgb()
