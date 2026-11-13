package ar.edu.utn.frba.expendinator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ar.edu.utn.frba.expendinator.data.ThemePreferences
import ar.edu.utn.frba.expendinator.ui.theme.ExpendinatorTheme
import ar.edu.utn.frba.expendinator.ui.theme.ThemeMode
import ar.edu.utn.frba.expendinator.ui.theme.isDark
import ar.edu.utn.frba.expendinator.navigation.AppNavHost
import ar.edu.utn.frba.expendinator.navigation.NavigationController
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = ThemePreferences(this)
        setContent {
            val mode by preferences.themeMode.collectAsState(ThemeMode.DARK)
            val dark = mode.isDark()
            SideEffect {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(transparent)
                else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            ExpendinatorTheme(themeMode = mode) {
                AppNavHost(preferences, mode)
            }
        }

        // Verificar después de que todo esté listo
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val openScanner = intent.getBooleanExtra("openScanner", false)
        android.util.Log.d("MainActivity", "handleIntent openScanner = $openScanner")

        if (openScanner) {
            // Esperar a que Compose esté listo
            lifecycleScope.launch {
                kotlinx.coroutines.delay(300) // Esperar 300ms
                NavigationController.navigateToScanner()
            }
        }
    }
}