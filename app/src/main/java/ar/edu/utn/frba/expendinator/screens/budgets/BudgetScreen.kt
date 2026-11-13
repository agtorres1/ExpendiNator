package ar.edu.utn.frba.expendinator.screens.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.utn.frba.ExpendinatorApp.R
import ar.edu.utn.frba.expendinator.models.BudgetPeriod
import ar.edu.utn.frba.expendinator.models.Category
import ar.edu.utn.frba.expendinator.screens.expenses.ExpenseListViewModel
import ar.edu.utn.frba.expendinator.ui.components.*
import ar.edu.utn.frba.expendinator.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun BudgetScreen(
    budgetVm: BudgetViewModel,
    expensesVm: ExpenseListViewModel,
    onNew: () -> Unit = {},
    onBudgetClicked: (BudgetUsageUi) -> Unit = {}
) {
    LaunchedEffect(Unit) { budgetVm.refresh() }
    val budgets by budgetVm.budgets.collectAsState()
    BudgetContent(budgets, onNew, onBudgetClicked)
}

@Composable
private fun BudgetContent(budgets: List<BudgetUsageUi>, onNew: () -> Unit, onBudgetClicked: (BudgetUsageUi) -> Unit) {
    val month = YearMonth.now()
    val today = LocalDate.now()
    val monthly = budgets.filter {
        it.period == BudgetPeriod.MONTHLY &&
            it.startDate.take(10) <= today.toString() && it.endDate.take(10) >= today.toString()
    }
    val limit = monthly.sumOf { it.limitAmount }
    val used = monthly.sumOf { it.spent }
    val ratio = if (limit > 0) (used / limit).toFloat().coerceAtLeast(0f) else 0f
    val tokens = ExpendiTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.presupuestos), style = MaterialTheme.typography.titleLarge)
                IconSquareButton(Icons.Rounded.Add, stringResource(R.string.nuevo_presupuesto), onNew, prominent = true)
            }
        }
        if (limit > 0) item {
            ExpendiCard(Modifier.fillMaxWidth(), radius = 24.dp) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { ratio.coerceAtMost(1f) },
                            modifier = Modifier.fillMaxSize(), color = tokens.accent,
                            trackColor = MaterialTheme.colorScheme.outline, strokeWidth = 6.dp)
                        Text(stringResource(R.string.porcentaje, (ratio * 100).roundToInt()),
                            style = ExpendiTheme.typography.amount, color = tokens.accent)
                    }
                    Column {
                        Text(stringResource(R.string.usado_mes, monthName(month)),
                            style = MaterialTheme.typography.bodyMedium, color = muted)
                        Text(peso(used), style = ExpendiTheme.typography.amount.copy(fontSize = 24.sp))
                        val days = month.lengthOfMonth() - today.dayOfMonth
                        Text(stringResource(R.string.meta_dos, stringResource(R.string.de_limite, peso(limit)),
                            pluralStringResource(R.plurals.dias_restantes, days, days)),
                            style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }
            }
        }
        if (budgets.isEmpty()) item {
            Text(stringResource(R.string.sin_presupuestos), color = muted, style = MaterialTheme.typography.bodyMedium)
        }
        items(budgets, key = { it.id }) { budget ->
            val remaining = budget.limitAmount - budget.spent
            val progress = if (budget.limitAmount > 0) (budget.spent / budget.limitAmount).toFloat() else 0f
            val categoryColor = tokens.category(budget.category.color)
            val status = when {
                remaining < 0 -> stringResource(R.string.excedido_monto, peso(-remaining))
                progress >= .8f -> stringResource(R.string.cerca_limite)
                else -> stringResource(R.string.quedan_monto, peso(remaining))
            }
            val statusColor = when {
                remaining < 0 -> tokens.warning
                progress >= .8f -> tokens.onWarningContainer
                else -> muted
            }
            ExpendiCard(Modifier.fillMaxWidth(), { onBudgetClicked(budget) }, radius = 20.dp,
                border = if (remaining < 0) tokens.warning.copy(alpha = .45f) else MaterialTheme.colorScheme.outline) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(categoryColor, CircleShape))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(budget.category.name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp))
                            val period = when (budget.period) {
                                BudgetPeriod.WEEKLY -> R.string.semanal
                                BudgetPeriod.MONTHLY -> R.string.mensual
                                BudgetPeriod.YEARLY -> R.string.anual
                            }
                            CategoryChip(stringResource(period), budget.category.color)
                        }
                        Text(status, color = statusColor, style = MaterialTheme.typography.bodySmall)
                    }
                    ProgressBar(progress, categoryColor)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(peso(budget.spent), style = ExpendiTheme.typography.amount)
                        Text(stringResource(R.string.de_limite, peso(budget.limitAmount)), color = muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Preview(name = "Oscuro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Claro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun BudgetPreview() {
    ExpendinatorTheme(ThemeMode.SYSTEM) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val month = YearMonth.now()
            BudgetContent(listOf(BudgetUsageUi("1", Category("1", stringResource(R.string.ejemplo_gasto), PreviewCategoryColor),
                90000.0, BudgetPeriod.MONTHLY, month.atDay(1).toString(), month.atEndOfMonth().toString(), 72500.0)), {}, {})
        }
    }
}
