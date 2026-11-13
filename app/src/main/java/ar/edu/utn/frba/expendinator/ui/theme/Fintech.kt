package ar.edu.utn.frba.expendinator.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

internal val FintechShape = RoundedCornerShape(16.dp)

@Composable
internal fun FintechCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick == null) {
        Card(modifier, FintechShape, colors, border = border, content = content)
    } else {
        Card(onClick, modifier, shape = FintechShape, colors = colors, border = border, content = content)
    }
}

internal fun peso(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(amount)

internal fun monthName(month: java.time.YearMonth): String =
    month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-AR")))
        .replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-AR")) }

internal fun shortDate(value: String): String = runCatching {
    java.time.LocalDate.parse(value.take(10)).format(
        java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-AR"))
    )
}.getOrDefault(value)
