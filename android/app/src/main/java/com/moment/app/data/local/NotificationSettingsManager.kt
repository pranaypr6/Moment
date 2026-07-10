package com.moment.app.data.local

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_NOTIFS_MOMENT = "notifs_moment"
        private const val KEY_NOTIFS_REACTION = "notifs_reaction"
        private const val KEY_NOTIFS_WIDGET = "notifs_widget"
    }

    var momentNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFS_MOMENT, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFS_MOMENT, value).apply()

    var reactionNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFS_REACTION, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFS_REACTION, value).apply()

    var widgetAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFS_WIDGET, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFS_WIDGET, value).apply()
}
