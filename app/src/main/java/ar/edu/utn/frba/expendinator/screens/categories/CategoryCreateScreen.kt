package ar.edu.utn.frba.expendinator.screens.categories

import androidx.collection.longListOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ar.edu.utn.frba.ExpendinatorApp.R
import ar.edu.utn.frba.expendinator.ui.components.*
import ar.edu.utn.frba.expendinator.ui.theme.*
import ar.edu.utn.frba.expendinator.screens.expenses.ExpenseListViewModel
import ar.edu.utn.frba.expendinator.utils.showErrorToast
import ar.edu.utn.frba.expendinator.utils.showSuccessToast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryCreateScreen(
    viewModel: ExpenseListViewModel,
    onSaved: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val keywords = remember { mutableStateListOf("") }
    var selectedColor by rememberSaveable { mutableLongStateOf(CategoryPastels.first()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val nombreRequeridoText = stringResource(R.string.nombre_requerido)
    val categoriaCreadaCorrectamenteText = stringResource(R.string.categoria_creada_correctamente)
    val noSePudoGuardarCategoriaText = stringResource(R.string.no_se_pudo_guardar_categoria)

    fun removeKeyword(index: Int) {
        if (keywords.size > 1) {
            keywords.removeAt(index)
        }
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExpendiCard(Modifier.fillMaxWidth(), radius = 24.dp) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    colors = expendInputColors(),
                    shape = MaterialTheme.shapes.small,
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.nombre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.color), style = MaterialTheme.typography.labelLarge)
                val colorOptions = CategoryPastels
                FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colorOptions.forEachIndexed { index, color ->
                        val selected = color == selectedColor
                        val description = stringResource(R.string.color_opcion, index + 1)
                        Box(Modifier.size(44.dp).semantics { contentDescription = description }
                            .selectable(selected, role = Role.RadioButton, onClick = { selectedColor = color }),
                            contentAlignment = Alignment.Center) {
                            Box(Modifier.size(32.dp).background(ExpendiTheme.colors.category(color), CircleShape)
                                .border(if (selected) 2.dp else 1.dp,
                                    if (selected) ExpendiTheme.colors.accent else MaterialTheme.colorScheme.outline, CircleShape))
                        }
                    }
                }

                Text(stringResource(R.string.palabras_clave), style = MaterialTheme.typography.labelLarge)

                keywords.forEachIndexed { index, value ->
                    OutlinedTextField(
                        colors = expendInputColors(),
                        shape = MaterialTheme.shapes.small,
                        value = value,
                        onValueChange = { new -> keywords[index] = new },
                        placeholder = { Text(stringResource(R.string.ejemplo_palabras_clave)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = { removeKeyword(index) },
                                enabled = keywords.size > 1,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.eliminar_palabra_clave)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (keywords[index].isNotBlank()) keywords.add("")
                            }
                        )
                    )
                }

                TextButton(onClick = { keywords.add("") }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.agregar_palabra_clave))
                }

                Spacer(Modifier.height(12.dp))

                PrimaryButton(
                    label = stringResource(R.string.guardar),
                    onClick = {
                        if (name.isBlank()) {
                            showErrorToast(context, nombreRequeridoText)
                            return@PrimaryButton
                        }

                        val cleanedKeywords = keywords
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        scope.launch {
                            val ok = viewModel.createCategory(
                                name = name.trim(),
                                color = selectedColor,
                                keywords = cleanedKeywords
                            )
                            if (ok) {
                                showSuccessToast(
                                    context,
                                    categoriaCreadaCorrectamenteText
                                )

                                name = ""
                                keywords.clear()
                                keywords.add("")
                                selectedColor = CategoryPastels.first()

                                onSaved()
                            } else {
                                showErrorToast(
                                    context,
                                    noSePudoGuardarCategoriaText
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
