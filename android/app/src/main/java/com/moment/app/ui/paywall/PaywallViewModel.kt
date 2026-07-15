package com.moment.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moment.app.domain.repository.AuthRepository
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _packages = MutableStateFlow<List<Package>>(emptyList())
    val packages: StateFlow<List<Package>> = _packages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _purchaseSuccess = MutableStateFlow(false)
    val purchaseSuccess: StateFlow<Boolean> = _purchaseSuccess.asStateFlow()

    init {
        fetchOfferings()
    }

    private fun fetchOfferings() {
        _isLoading.value = true
        try {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    _error.value = error.message
                    _isLoading.value = false
                },
                onSuccess = { offerings ->
                    val currentOffering = offerings.current
                    if (currentOffering != null) {
                        _packages.value = currentOffering.availablePackages
                    } else {
                        _error.value = "No offerings available"
                    }
                    _isLoading.value = false
                }
            )
        } catch (e: Exception) {
            _error.value = "RevenueCat not initialized"
            _isLoading.value = false
        }
    }

    fun purchasePackage(activity: Activity, pkg: Package) {
        _isLoading.value = true
        _error.value = null
        try {
            Purchases.sharedInstance.purchaseWith(
                activity,
                packageToPurchase = pkg,
                onError = { error, userCancelled ->
                    if (!userCancelled) {
                        _error.value = error.message
                    }
                    _isLoading.value = false
                },
                onSuccess = { storeTransaction, customerInfo ->
                    if (customerInfo.entitlements["premium"]?.isActive == true) {
                        // Refresh our own local state
                        viewModelScope.launch {
                            try {
                                val current = authRepository.getCachedProfile()
                                if (current != null) {
                                    authRepository.updateProfile(current.displayName ?: "", current.profilePictureUrl) // Hacky way to force profile fetch/refresh from server if we had a webhook, but since we rely on client state, we will rely on PremiumRepository.
                                }
                            } catch (e: Exception) { }
                            _purchaseSuccess.value = true
                            _isLoading.value = false
                        }
                    } else {
                        _error.value = "Purchase succeeded but entitlement not granted."
                        _isLoading.value = false
                    }
                }
            )
        } catch (e: Exception) {
            _error.value = "Purchase failed to start."
            _isLoading.value = false
        }
    }
}
