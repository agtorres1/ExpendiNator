package ar.edu.utn.frba.expendinator.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import ar.edu.utn.frba.expendinator.data.remote.ApiClient


sealed class Dest(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    data object Login : Dest("login")
    data object Register : Dest("register")
    data object Main: Dest("main","Tus gastos", Icons.Default.Home)
    data object Budget: Dest("budget","Presupuestos", Icons.Default.ShoppingCart)
    data object Categories: Dest("categories","Categorías", Icons.Default.Build)
    data object Metrics: Dest("metrics","Métricas", Icons.Default.DateRange)
    data object OcrReview: Dest("ocrReview", "Revisar ticket", Icons.Default.ShoppingCart)

    data object Settings : Dest("settings")

    companion object {
        val drawerItems by lazy { listOf(Main, Budget, Categories, Metrics) }
        val startDestination by lazy { if (ApiClient.isLoggedIn()) Main.route else Login.route }
    }
}
