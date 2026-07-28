package com.pranayburra.moment.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkState {
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    // Bumped every time the user explicitly asks to retry (e.g. tapping "Try Again" on the
    // maintenance screen). Just flipping isOffline back to false doesn't re-fetch anything on
    // its own - anything that wants to react to a manual retry (like re-pulling the current
    // relationship) should collect this and re-run its fetch whenever it increments.
    private val _retrySignal = MutableStateFlow(0)
    val retrySignal: StateFlow<Int> = _retrySignal.asStateFlow()

    fun setOffline(offline: Boolean) {
        _isOffline.value = offline
    }

    fun retry() {
        _isOffline.value = false
        _retrySignal.value += 1
    }
}
