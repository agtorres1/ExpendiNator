package ar.edu.utn.frba.expendinator.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import ar.edu.utn.frba.expendinator.ui.components.*
import ar.edu.utn.frba.expendinator.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import ar.edu.utn.frba.expendinator.model.dto.OcrPreview
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.utn.frba.ExpendinatorApp.R
import ar.edu.utn.frba.expendinator.model.dto.OcrItem
import ar.edu.utn.frba.expendinator.models.Category
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun OcrReviewScreen(
    ocrVm: OcrViewModel = viewModel(),
    expensesVm: ExpenseListViewModel,
    onRetake: (() -> Unit)? = null,
    onConfirmed: () -> Unit,
    fromCamera: Boolean = false,
) {
    val state by ocrVm.ui.collectAsState()
    val categories by expensesVm.categoriesFlow.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryDialogError by remember { mutableStateOf<String?>(null) }
    var creatingCategory by remember { mutableStateOf(false) }
    var pendingCategoryIndex by remember { mutableStateOf<Int?>(null) }

    val nombreRequeridoText = stringResource(R.string.nombre_requerido)
    val noSePudoCrearCategoriaText = stringResource(R.string.no_se_pudo_crear_categoria)

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { ocrVm.startScan(context) }
    LaunchedEffect(categories.isEmpty()) {
        if (categories.isEmpty()) {
            expensesVm.refreshAll()
        }
    }

    when (state) {
        is OcrUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator(color = ExpendiTheme.colors.accent)
                    Text(stringResource(R.string.leyendo_ticket), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        is OcrUiState.Error -> {
            val msg = (state as OcrUiState.Error).message
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WarningBanner(msg)
                    onRetake?.let { SecondaryTextButton(stringResource(R.string.sacar_otra_foto), it, Modifier.fillMaxWidth()) }
                }
            }
        }
        is OcrUiState.Preview -> {
            val preview = (state as OcrUiState.Preview)
            ReceiptContent(
                data = preview.data, entries = preview.editable, fromCamera = fromCamera,
                onConfirm = { ocrVm.confirm(onDone = onConfirmed) }, onRetake = onRetake
            ) { index, item ->
                OcrItemEditor(
                    item = item, currency = preview.data.currency, categories = categories,
                    onChange = { preview.editable[index] = it },
                    onAddCategoryRequested = {
                        pendingCategoryIndex = index
                        categoryDialogError = null
                        showAddCategoryDialog = true
                    }
                )
            }
        }
        OcrUiState.Confirming -> {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        OcrUiState.Confirmed, OcrUiState.Idle -> Unit
    }

    if (showAddCategoryDialog) {
        val preview = state as? OcrUiState.Preview
        AddCategoryDialog(
            isLoading = creatingCategory,
            error = categoryDialogError,
            onDismiss = {
                if (!creatingCategory) {
                    showAddCategoryDialog = false
                    categoryDialogError = null
                    pendingCategoryIndex = null
                }
            },
            onConfirm = { name, keywords ->
                val idx = pendingCategoryIndex ?: return@AddCategoryDialog
                if (name.isBlank()) {
                    categoryDialogError = nombreRequeridoText
                    return@AddCategoryDialog
                }
                scope.launch {
                    creatingCategory = true
                    val success = expensesVm.createCategory(
                        name.trim(),
                        randomCategoryColor(),
                        keywords
                    )
                    creatingCategory = false
                    if (success) {
                        preview?.editable?.let { list ->
                            val created = expensesVm.categoriesFlow.value.lastOrNull { it.name == name.trim() }
                            list[idx] = list[idx].copy(
                                categoryName = name.trim(),
                                categoryId = created?.id?.toIntOrNull()
                            )
                        }
                        showAddCategoryDialog = false
                        categoryDialogError = null
                        pendingCategoryIndex = null
                    } else {
                        categoryDialogError = noSePudoCrearCategoriaText
                    }
                }
            }
        )
    }
}

@Composable
private fun ReceiptContent(
    data: OcrPreview, entries: List<OcrItem>, fromCamera: Boolean,
    onConfirm: () -> Unit, onRetake: (() -> Unit)?,
    itemContent: @Composable (Int, OcrItem) -> Unit
) {
    val tokens = ExpendiTheme.colors
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                ExpendiCard(Modifier.fillMaxWidth(), radius = 24.dp) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val merchant = data.merchant?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.ticket_id, data.receiptId)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryAvatar(merchant, null)
                            Column(Modifier.weight(1f)) {
                                Text(merchant, style = MaterialTheme.typography.titleMedium)
                                val count = androidx.compose.ui.res.pluralStringResource(R.plurals.ticket_items, entries.size, entries.size)
                                Text(stringResource(R.string.meta_dos, shortDate(data.date), count),
                                    style = MaterialTheme.typography.bodySmall, color = muted)
                            }
                            if (fromCamera) Surface(
                                shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, tokens.accent.copy(alpha = .4f))
                            ) {
                                Row(Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(13.dp), tint = tokens.accent)
                                    Text(stringResource(R.string.leido_ia), style = MaterialTheme.typography.labelSmall, color = tokens.accent)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text(stringResource(R.string.total_ticket), style = MaterialTheme.typography.bodyMedium, color = muted)
                        Text(receiptAmount(data.total, data.currency), style = ExpendiTheme.typography.amount.copy(fontSize = 30.sp))
                    }
                }
            }
            data.warnings.forEach { warning -> item { WarningBanner(warning) } }
            item { Text(stringResource(R.string.editar_item_ayuda), style = MaterialTheme.typography.bodyMedium, color = muted) }
            itemsIndexed(entries, key = { index, _ -> index }) { index, item -> itemContent(index, item) }
        }
        PrimaryButton(stringResource(R.string.confirmar_y_guardar), onConfirm, Modifier.fillMaxWidth())
        onRetake?.let { SecondaryTextButton(stringResource(R.string.sacar_otra_foto), it, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(8.dp))
    }
}

@Preview(name = "Oscuro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Claro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ReceiptPreview() {
    ExpendinatorTheme(ThemeMode.SYSTEM) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val item = OcrItem(stringResource(R.string.ejemplo_gasto), 18250.0, date = "2026-11-13")
            val data = OcrPreview("demo", "ARS", item.date, listOf(item), item.amount, merchant = item.title)
            ReceiptContent(data, data.items, true, {}, {}) { _, entry ->
                OcrItemEditor(entry, data.currency, emptyList(), {}, {})
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrItemEditor(
    item: OcrItem,
    currency: String,
    categories: List<Category>,
    onChange: (OcrItem) -> Unit,
    onAddCategoryRequested: () -> Unit
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    if (!editing) {
        ExpendiCard(Modifier.fillMaxWidth(), onClick = { editing = true }) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                    val category = categories.firstOrNull { it.id == item.categoryId?.toString() }
                        ?: categories.firstOrNull { it.name == item.categoryName }
                    CategoryChip(category?.name ?: item.categoryName ?: stringResource(R.string.sin_categoria), category?.color)

                }
                Text(receiptAmount(item.amount, currency), style = ExpendiTheme.typography.amount,
                    color = if (item.amount < 0) ExpendiTheme.colors.accent else MaterialTheme.colorScheme.onSurface)
            }
        }
        return
    }
    ExpendiCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                colors = expendInputColors(),
                shape = MaterialTheme.shapes.small,
                value = item.title,
                onValueChange = { onChange(item.copy(title = it)) },
                label = { Text(stringResource(R.string.titulo)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            var amountText by rememberSaveable { mutableStateOf(item.amount.toString()) }
            OutlinedTextField(
                colors = expendInputColors(),
                shape = MaterialTheme.shapes.small,
                value = amountText,
                onValueChange = { txt ->
                    amountText = txt
                    txt.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }?.let {
                        onChange(item.copy(amount = it))
                    }
                },
                label = { Text(stringResource(R.string.monto_solo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            var expanded by remember { mutableStateOf(false) }
            val categoryLabel = item.categoryName ?: stringResource(R.string.sin_categoria)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                colors = expendInputColors(),
                shape = MaterialTheme.shapes.small,
                    value = categoryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.categoria)) },
                    leadingIcon = {
                        val color = item.categoryId?.let { id ->
                            categories.firstOrNull { it.id == id.toString() }?.color
                        }
                        CategoryColorDot(color)
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sin_categoria)) },
                        onClick = {
                            expanded = false
                            onChange(item.copy(categoryId = null, categoryName = null))
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            leadingIcon = { CategoryColorDot(category.color) },
                            text = { Text(category.name) },
                            onClick = {
                                expanded = false
                                onChange(item.copy(categoryId = category.id.toIntOrNull(), categoryName = category.name))
                            }
                        )
                    }
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.agregar_categoria)) },
                        onClick = {
                            expanded = false
                            onAddCategoryRequested()
                        }
                    )
                }
            }
            OutlinedTextField(
                colors = expendInputColors(),
                shape = MaterialTheme.shapes.small,
                value = item.date,
                onValueChange = { onChange(item.copy(date = it)) },
                label = { Text(stringResource(R.string.fecha_formato)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryTextButton(stringResource(R.string.listo), { editing = false })
        }
    }
}

private fun receiptAmount(amount: Double, currency: String): String =
    if (currency.equals("ARS", ignoreCase = true)) peso(amount) else "$currency $amount"

@Composable
private fun CategoryColorDot(color: Long?) {
    val actualColor = ExpendiTheme.colors.category(color)
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(actualColor, shape = CircleShape)
    )
}

@Composable
private fun AddCategoryDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var keywordsText by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nueva_categoria)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                colors = expendInputColors(),
                shape = MaterialTheme.shapes.small,
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.nombre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                colors = expendInputColors(),
                shape = MaterialTheme.shapes.small,
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text(stringResource(R.string.keywords_separadas_por_coma)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val keywords = keywordsText.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                onConfirm(name, keywords)
            }, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.crear))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
}

private fun randomCategoryColor(): Long {
    val r = Random.nextInt(90, 220)
    val g = Random.nextInt(90, 220)
    val b = Random.nextInt(90, 220)
    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}
