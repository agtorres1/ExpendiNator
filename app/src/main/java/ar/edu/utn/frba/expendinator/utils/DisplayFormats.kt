package ar.edu.utn.frba.expendinator.utils

import java.text.NumberFormat
import java.util.Locale

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
