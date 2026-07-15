package com.moment.app.domain.repository

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumRepository @Inject constructor() {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        try {
            // Listen for real-time updates from RevenueCat
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                updatePremiumState(customerInfo)
            }
        } catch (e: Exception) {
            // Purchases not configured yet
        }
    }

    fun checkPremiumStatus() {
        try {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    // Fallback or handle error
                },
                onSuccess = { customerInfo ->
                    updatePremiumState(customerInfo)
                }
            )
        } catch (e: Exception) {
            // Ignore if Purchases not initialized
        }
    }

    private fun updatePremiumState(customerInfo: CustomerInfo) {
        val entitlement = customerInfo.entitlements["premium"]
        _isPremium.value = entitlement?.isActive == true
    }
}
