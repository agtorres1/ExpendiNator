package ar.edu.utn.frba.expendinator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ar.edu.utn.frba.ExpendinatorApp.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variableFamily(resource: Int, vararg weights: FontWeight) = FontFamily(
    weights.map { weight ->
        Font(resource, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    }
)

private val inter = variableFamily(R.font.inter, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
private val spaceGrotesk = variableFamily(R.font.space_grotesk, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private fun interStyle(size: Int, weight: FontWeight) = TextStyle(
    fontFamily = inter, fontSize = size.sp, fontWeight = weight, lineHeight = (size + 5).sp
)

val Typography = Typography(
    displayLarge = interStyle(40, FontWeight.SemiBold),
    displayMedium = interStyle(34, FontWeight.SemiBold),
    displaySmall = interStyle(30, FontWeight.SemiBold),
    headlineLarge = interStyle(28, FontWeight.Bold),
    headlineMedium = interStyle(24, FontWeight.Bold),
    headlineSmall = interStyle(22, FontWeight.Bold),
    titleLarge = interStyle(22, FontWeight.Bold),
    titleMedium = interStyle(16, FontWeight.SemiBold),
    titleSmall = interStyle(13, FontWeight.SemiBold),
    bodyLarge = interStyle(15, FontWeight.Medium),
    bodyMedium = interStyle(13, FontWeight.Normal),
    bodySmall = interStyle(12, FontWeight.Normal),
    labelLarge = interStyle(14, FontWeight.SemiBold),
    labelMedium = interStyle(12, FontWeight.Medium),
    labelSmall = interStyle(11, FontWeight.Medium)
)

object ExpendiTypography {
    val amount = TextStyle(fontFamily = spaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val heroAmount = amount.copy(fontSize = 40.sp, letterSpacing = (-.02).em)
}
