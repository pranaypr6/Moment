package com.moment.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestNotificationPermission()
        registerDevice()
        syncPendingMoments()
        checkInstallReferrer()

        setContent {
            MomentTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(navController = navController)
                }
            }
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

    private fun syncPendingMoments() {
        lifecycleScope.launch {
            try {
                val result = momentRepository.getPendingMoments()
                result.onSuccess { moments ->
                    moments.forEach { moment ->
                        val workData = Data.Builder()
                            .putString("momentId", moment.id)
                            .putString("imageUrl", moment.imageUrl)
                            .putString("wallpaperTarget", moment.wallpaperTarget)
                            .putString("senderName", moment.sender.displayName ?: moment.sender.username)
                            .build()

                        val workRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
                            .setInputData(workData)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()

                        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                            "apply_moment_${moment.id}",
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("Moment", "Failed to sync pending moments", e)
            }
        }
    }

    private fun registerDevice() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val deviceName = Build.MODEL
            
            lifecycleScope.launch {
                try {
                    momentRepository.registerDevice(token, "ANDROID", deviceName)
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to register device to backend", e)
                }
            }
        }
    }
}
