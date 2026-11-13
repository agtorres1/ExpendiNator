package ar.edu.utn.frba.expendinator.screens.expenses

import ar.edu.utn.frba.expendinator.utils.peso
import ar.edu.utn.frba.expendinator.utils.monthName
import ar.edu.utn.frba.expendinator.utils.shortDate
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import ar.edu.utn.frba.expendinator.ui.components.*
import ar.edu.utn.frba.expendinator.ui.theme.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import kotlin.math.roundToInt
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import ar.edu.utn.frba.expendinator.models.Category
import ar.edu.utn.frba.expendinator.models.Expense
import java.text.SimpleDateFormat
import java.util.Locale
import ar.edu.utn.frba.ExpendinatorApp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    viewModel: ExpenseListViewModel,
    onPhotoCaptured: (Uri) -> Unit = {},
    onExpenseClicked: (Expense) -> Unit = {},
    onSettings: () -> Unit = {},
    setFabAction: ((() -> Unit) -> Unit)? = null,
    scanRequested: Boolean = false,
    onScanRequestHandled: () -> Unit = {}
) {
    val expenses by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    // Actualizar widget cada vez que cambien los gastos
    LaunchedEffect(expenses) {
        viewModel.saveWidgetData(context)
    }

    // ---- Cámara: estado + launchers ----
    // La foto se guarda en resolución completa en un archivo temporal (cache/tickets)
    // y se pasa como Uri a la pantalla de revisión, que la sube al backend.
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingUri
        if (saved && uri != null) {
            onPhotoCaptured(uri)
        }
    }

    val launchCamera = {
        val dir = File(context.cacheDir, "tickets").apply { mkdirs() }
        val file = File(dir, "ticket_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingUri = uri
        cameraLauncher.launch(uri)
    }

    // Pide permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        // TODO: mostrar Snackbar si no se otorgó permiso
    }

    val onFabClick = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            launchCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        // No navegamos acá; navegamos cuando la foto quedó guardada (arriba)
    }

    LaunchedEffect(Unit) {
        setFabAction?.invoke { onFabClick() }
    }

    LaunchedEffect(scanRequested) {
        if (scanRequested) {
            onScanRequestHandled()
            onFabClick()
        }
    }

    HomeContent(expenses, onFabClick, onSettings, onExpenseClicked)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeContent(
    expenses: List<Expense>, onScan: () -> Unit, onSettings: () -> Unit,
    onExpenseClicked: (Expense) -> Unit
) {
    val month = YearMonth.now()
    val latest = remember(expenses) { expenses.sortedByDescending { it.date } }
    val monthExpenses = expenses.filter { it.date.take(7) == month.toString() }
    val total = monthExpenses.sumOf { it.amount }
    val previousMonth = month.minusMonths(1)
    val previous = expenses.filter { it.date.take(7) == previousMonth.toString() }.sumOf { it.amount }
    val change = if (previous > 0) ((total - previous) / previous * 100).roundToInt() else null
    val categories = monthExpenses.groupBy { it.category }.mapValues { (_, entries) -> entries.sumOf { it.amount } }
        .filterValues { it > 0 }.entries.sortedByDescending { it.value }
    val colors = ExpendiTheme.colors
    val scheme = MaterialTheme.colorScheme
    var showAll by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(monthName(month), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    Text(stringResource(R.string.tus_gastos), style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp))
                }
                IconSquareButton(RoundedSymbols.Menu, stringResource(R.string.ajustes), onSettings)
            }
        }
        item {
            ExpendiCard(Modifier.fillMaxWidth(), radius = 24.dp) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.gastaste_mes), style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(peso(total), style = ExpendiTheme.typography.heroAmount, modifier = Modifier.align(Alignment.CenterVertically))
                        if (change != null) {
                            Text(stringResource(R.string.variacion_mes, change), color = colors.accentText(scheme.surface),
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (categories.isEmpty()) Box(Modifier.fillMaxSize().background(scheme.outline, CircleShape))
                        categories.forEach { (category, amount) ->
                            Box(Modifier.weight(amount.toFloat()).fillMaxHeight().background(colors.category(category?.color), CircleShape))
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.take(4).forEach { (category, _) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(colors.category(category?.color), CircleShape))
                                Text(category?.name ?: stringResource(R.string.sin_categoria),
                                    style = MaterialTheme.typography.bodySmall, color = colors.textSoft)
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(onClick = onScan, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                color = colors.primaryButton, contentColor = colors.onPrimaryButton) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(colors.onPrimaryButton.copy(alpha = .12f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) { Icon(RoundedSymbols.DocumentScanner, null) }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.escanear_ticket), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.escanear_ayuda), style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium, color = colors.onPrimaryButton.copy(alpha = .8f))
                    }
                    Icon(RoundedSymbols.KeyboardArrowRight, null)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ultimos_gastos), style = MaterialTheme.typography.titleMedium)
                if (latest.isNotEmpty()) SecondaryTextButton(
                    stringResource(if (showAll) R.string.ver_menos else R.string.ver_todos),
                    { showAll = !showAll }, color = colors.accentText(scheme.background)
                )
            }
        }
        if (latest.isEmpty()) item { Text(stringResource(R.string.sin_gastos_todavia), color = scheme.onSurfaceVariant) }
        items(if (showAll) latest else latest.take(5), key = { it.id }) { expense ->
            val category = expense.category?.name ?: stringResource(R.string.sin_categoria)
            ExpenseRow(expense.title, stringResource(R.string.meta_dos, category, shortDate(expense.date)),
                peso(expense.amount), expense.category?.color) { onExpenseClicked(expense) }
        }
    }
}

@Preview(name = "Oscuro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Claro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun HomePreview() {
    ExpendinatorTheme(ThemeMode.SYSTEM) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeContent(listOf(Expense("0", stringResource(R.string.ejemplo_gasto), 12345.67, null,
                java.time.LocalDate.now().toString())), {}, {}, {})
        }
    }
}
