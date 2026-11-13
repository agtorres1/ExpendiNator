package ar.edu.utn.frba.expendinator.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.utn.frba.ExpendinatorApp.R
import ar.edu.utn.frba.expendinator.ui.components.*
import ar.edu.utn.frba.expendinator.ui.theme.*

@Composable
fun SettingsScreen(
    mode: ThemeMode, alerts: Boolean, onMode: (ThemeMode) -> Unit,
    onAlerts: (Boolean) -> Unit, onBack: () -> Unit, onCategories: () -> Unit,
    onLogout: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = ExpendiTheme.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconSquareButton(RoundedSymbols.ArrowBack, stringResource(R.string.volver), onBack)
            Text(stringResource(R.string.ajustes), style = MaterialTheme.typography.titleLarge)
        }
        Text(stringResource(R.string.apariencia), style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { option ->
                val selected = mode == option
                val shape = RoundedCornerShape(16.dp)
                Surface(
                    Modifier.weight(1f).clip(shape).selectable(selected, role = Role.RadioButton, onClick = { onMode(option) }),
                    shape = shape, color = scheme.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) tokens.accent else scheme.outline)
                ) {
                    Column(Modifier.padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        ThemeThumbnail(option)
                        Spacer(Modifier.height(7.dp))
                        Text(stringResource(when (option) {
                            ThemeMode.DARK -> R.string.modo_oscuro
                            ThemeMode.LIGHT -> R.string.modo_claro
                            ThemeMode.SYSTEM -> R.string.modo_sistema
                        }), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        Text(stringResource(R.string.modo_sistema_ayuda), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        Text(stringResource(R.string.general), style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant)
        ExpendiCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.moneda))
                Text(stringResource(R.string.moneda_ars), color = scheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.avisos_presupuesto))
                    Text(stringResource(R.string.avisos_presupuesto_ayuda),
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
                Switch(alerts, onAlerts, colors = SwitchDefaults.colors(
                    checkedTrackColor = tokens.accent, checkedThumbColor = tokens.onPrimaryButton))
            }
            HorizontalDivider()
            Surface(onClick = onCategories, color = scheme.surface) {
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.mis_categorias))
                    Icon(RoundedSymbols.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
                }
            }
        }
        ExpendiCard(Modifier.fillMaxWidth(), onClick = onLogout) {
            Box(Modifier.fillMaxWidth().heightIn(min = 52.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.cerrar_sesion), color = tokens.warning, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ThemeThumbnail(mode: ThemeMode) {
    Canvas(Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(10.dp))) {
        fun drawScreen(dark: Boolean) {
            val scheme = if (dark) DarkScheme else LightScheme
            val unit = size.width / 10
            drawRect(scheme.background)
            drawRoundRect(scheme.surface, Offset(unit, 12.dp.toPx()),
                Size(unit * 8, 42.dp.toPx()), CornerRadius(6.dp.toPx()))
            drawRoundRect(scheme.onSurfaceVariant, Offset(unit * 2, 22.dp.toPx()),
                Size(unit * 5, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawRoundRect(scheme.outline, Offset(unit * 2, 32.dp.toPx()),
                Size(unit * 4, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawRoundRect(Mint, Offset(unit, 60.dp.toPx()),
                Size(unit * 8, 12.dp.toPx()), CornerRadius(4.dp.toPx()))
        }
        drawScreen(mode != ThemeMode.LIGHT)
        if (mode == ThemeMode.SYSTEM) {
            val diagonal = Path().apply {
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            clipPath(diagonal) { drawScreen(false) }
        }
    }
}

@Preview(name = "Oscuro", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Claro", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SettingsPreview() {
    ExpendinatorTheme(ThemeMode.SYSTEM) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(ThemeMode.DARK, true, {}, {}, {}, {}, {})
        }
    }
}
