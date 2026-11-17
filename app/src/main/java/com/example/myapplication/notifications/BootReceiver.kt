package com.example.myapplication.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Восстанавливает уведомления после перезагрузки телефона
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            createNotificationChannel(context)
            // Уведомления восстанавливаются из DataStore при следующем открытии
            // В реальном проекте здесь можно перечитать задачи и пересоздать AlarmManager
        }
    }
}
