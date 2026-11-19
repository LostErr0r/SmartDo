package com.example.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Сменная иконка приложения на рабочем столе.
 *
 * Каждая иконка — отдельный <activity-alias> в манифесте (IconAlias_<id>),
 * указывающий на MainActivity. Включён всегда ровно один alias;
 * источник правды — состояние компонентов в PackageManager.
 */
enum class AppIcon(
    val id: String,
    @StringRes val titleRes: Int,
    @DrawableRes val previewRes: Int
) {
    CYBER("cyber", R.string.app_icon_cyber, R.drawable.app_icon_preview_cyber),
    LAYERS("layers", R.string.app_icon_layers, R.drawable.app_icon_preview_layers),
    PEN("pen", R.string.app_icon_pen, R.drawable.app_icon_preview_pen),
    CIRCUIT("circuit", R.string.app_icon_circuit, R.drawable.app_icon_preview_circuit),
    BOOK("book", R.string.app_icon_book, R.drawable.app_icon_preview_book);

    fun component(context: Context) =
        ComponentName(context.packageName, "com.example.myapplication.IconAlias_$id")

    companion object {
        val DEFAULT = CYBER
    }
}

object AppIconManager {

    fun current(context: Context): AppIcon {
        val pm = context.packageManager
        return AppIcon.entries.firstOrNull { icon ->
            when (pm.getComponentEnabledSetting(icon.component(context))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                // DEFAULT = как в манифесте: там включён только DEFAULT
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.DEFAULT
                else -> false
            }
        } ?: AppIcon.DEFAULT
    }

    fun set(context: Context, icon: AppIcon) {
        if (current(context) == icon) return
        val pm = context.packageManager
        // Сначала включаем новый alias, потом выключаем остальные —
        // чтобы ни в какой момент не остаться без иконки в лаунчере.
        pm.setComponentEnabledSetting(
            icon.component(context),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        AppIcon.entries.filter { it != icon }.forEach { other ->
            pm.setComponentEnabledSetting(
                other.component(context),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
