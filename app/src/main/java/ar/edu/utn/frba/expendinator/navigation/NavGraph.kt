package ar.edu.utn.frba.expendinator.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ar.edu.utn.frba.ExpendinatorApp.R
import ar.edu.utn.frba.expendinator.data.remote.ApiClient
import ar.edu.utn.frba.expendinator.screens.auth.AuthViewModel
import ar.edu.utn.frba.expendinator.screens.auth.LoginScreen
import ar.edu.utn.frba.expendinator.screens.auth.RegisterScreen
import ar.edu.utn.frba.expendinator.screens.budgets.BudgetCreateScreen
import ar.edu.utn.frba.expendinator.screens.budgets.BudgetEditScreen
import ar.edu.utn.frba.expendinator.screens.budgets.BudgetScreen
import ar.edu.utn.frba.expendinator.screens.budgets.BudgetViewModel
import ar.edu.utn.frba.expendinator.screens.categories.CategoryCreateScreen
import ar.edu.utn.frba.expendinator.screens.expenses.ExpenseDetailScreen
import ar.edu.utn.frba.expendinator.screens.expenses.ExpenseListScreen
import ar.edu.utn.frba.expendinator.screens.expenses.ExpenseListViewModel
import ar.edu.utn.frba.expendinator.screens.expenses.OcrReviewScreen
import ar.edu.utn.frba.expendinator.screens.expenses.OcrViewModel
import kotlinx.coroutines.launch
import ar.edu.utn.frba.expendinator.data.ThemePreferences
import ar.edu.utn.frba.expendinator.screens.settings.SettingsScreen
import ar.edu.utn.frba.expendinator.ui.components.*
import ar.edu.utn.frba.expendinator.ui.theme.ThemeMode
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import ar.edu.utn.frba.expendinator.screens.metrics.MetricsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(preferences: ThemePreferences, mode: ThemeMode) {

    val nav = rememberNavController()
    LaunchedEffect(Unit) {
        NavigationController.navController = nav
    }
    val vm: ExpenseListViewModel = viewModel()
    val budgetVm: BudgetViewModel = viewModel()
    val authVm: AuthViewModel = viewModel()

    val scope = rememberCoroutineScope()


    val currentRoute by nav.currentBackStackEntryAsState()
        .let { state -> derivedStateOf { state.value?.destination?.route } }

    val primaryDestinations = listOf(
        BottomDestination(Dest.Main.route, stringResource(R.string.inicio), Icons.Rounded.Home),
        BottomDestination(Dest.Metrics.route, stringResource(R.string.metricas), Icons.Rounded.BarChart),
        BottomDestination(Dest.Budget.route, stringResource(R.string.presupuestos), Icons.Rounded.AccountBalanceWallet),
        BottomDestination(Dest.Categories.route, stringResource(R.string.categorias), Icons.Rounded.Category)
    )
    val topTitle = when (currentRoute) {
        "detail/{id}" -> R.string.detalle
        Dest.OcrReview.route -> R.string.revisar_ticket
        Dest.Categories.route -> R.string.categorias
        else -> null
    }

    val topBarActions = remember { mutableStateOf<@Composable RowScope.() -> Unit>({}) }
    var detailIsEditing by rememberSaveable { mutableStateOf(false) }

    var fabAction by remember { mutableStateOf<() -> Unit>({}) }

    val ocrVm: OcrViewModel = viewModel()

    Scaffold(
        topBar = {
            if (topTitle != null) {
                TopAppBar(
                    title = { Text(stringResource(topTitle)) },
                    navigationIcon = {
                        IconSquareButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.volver),
                            { nav.popBackStack() })
                    },
                    actions = topBarActions.value,
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (primaryDestinations.any { it.route == currentRoute }) {
                ExpendiBottomBar(primaryDestinations, currentRoute) { route ->
                    if (currentRoute != route) {
                        nav.navigate(route) {
                            popUpTo(Dest.Main.route)
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
            NavHost(
                navController = nav,
                startDestination = Dest.startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {

                composable(Dest.Login.route) {
                    LoginScreen(
                        viewModel = authVm,
                        onSuccess = {
                            vm.refreshAll()
                            budgetVm.refresh()
                            nav.navigate(Dest.Main.route) {
                                popUpTo(Dest.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = { nav.navigate(Dest.Register.route) }
                    )
                }

                composable(Dest.Register.route) {
                    RegisterScreen(
                        viewModel = authVm,
                        onSuccess = {
                            vm.refreshAll()
                            budgetVm.refresh()
                            nav.navigate(Dest.Main.route) {
                                popUpTo(Dest.Register.route) { inclusive = true }
                            }
                        },
                        onNavigateToLogin = { nav.navigate(Dest.Login.route) }
                    )
                }

                composable(Dest.Settings.route) {
                    val alerts by preferences.budgetAlerts.collectAsState(false)
                    SettingsScreen(
                        mode, alerts,
                        onMode = { scope.launch { preferences.setThemeMode(it) } },
                        onAlerts = { scope.launch { preferences.setBudgetAlerts(it) } },
                        onBack = { nav.popBackStack() },
                        onCategories = { nav.navigate(Dest.Categories.route) { launchSingleTop = true } },
                        onLogout = {
                            ApiClient.logout()
                            nav.navigate(Dest.Login.route) {
                                popUpTo(nav.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // Home
                composable(Dest.Main.route) { entry ->
                    val scanRequested by entry.savedStateHandle
                        .getStateFlow("scanRequested", false).collectAsState()
                    LaunchedEffect(fabAction) {
                        NavigationController.triggerFab = fabAction
                    }

                    ExpenseListScreen(
                        viewModel = vm,
                        onExpenseClicked = { expense -> nav.navigate("detail/${expense.id}") },
                        onSettings = { nav.navigate(Dest.Settings.route) },
                        setFabAction = { action -> fabAction = action },
                        scanRequested = scanRequested,
                        onScanRequestHandled = { entry.savedStateHandle["scanRequested"] = false },
                        onPhotoCaptured = { uri ->
                            ocrVm.setPendingPhoto(uri)
                            nav.navigate(Dest.OcrReview.route)
                        }
                    )
                }


                // Detalle
                composable(
                    route = "detail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: return@composable

                    LaunchedEffect(Unit) {
                        vm.refreshAll()
                    }

                    var onSave by remember { mutableStateOf<() -> Unit>({}) }
                    var onCancel by remember { mutableStateOf<() -> Unit>({}) }
                    var onStartEdit by remember { mutableStateOf<() -> Unit>({}) }
                    var onDelete by remember { mutableStateOf<() -> Unit>({}) }

                    LaunchedEffect(id, detailIsEditing) {
                        topBarActions.value = {
                            if (detailIsEditing) {
                                IconButton(onClick = { onSave() }) {
                                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.guardar))
                                }
                                IconButton(onClick = { onCancel() }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancelar))
                                }
                            } else {
                                IconButton(onClick = { onStartEdit() }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.editar))
                                }
                                IconButton(onClick = { onDelete() }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.borrar))
                                }
                            }
                        }
                    }
                    DisposableEffect(Unit) { onDispose { topBarActions.value = {} } }

                    ExpenseDetailScreen(
                        id = id,
                        viewModel = vm,
                        onBack = { nav.popBackStack() },
                        onEditingChanged = { detailIsEditing = it },
                        setAppBarHandlers = { save, cancel, startEdit, delete ->
                            onSave = save
                            onCancel = cancel
                            onStartEdit = startEdit
                            onDelete = delete
                        }
                    )
                }

                // Categorías
                composable(Dest.Categories.route) {
                    CategoryCreateScreen(
                        viewModel = vm,
                        onSaved = {
                            vm.refreshAll()
                            nav.popBackStack()
                        }
                    )
                }

                // Presupuestos
                composable(Dest.Budget.route) {
                    BudgetScreen(
                        budgetVm = budgetVm,
                        expensesVm = vm,
                        onNew = { nav.navigate("budget/new") },
                        onBudgetClicked = { nav.navigate("budget/${it.id}/edit") }
                    )
                }

                composable("budget/new") {
                    BudgetCreateScreen(
                        budgetVm = budgetVm,
                        expensesVm = vm,
                        onSaved = { nav.popBackStack() },
                        onCancel = { nav.popBackStack() }
                    )
                }

                composable("budget/{id}/edit") { backStack ->
                    val id = backStack.arguments?.getString("id") ?: return@composable
                    BudgetEditScreen(
                        budgetVm = budgetVm,
                        expensesVm = vm,
                        budgetId = id,
                        onSaved = { nav.popBackStack() },
                        onCancel = { nav.popBackStack() }
                    )
                }

                composable(Dest.Metrics.route) {
                    MetricsScreen(expenseVm = vm)
                }

                composable(Dest.OcrReview.route) {
                    val context = androidx.compose.ui.platform.LocalContext.current

                    OcrReviewScreen(
                        ocrVm,
                        expensesVm = vm,
                        onRetake = {
                            nav.previousBackStackEntry?.savedStateHandle?.set("scanRequested", true)
                            nav.popBackStack()
                        },
                        onConfirmed = {
                            vm.refreshAll()
                            budgetVm.refresh()
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                kotlinx.coroutines.delay(500)
                                vm.saveWidgetData(context)
                            }

                            nav.navigate(Dest.Main.route) {
                                popUpTo(Dest.Main.route) { inclusive = true }
                            }
                        }
                    )
                }

            }
        }
    }
