package ar.edu.utn.frba.expendinator.screens.expenses

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    setFabAction: ((() -> Unit) -> Unit)? = null
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
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.sin_gastos_todavia))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses, key = { it.id }) { e ->
                    ExpenseItem(e, onClick = { onExpenseClicked(e) })
                }
            }
        }

    }
}

@Composable
private fun ExpenseItem(e: Expense, onClick: () -> Unit) {
    val dateText = remember(e.date) {
        runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale("es", "AR"))
            val formatter = SimpleDateFormat("d MMM yyyy", Locale("es", "AR"))
            val parsedDate = parser.parse(e.date)
            formatter.format(parsedDate ?: e.date)
        }.getOrElse { e.date }
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    e.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    e.category?.let { CategoryTag(it) }

                    Text(
                        dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "$ ${"%,.2f".format(e.amount)}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CategoryTag(category: Category) {
    val bg = Color(category.color) // color AARRGGBB (Long)
    val textColor = if (bg.luminance() > 0.5f) Color.Black else Color.White

    Surface(
        color = bg,
        contentColor = textColor,
        shape = RoundedCornerShape(50),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpenseItemPreview() {
    val category = Category(id = "0", name = "Categoria", color = 0)
    ExpenseItem(
        e = Expense("0", "Ejemplo", 12345.67, category, "2025-09-21"),
        onClick = {}
    )
}
