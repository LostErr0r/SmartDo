package com.example.myapplication.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * Глобальные настройки обратной связи приложения.
 * Значения прокидываются из [com.example.myapplication.data.AppData] в MainActivity
 * и читаются в местах, где воспроизводится звук нажатия или тактильный отклик.
 */
val LocalSoundEnabled = compositionLocalOf { true }
val LocalHapticsEnabled = compositionLocalOf { true }

/** Нужно ли спрашивать подтверждение перед удалением задачи. */
val LocalConfirmDelete = compositionLocalOf { false }
