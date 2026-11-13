package ar.edu.utn.frba.expendinator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.utn.frba.expendinator.ui.theme.ExpendiTheme
import ar.edu.utn.frba.expendinator.ui.theme.category
import ar.edu.utn.frba.expendinator.ui.theme.accentText
import ar.edu.utn.frba.expendinator.ui.theme.readableOn

@Composable
fun ExpendiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    radius: Dp = 16.dp,
    border: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val stroke = BorderStroke(1.dp, border)
    if (onClick == null) Card(modifier, shape, colors, border = stroke, content = content)
    else Card(onClick, modifier, shape = shape, colors = colors, border = stroke, content = content)
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = ExpendiTheme.colors
    Button(
        onClick = onClick, modifier = modifier.heightIn(min = 56.dp), enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryButton, contentColor = colors.onPrimaryButton)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryTextButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    color: Color = ExpendiTheme.colors.textSoft, enabled: Boolean = true
) {
    TextButton(onClick, modifier.heightIn(min = 44.dp), enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = color)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun IconSquareButton(icon: ImageVector, label: String, onClick: () -> Unit, prominent: Boolean = false) {
    val tokens = ExpendiTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (prominent) tokens.primaryButton else MaterialTheme.colorScheme.surface,
        contentColor = if (prominent) tokens.onPrimaryButton else MaterialTheme.colorScheme.onSurface,
        border = if (prominent) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(22.dp)) }
    }
}

@Composable
fun CategoryChip(label: String, storedColor: Long?, modifier: Modifier = Modifier) {
    val tokens = ExpendiTheme.colors
    val color = tokens.category(storedColor)
    val tint = color.copy(alpha = tokens.categoryBackgroundAlpha)
    val background = tint.compositeOver(MaterialTheme.colorScheme.surface)
    Surface(modifier, shape = RoundedCornerShape(99.dp), color = tint) {
        Text(label, Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = color.readableOn(background),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CategoryAvatar(label: String, storedColor: Long?) {
    val tokens = ExpendiTheme.colors
    val color = tokens.category(storedColor)
    val tint = color.copy(alpha = tokens.categoryBackgroundAlpha)
    val background = tint.compositeOver(MaterialTheme.colorScheme.surface)
    Surface(Modifier.size(42.dp), shape = RoundedCornerShape(14.dp),
        color = tint) {
        Box(contentAlignment = Alignment.Center) {
            Text(label.trim().take(1).uppercase(), color = color.readableOn(background), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ExpenseRow(title: String, metadata: String, amount: String, categoryColor: Long?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryAvatar(title, categoryColor)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(metadata, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(amount, style = ExpendiTheme.typography.amount)
    }
}

@Composable
fun ProgressBar(ratio: Float, color: Color) {
    val shape = RoundedCornerShape(99.dp)
    Box(Modifier.fillMaxWidth().height(8.dp).clip(shape).background(MaterialTheme.colorScheme.outline)) {
        Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).fillMaxHeight().clip(shape)
            .background(if (ratio > 1f) ExpendiTheme.colors.warning else color))
    }
}

@Composable
fun WarningBanner(message: String, icon: ImageVector = RoundedSymbols.WarningAmber) {
    val tokens = ExpendiTheme.colors
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = tokens.warningContainer, border = BorderStroke(1.dp, tokens.warningBorder)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(20.dp), tint = tokens.warning)
            Text(message, color = tokens.onWarningContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SegmentedSelector(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    ExpendiCard(Modifier.fillMaxWidth(), radius = 99.dp) {
        Row(Modifier.fillMaxWidth().selectableGroup().padding(4.dp)) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                val shape = RoundedCornerShape(99.dp)
                Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(shape)
                    .background(if (active) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.surface)
                    .selectable(active, role = Role.RadioButton, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

@Composable
fun ExpendiBottomBar(destinations: List<BottomDestination>, current: String?, onSelect: (String) -> Unit) {
    ExpendiCard(
        Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp).fillMaxWidth(),
        radius = 24.dp
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).selectableGroup()) {
            destinations.forEach { destination ->
                val selected = destination.route == current
                val color = if (selected) ExpendiTheme.colors.accent else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    Modifier.weight(1f).heightIn(min = 68.dp)
                        .selectable(selected, role = Role.Tab, onClick = { onSelect(destination.route) }),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(destination.icon, null, Modifier.size(22.dp), tint = color)
                    Spacer(Modifier.height(4.dp))
                    Text(destination.label, style = MaterialTheme.typography.labelSmall,
                        color = if (selected) ExpendiTheme.colors.accentText(MaterialTheme.colorScheme.surface) else color,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun expendInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline
)

