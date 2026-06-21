package com.moment.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.moment.app.MainActivity
import com.moment.app.di.WidgetEntryPoint
import com.moment.app.util.Resource
import com.moment.app.util.TimeUtils
import com.moment.app.worker.SendPresenceWorker
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

class RelationshipWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
        val relationshipRepository = entryPoint.relationshipRepository()
        
        val resource = relationshipRepository.relationshipState.firstOrNull()
        val relationship = (resource as? Resource.Success)?.data
        
        val authRepository = entryPoint.authRepository()
        val me = authRepository.getProfile().getOrNull()
        
        var daysTogether = TimeUtils.getDaysTogether(relationship?.pairedAt)
        if (!daysTogether.contains("Journey") && !daysTogether.contains("❤️")) {
            daysTogether += " ❤️"
        }
        
        val myBitmap = getBitmap(appContext, me?.profilePictureUrl)
        val partnerBitmap = getBitmap(appContext, relationship?.partner?.profilePictureUrl)
        val partnerName = relationship?.partner?.displayName ?: "Partner"

        provideContent {
            WidgetContent(myBitmap, partnerBitmap, daysTogether, partnerName)
        }
    }

    private suspend fun getBitmap(context: Context, url: String?): Bitmap? {
        if (url == null) return null
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .transformations(CircleCropTransformation())
            .size(120)
            .build()
        val result = loader.execute(request)
        return (result.drawable as? BitmapDrawable)?.bitmap
    }

    @Composable
    private fun WidgetContent(myBitmap: Bitmap?, partnerBitmap: Bitmap?, daysTogether: String, partnerName: String) {
        val prefs = currentState<Preferences>()
        val sendStatus = prefs[stringPreferencesKey("send_status")] ?: "IDLE"
        val lastAction = prefs[stringPreferencesKey("last_action")] ?: "ThinkingOfYou"
        
        val intent = Intent(androidx.glance.LocalContext.current, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(com.moment.app.R.drawable.widget_background))
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Clickable header region opens the app
                Column(
                    modifier = GlanceModifier.clickable(actionStartActivity(intent)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Pictures and Heart
                    Row(
                        modifier = GlanceModifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (myBitmap != null) {
                            Image(provider = ImageProvider(myBitmap), contentDescription = null, modifier = GlanceModifier.size(48.dp))
                        } else {
                            Text(text = "🧑", style = TextStyle(fontSize = 32.sp))
                        }
                        
                        Image(
                            provider = ImageProvider(com.moment.app.R.drawable.ic_pulse_line),
                            contentDescription = "Pulse Rate",
                            modifier = GlanceModifier.width(60.dp).height(24.dp).padding(horizontal = 4.dp)
                        )
                        
                        if (partnerBitmap != null) {
                            Image(provider = ImageProvider(partnerBitmap), contentDescription = null, modifier = GlanceModifier.size(48.dp))
                        } else {
                            Text(text = "🧑", style = TextStyle(fontSize = 32.sp))
                        }
                    }
                    
                    Text(
                        text = daysTogether,
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(Color(0xFF2D2D2D), Color(0xFF2D2D2D)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.padding(bottom = 24.dp)
                    )
                }

                if (sendStatus == "SUCCESS") {
                    // Show success feedback
                    val msg = when (lastAction) {
                        "ThinkingOfYou" -> "Sent to $partnerName 💕"
                        "Hug" -> "Hug sent 🤗"
                        "Kiss" -> "Kiss sent 😘"
                        "Rose" -> "Rose sent 🌹"
                        "PlayfulPunch" -> "Playfully punched 👊"
                        else -> "Sent to $partnerName 💕"
                    }
                    Box(modifier = GlanceModifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = msg,
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(Color(0xFFE91E63), Color(0xFFE91E63)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                } else {
                    // Actions Grid
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EmojiButton("🤗", "Hug")
                            Spacer(modifier = GlanceModifier.width(32.dp))
                            EmojiButton("🌹", "Rose")
                            Spacer(modifier = GlanceModifier.width(32.dp))
                            EmojiButton("😘", "Kiss")
                        }
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EmojiButton("💕", "ThinkingOfYou")
                            Spacer(modifier = GlanceModifier.width(48.dp))
                            EmojiButton("👊", "PlayfulPunch")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmojiButton(emoji: String, actionType: String) {
        val actionKey = ActionParameters.Key<String>("presenceType")
        Text(
            text = emoji,
            style = TextStyle(fontSize = 28.sp),
            modifier = GlanceModifier
                .clickable(actionRunCallback<SendPresenceActionCallback>(actionParametersOf(actionKey to actionType)))
                .padding(8.dp)
        )
    }
}

class SendPresenceActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val presenceType = parameters[ActionParameters.Key<String>("presenceType")] ?: "ThinkingOfYou"
        
        val workData = Data.Builder().putString("presenceType", presenceType).build()
        val workRequest = OneTimeWorkRequestBuilder<SendPresenceWorker>()
            .setInputData(workData)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
        
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("send_status")] = "SUCCESS"
            prefs[stringPreferencesKey("last_action")] = presenceType
        }
        RelationshipWidget().update(context, glanceId)
        
        delay(1500) // 1.5 seconds success state
        
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("send_status")] = "IDLE"
        }
        RelationshipWidget().update(context, glanceId)
    }
}
