package com.errorzero.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReminderConfig(val enabled: Boolean = true, val hour: Int = 20, val minute: Int = 0)

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _reminder = MutableStateFlow(
        ReminderConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            hour = prefs.getInt(KEY_HOUR, 20),
            minute = prefs.getInt(KEY_MINUTE, 0),
        ),
    )
    val reminder: StateFlow<ReminderConfig> = _reminder.asStateFlow()

    fun setReminder(config: ReminderConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putInt(KEY_HOUR, config.hour)
            .putInt(KEY_MINUTE, config.minute)
            .apply()
        _reminder.value = config
    }

    var notificationPermissionAsked: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    private companion object {
        const val KEY_ENABLED = "reminder_enabled"
        const val KEY_HOUR = "reminder_hour"
        const val KEY_MINUTE = "reminder_minute"
        const val KEY_ASKED = "notification_permission_asked"
    }
}
