package com.devdenisviana.gabaritoscanner.ui.theme

import android.content.Context

/** How the app chooses between the light and dark theme. */
enum class ThemeMode(val label: String, val short: String) {
    SYSTEM("Automático (igual ao celular)", "Automático"),
    LIGHT("Claro", "Claro"),
    DARK("Escuro", "Escuro"),
}

/** Remembers the chosen [ThemeMode] on the phone. */
object ThemePreference {
    private const val FILE = "settings"
    private const val KEY = "theme_mode"

    fun load(context: Context): ThemeMode {
        val name = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
        return ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM
    }

    fun save(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, mode.name)
            .apply()
    }
}