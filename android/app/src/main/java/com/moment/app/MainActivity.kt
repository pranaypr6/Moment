package com.moment.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.firebase.messaging.FirebaseMessaging
import com.moment.app.domain.repository.MomentRepository
import com.moment.app.ui.navigation.NavGraph
import com.moment.app.ui.theme.MomentTheme
import com.moment.app.worker.WallpaperWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var momentRepository: MomentRepository
    
    @Inject
    lateinit var authRepository: com.moment.app.domain.repository.AuthRepository

    private val _interactionOverlayState = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _targetTabState = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestNotificationPermission()
        checkInstallReferrer()

        intent?.getStringExtra("interactionType")?.let {
            _interactionOverlayState.value = it
            // Clear the intent so it doesn't re-trigger on configuration changes
            intent?.removeExtra("interactionType")
        }

        intent?.getStringExtra("openTab")?.let {
            _targetTabState.value = it
            intent?.removeExtra("openTab")
        }

        setContent {
            MomentTheme {
                val navController = rememberNavController()
                
                val interactionType by _interactionOverlayState.collectAsState()
                val targetTab by _targetTabState.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(
                            navController = navController,
                            targetTab = targetTab,
                            onTargetTabConsumed = { _targetTabState.value = null }
                        )
                    }
                    
                    if (interactionType != null) {
                        com.moment.app.ui.main.EmotionalOverlay(interactionType!!)
                        
                        // Clear the state after the animation plays (1.2s delay is inside EmotionalOverlay)
                        LaunchedEffect(interactionType) {
                            kotlinx.coroutines.delay(1500)
                            _interactionOverlayState.value = null
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.getStringExtra("interactionType")?.let {
            _interactionOverlayState.value = it
            intent.removeExtra("interactionType")
        }
        intent?.getStringExtra("openTab")?.let {
            _targetTabState.value = it
            intent.removeExtra("openTab")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun checkInstallReferrer() {
        val referrerClient = InstallReferrerClient.newBuilder(this).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val response = referrerClient.installReferrer
                            val referrerUrl = response.installReferrer
                            if (referrerUrl != null && referrerUrl.contains("moment_invite_code=")) {
                                val code = referrerUrl.split("moment_invite_code=")[1].split("&")[0]
                                authRepository.savePendingInviteCode(code)
                            }
                            referrerClient.endConnection()
                        } catch (e: Exception) {
                            Log.e("Moment", "Error getting install referrer", e)
                        }
                    }
                    else -> {}
                }
            }

            override fun onInstallReferrerServiceDisconnected() {}
        })
    }
}
