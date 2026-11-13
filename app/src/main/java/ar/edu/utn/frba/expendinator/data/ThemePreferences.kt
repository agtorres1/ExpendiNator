package ar.edu.utn.frba.expendinator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ar.edu.utn.frba.expendinator.ui.theme.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appearanceStore by preferencesDataStore("appearance")

class ThemePreferences(context: Context) {
    private val store = context.applicationContext.appearanceStore
    private val themeKey = stringPreferencesKey("theme_mode")
    private val alertsKey = booleanPreferencesKey("budget_alerts")
    private val settings = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val themeMode: Flow<ThemeMode> = settings.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.DARK
    }
    val budgetAlerts: Flow<Boolean> = settings.map { it[alertsKey] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }

    suspend fun setBudgetAlerts(enabled: Boolean) {
        store.edit { it[alertsKey] = enabled }
    }
}
