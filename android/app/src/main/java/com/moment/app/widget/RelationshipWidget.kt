package com.moment.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.delay
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.moment.app.MainActivity
import com.moment.app.worker.SendPresenceWorker
import com.moment.app.di.WidgetEntryPoint
import com.moment.app.util.TimeUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.firstOrNull
import com.moment.app.util.Resource
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import androidx.glance.Image

class RelationshipWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
        val relationshipRepository = entryPoint.relationshipRepository()
        
        val resource = relationshipRepository.relationshipState.firstOrNull()
        val relationship = (resource as? Resource.Success)?.data
        
        val authRepository = entryPoint.authRepository()
        val me = authRepository.getProfile().getOrNull()
        
        val spaceName = relationship?.spaceName ?: "❤️ Us"
        val daysTogether = TimeUtils.getDaysTogether(relationship?.pairedAt)
        
        val myBitmap = getBitmap(appContext, me?.profilePictureUrl)
        val partnerBitmap = getBitmap(appContext, relationship?.partner?.profilePictureUrl)
        val partnerName = relationship?.partner?.username ?: "Partner"

        provideContent {
            WidgetContent(spaceName, myBitmap, partnerBitmap, daysTogether, partnerName)
        }
    }

    private suspend fun getBitmap(context: Context, url: String?): Bitmap? {
        if (url == null) return null
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .transformations(CircleCropTransformation())
            .size(100)
            .build()
        val result = loader.execute(request)
        return (result.drawable as? BitmapDrawable)?.bitmap
    }

    @Composable
    private fun WidgetContent(spaceName: String, myBitmap: Bitmap?, partnerBitmap: Bitmap?, daysTogether: String, partnerName: String) {
        val prefs = currentState<Preferences>()
        val sendStatus = prefs[stringPreferencesKey("send_status")] ?: "IDLE"
        
        val intent = Intent(androidx.glance.LocalContext.current, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(com.moment.app.R.drawable.widget_background))
                .padding(16.dp)
                .clickable(actionStartActivity(intent))
        ) {
            // Background scattered hearts
            Column(modifier = GlanceModifier.fillMaxSize()) {
                // Top row of hearts
                Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(provider = ImageProvider(com.moment.app.R.drawable.ic_heart_bg), contentDescription = null, modifier = GlanceModifier.size(20.dp))
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(provider = ImageProvider(com.moment.app.R.drawable.ic_heart_bg), contentDescription = null, modifier = GlanceModifier.size(28.dp).padding(end = 32.dp))
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
                
                // Middle row of hearts
                Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 56.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(provider = ImageProvider(com.moment.app.R.drawable.ic_heart_bg), contentDescription = null, modifier = GlanceModifier.size(16.dp))
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(provider = ImageProvider(com.moment.app.R.drawable.ic_heart_bg), contentDescription = null, modifier = GlanceModifier.size(22.dp))
                }
                
                Spacer(modifier = GlanceModifier.defaultWeight())
                // Bottom row of hearts
                Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 32.dp, bottom = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(provider = ImageProvider(com.moment.app.R.drawable.ic_heart_bg), contentDescription = null, modifier = GlanceModifier.size(32.dp))
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Image(provider = ImageProvider(com.moment.app.R.drawable.ic_heart_bg), contentDescription = null, modifier = GlanceModifier.size(24.dp))
                }
            }

            Column(
                modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (myBitmap != null) {
                        Image(
                            provider = ImageProvider(myBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.size(40.dp)
                        )
                    } else {
                        Text(text = "🧑", style = TextStyle(fontSize = 24.sp))
                    }
                    
                    Image(
                        provider = ImageProvider(com.moment.app.R.drawable.ic_pulse_line),
                        contentDescription = "Pulse Rate",
                        modifier = GlanceModifier.width(60.dp).height(24.dp).padding(horizontal = 4.dp)
                    )
                    
                    if (partnerBitmap != null) {
                        Image(
                            provider = ImageProvider(partnerBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier.size(40.dp)
                        )
                    } else {
                        Text(text = "🧑", style = TextStyle(fontSize = 24.sp))
                    }
                }
                
                Text(
                    text = daysTogether,
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(Color(0xFF2D2D2D), Color(0xFF2D2D2D)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = GlanceModifier
                        .background(ImageProvider(com.moment.app.R.drawable.widget_button_bg))
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .clickable(actionRunCallback<SendPresenceActionCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (sendStatus == "SUCCESS") "Sent to $partnerName 💕" else "💕 Send Love",
                        style = TextStyle(
                            color = androidx.glance.color.ColorProvider(Color.White, Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class SendPresenceActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val workRequest = OneTimeWorkRequestBuilder<SendPresenceWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
        
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("send_status")] = "SUCCESS"
        }
        RelationshipWidget().update(context, glanceId)
        
        delay(2000)
        
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("send_status")] = "IDLE"
        }
        RelationshipWidget().update(context, glanceId)
    }
}
