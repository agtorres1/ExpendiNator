package ar.edu.utn.frba.expendinator.screens.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import ar.edu.utn.frba.expendinator.models.Expense
import kotlin.math.roundToInt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import ar.edu.utn.frba.expendinator.ui.components.*
import ar.edu.utn.frba.expendinator.ui.theme.*
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.utn.frba.ExpendinatorApp.R
import ar.edu.utn.frba.expendinator.screens.expenses.ExpenseListViewModel

@Composable
fun MetricsScreen(
    expenseVm: ExpenseListViewModel = viewModel(),
    metricsVm: MetricsViewModel = viewModel()
) {
    val expenses by expenseVm.uiState.collectAsState()
    val start by metricsVm.startDate.collectAsState()
    val end by metricsVm.endDate.collectAsState()
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    val today = LocalDate.now()
    val starts = listOf(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
        today.withDayOfMonth(1), today.withDayOfYear(1))
    val categories = remember(expenses, start, end) { metricsVm.getCategoryData(expenses) }
    val daily = remember(expenses, start, end) { metricsVm.getDailyData(expenses) }
    val previous = when (selected) {
        0 -> start.minusWeeks(1)
        1 -> start.minusMonths(1)
        2 -> start.minusYears(1)
        else -> null
    }
    val previousData = remember(expenses, previous, start) {
        if (previous == null) emptyMap() else expenses.filter {
            val date = it.date.take(10)
            date >= previous.toString() && date < start.toString()
        }.groupBy { it.category?.name }.mapValues { (_, rows) -> rows.sumOf(Expense::amount) }
    }
    val increases = categories.mapNotNull { category ->
        val baseline = previousData[category.name]?.takeIf { it > 0 } ?: return@mapNotNull null
        val change = (category.total / baseline - 1) * 100
        if (change > 20) category.name to change.roundToInt() else null
    }

    MetricsContent(
        categories, daily, start, end, selected,
        onSelect = {
            selected = it
            metricsVm.setStartDate(starts[it])
            metricsVm.setEndDate(today)
        },
        onStart = { selected = -1; metricsVm.setStartDate(it) },
        onEnd = { selected = -1; metricsVm.setEndDate(it) },
        warnings = increases.map { (name, percent) ->
            stringResource(R.string.aumento_categoria, name, percent,
                if (selected == 1 && previous != null) monthName(java.time.YearMonth.from(previous))
                else stringResource(R.string.periodo_anterior))
        }
    )
}

@Composable
private fun MetricsContent(
    categories: List<CategoryData>, daily: List<DailyData>, start: LocalDate, end: LocalDate,
    selected: Int, onSelect: (Int) -> Unit, onStart: (LocalDate) -> Unit, onEnd: (LocalDate) -> Unit,
    warnings: List<String>
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.metricas), style = MaterialTheme.typography.titleLarge)
        SegmentedSelector(listOf(stringResource(R.string.semana), stringResource(R.string.mes),
            stringResource(R.string.anio)), selected, onSelect)
        if (categories.isNotEmpty()) CategoryDonut(categories)
        else ExpendiCard(Modifier.fillMaxWidth(), radius = 24.dp) {
            Text(stringResource(R.string.no_hay_datos_para_este_periodo), Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        WeeklyChart(daily, start, end)
        warnings.forEach { WarningBanner(it, Icons.Rounded.TrendingUp) }
        FiltersCard(start, end, onStart, onEnd)
    }
}

@Composable
private fun CategoryDonut(categories: List<CategoryData>) {
    val tokens = ExpendiTheme.colors
    val scheme = MaterialTheme.colorScheme
    val positiveTotal = categories.sumOf { it.total.coerceAtLeast(0.0) }
    ExpendiCard(Modifier.fillMaxWidth(), radius = 24.dp) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 12.dp.toPx()
                    val bounds = Size(size.width - stroke, size.height - stroke)
                    val offset = Offset(stroke / 2, stroke / 2)
                    drawArc(scheme.outline, 0f, 360f, false, offset, bounds, style = Stroke(stroke))
                    var angle = -90f
                    if (positiveTotal > 0) categories.forEach { category ->
                        val sweep = (category.total.coerceAtLeast(0.0) / positiveTotal * 360).toFloat()
                        if (sweep > 0f) drawArc(tokens.category(category.color), angle, sweep, false,
                            offset, bounds, style = Stroke(stroke, cap = StrokeCap.Butt))
                        angle += sweep
                    }
                }
                Column(Modifier.widthIn(max = 98.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.total), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    Text(peso(categories.sumOf { it.total }), style = ExpendiTheme.typography.amount.copy(fontSize = 16.sp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(8.dp).background(tokens.category(category.color), CircleShape))
                        Text(category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = tokens.textSoft)
                        Text(stringResource(R.string.porcentaje, (category.percentage * 100).roundToInt()),
                            style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyChart(daily: List<DailyData>, start: LocalDate, end: LocalDate) {
    if (end < start) return
    val weeks = List(4) { index ->
        val weekStart = if (end >= start.plusWeeks(4)) maxOf(start, end.minusDays(27)).plusWeeks(index.toLong())
            else start.plusWeeks(index.toLong())
        val weekEnd = weekStart.plusDays(6)
        val total = daily.filter { it.date in weekStart.toString()..minOf(weekEnd, end).toString() }.sumOf { it.total }
        total to (LocalDate.now() in weekStart..weekEnd)
    }
    val maximum = weeks.maxOf { it.first }.coerceAtLeast(1.0)
    val tokens = ExpendiTheme.colors
    val scheme = MaterialTheme.colorScheme
    ExpendiCard(Modifier.fillMaxWidth(), radius = 24.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.por_semana), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom) {
                weeks.forEachIndexed { index, (total, current) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(peso(total), style = ExpendiTheme.typography.amount.copy(fontSize = 11.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val shape = RoundedCornerShape(10.dp)
                        val bar = Modifier.fillMaxWidth(.7f).height((92 * total.coerceAtLeast(0.0) / maximum).dp.coerceAtLeast(4.dp))
                        Box(if (current) bar.border(1.dp, scheme.outline, shape) else bar.background(tokens.accent, shape))
                        Text(stringResource(R.string.semana_abrev, index + 1), style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Preview(name = "Oscuro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Claro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun MetricsPreview() {
    ExpendinatorTheme(ThemeMode.SYSTEM) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val today = LocalDate.now()
            MetricsContent(listOf(CategoryData(stringResource(R.string.ejemplo_gasto), 27000.0, PreviewCategoryColor, 1f)),
                listOf(DailyData(today.toString(), 27000.0)), today.withDayOfMonth(1), today, 1, {}, {}, {}, emptyList())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersCard(
    startDate: java.time.LocalDate,
    endDate: java.time.LocalDate,
    onStartDateChange: (java.time.LocalDate) -> Unit,
    onEndDateChange: (java.time.LocalDate) -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ExpendiCard(Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.filtros),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Fecha Inicio
            OutlinedTextField(
                value = startDate.toString(),
                onValueChange = {},
                label = { Text(stringResource(R.string.fecha_inicio)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Rounded.DateRange, "Seleccionar fecha")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fecha Fin
            OutlinedTextField(
                value = endDate.toString(),
                onValueChange = {},
                label = { Text(stringResource(R.string.fecha_fin)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showEndDatePicker = true }) {
                        Icon(Icons.Rounded.DateRange, "Seleccionar fecha")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Date Picker para fecha inicio
    if (showStartDatePicker) {
        DatePickerModalDialog(
            initialDate = startDate,
            onDateSelected = { newDate ->
                onStartDateChange(newDate)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    // Date Picker para fecha fin
    if (showEndDatePicker) {
        DatePickerModalDialog(
            initialDate = endDate,
            onDateSelected = { newDate ->
                onEndDateChange(newDate)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModalDialog(
    initialDate: java.time.LocalDate,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val localDate = java.time.Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    onDateSelected(localDate)
                }
            }) {
                Text(stringResource(R.string.aceptar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

