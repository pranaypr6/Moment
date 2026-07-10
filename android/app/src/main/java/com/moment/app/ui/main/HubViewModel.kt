package com.moment.app.ui.main

import androidx.lifecycle.ViewModel
import com.moment.app.data.local.NotificationSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HubViewModel @Inject constructor(
    private val notificationSettingsManager: NotificationSettingsManager
) : ViewModel() {

    private val _momentNotifs = MutableStateFlow(notificationSettingsManager.momentNotificationsEnabled)
    val momentNotifs: StateFlow<Boolean> = _momentNotifs.asStateFlow()

    private val _reactionNotifs = MutableStateFlow(notificationSettingsManager.reactionNotificationsEnabled)
    val reactionNotifs: StateFlow<Boolean> = _reactionNotifs.asStateFlow()

    private val _widgetAlerts = MutableStateFlow(notificationSettingsManager.widgetAlertsEnabled)
    val widgetAlerts: StateFlow<Boolean> = _widgetAlerts.asStateFlow()

    fun setMomentNotifs(enabled: Boolean) {
        notificationSettingsManager.momentNotificationsEnabled = enabled
        _momentNotifs.value = enabled
    }

    fun setReactionNotifs(enabled: Boolean) {
        notificationSettingsManager.reactionNotificationsEnabled = enabled
        _reactionNotifs.value = enabled
    }

    fun setWidgetAlerts(enabled: Boolean) {
        notificationSettingsManager.widgetAlertsEnabled = enabled
        _widgetAlerts.value = enabled
    }
}
