package com.example.myapplication

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Управление языком приложения с ручным переопределением.
 * Значение хранится в SharedPreferences (синхронно читается в attachBaseContext).
 * Значения: "system" (как в системе), "ru", "en".
 */
object LocaleHelper {
    private const val PREFS = "app_settings"
    private const val KEY = "app_language"

    fun getLanguage(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "system") ?: "system"

    fun setLanguage(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** Оборачивает контекст выбранной локалью (или возвращает исходный для "system"). */
    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        if (lang == "system") return context
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
