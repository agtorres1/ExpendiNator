package ar.edu.utn.frba.expendinator.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import ar.edu.utn.frba.expendinator.ui.components.*
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ar.edu.utn.frba.ExpendinatorApp.R

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    val isLoading = uiState is AuthUiState.Loading
    val match = password == confirm
    val isValid = email.isNotBlank() && password.length >= 6 && match

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.crear_cuenta), style = MaterialTheme.typography.headlineLarge)

        ExpendiCard(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
            radius = 24.dp
        ) {
            Column(Modifier.padding(18.dp)) {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.small,
                    colors = expendInputColors(),
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    shape = MaterialTheme.shapes.small,
                    colors = expendInputColors(),
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_min_6)) },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = stringResource(if (showPassword) R.string.ocultar_password else R.string.mostrar_password)
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    shape = MaterialTheme.shapes.small,
                    colors = expendInputColors(),
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(stringResource(R.string.confirmar_password)) },
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Lock, contentDescription = null) },
                    isError = confirm.isNotEmpty() && !match,
                    supportingText = {
                        if (confirm.isNotEmpty() && !match) Text(stringResource(R.string.password_no_coinciden))
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState is AuthUiState.Error) {
                    Text(
                        (uiState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                PrimaryButton(
                    label = stringResource(if (isLoading) R.string.autenticando else R.string.registrarme),
                    onClick = { viewModel.register(email.trim(), password) },
                    enabled = isValid && !isLoading,
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                )
                SecondaryTextButton(stringResource(R.string.ya_tenes_cuenta), onNavigateToLogin,
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
            }
        }
    }
}
