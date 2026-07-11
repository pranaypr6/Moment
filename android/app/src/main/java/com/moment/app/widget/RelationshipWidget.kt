package com.moment.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.text.TextAlign
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.moment.app.MainActivity
import com.moment.app.worker.SendPresenceWorker
import com.moment.app.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.firstOrNull
import com.moment.app.util.Resource
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import androidx.glance.Image
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class RelationshipWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
        val relationshipRepository = entryPoint.relationshipRepository()
        
        val resource = relationshipRepository.relationshipState.firstOrNull()
        val relationship = (resource as? Resource.Success)?.data
        
        val authRepository = entryPoint.authRepository()
        val me = authRepository.getProfile().getOrNull()
        val subtitleStr = getSubtitleDate(relationship?.pairedAt)
        
        // Use completely circular images for the editorial look
        val myBitmap = getBitmap(appContext, me?.profilePictureUrl)
        val partnerBitmap = getBitmap(appContext, relationship?.partner?.profilePictureUrl)

        provideContent {
            WidgetContent(myBitmap, partnerBitmap, me?.currentVibe, relationship?.partner?.currentVibe, relationship?.pairedAt, subtitleStr)
        }
    }
    
    private fun getDaysTogetherCustom(isoTimestamp: String?, sendStatus: String, lastAction: String): String {
        if (sendStatus == "SUCCESS") {
            return when (lastAction) {
                "ThinkingOfYou" -> "💭 You're distracting me."
                "Punch" -> "👊 Consider yourself punched."
                "Cuddle" -> "🧸 Cuddle deployed."
                "Kiss" -> "😘 Incoming smooch."
                "MissYou" -> "🥺 I miss you too."
                else -> "❤️ Sent with ridiculous amounts of love."
            }
        }

        if (isoTimestamp == null) return "Our journey begins."
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(isoTimestamp) ?: return "Our journey begins."
            val diff = System.currentTimeMillis() - date.time
            val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff).toInt()
            
            val milestones = setOf(30, 50, 100, 150, 200, 300, 365, 500, 730, 1000)
            
            if (days <= 0) {
                "Our journey begins."
            } else if (days == 1) {
                "1 day, still us."
            } else if (days in milestones || days % 100 == 0) {
                "$days days, still us."
            } else {
                val phrases = listOf("still us.", "just us.", "always us.", "forever us.")
                val index = days % phrases.size
                phrases[index]
            }
        } catch (e: Exception) {
            "Our journey begins."
        }
    }
    
    private fun getSubtitleDate(isoTimestamp: String?): String {
        val baseText = if (isoTimestamp == null) {
            "TODAY"
        } else {
            try {
                val parseSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                parseSdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = parseSdf.parse(isoTimestamp) ?: return "TODAY"
                
                val outSdf = SimpleDateFormat("MMMM d '•' yyyy", Locale.US)
                outSdf.timeZone = TimeZone.getDefault()
                "SINCE ${outSdf.format(date).uppercase(Locale.US)}"
            } catch (e: Exception) {
                "TODAY"
            }
        }
        
        // Add thin spaces between characters for elegant letter spacing
        return baseText.map { it.toString() }.joinToString("\u2009")
    }

    private suspend fun getBitmap(context: Context, url: String?): Bitmap? {
        if (url == null) return null
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .transformations(CircleCropTransformation()) // Fully circular
            .size(160)
            .build()
        val result = loader.execute(request)
        return (result.drawable as? BitmapDrawable)?.bitmap
    }

    @Composable
    private fun WidgetContent(myBitmap: Bitmap?, partnerBitmap: Bitmap?, myVibe: String?, partnerVibe: String?, pairedAt: String?, subtitle: String) {
        val prefs = currentState<Preferences>()
        val sendStatus = prefs[stringPreferencesKey("send_status")] ?: "IDLE"
        val lastAction = prefs[stringPreferencesKey("last_action")] ?: ""
        
        val intent = Intent(androidx.glance.LocalContext.current, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        if (sendStatus == "SUCCESS" || sendStatus == "SENDING") {
            val actionMessage = if (sendStatus == "SENDING") "Sending..." else getDaysTogetherCustom(pairedAt, sendStatus, lastAction)
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(androidx.glance.color.ColorProvider(Color(0xFFFFFAFA), Color(0xFFFFFAFA)))
                    .clickable(actionStartActivity(intent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionMessage,
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(Color(0xFFE99EA5), Color(0xFFE99EA5)),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                )
            }
            return
        }
        
        val daysTogether = getDaysTogetherCustom(pairedAt, sendStatus, lastAction)
        
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(com.moment.app.R.drawable.widget_background))
                // Compressed vertical padding to prevent clipping
                .padding(top = 16.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)
                .clickable(actionStartActivity(intent))
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Row: Circular Profile Pictures + Long Embedded Heartbeat Center
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialProfileImage(myBitmap, myVibe)
                    
                    // Heart Divider (Embedded style, longer lines)
                    Row(
                        modifier = GlanceModifier.padding(horizontal = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = GlanceModifier.width(76.dp).height(1.dp).background(androidx.glance.color.ColorProvider(Color(0xFFD7CEC8), Color(0xFFD7CEC8)))) {}
                        Text(
                            text = "♥",
                            style = TextStyle(
                                color = androidx.glance.color.ColorProvider(Color(0xFFE99EA5), Color(0xFFE99EA5)),
                                fontSize = 12.sp
                            ),
                            modifier = GlanceModifier.padding(horizontal = 2.dp)
                        )
                        Box(modifier = GlanceModifier.width(76.dp).height(1.dp).background(androidx.glance.color.ColorProvider(Color(0xFFD7CEC8), Color(0xFFD7CEC8)))) {}
                    }
                    
                    EditorialProfileImage(partnerBitmap, partnerVibe)
                }
                
                // Hero Text
                Text(
                    text = daysTogether,
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(Color(0xFF1F1A18), Color(0xFF1F1A18)),
                        fontSize = 24.sp, // Reduced by ~8% to increase elegance
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier.padding(bottom = 2.dp)
                )
                
                // Subtitle Text (Spaced Uppercase)
                Text(
                    text = subtitle,
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(Color(0xFF8B847C), Color(0xFF8B847C)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier.padding(bottom = 12.dp) // Perfect gap before reactions
                )

                // Quick Affections Grid (Single Row, perfectly spaced)
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EmojiButton(com.moment.app.R.drawable.ic_thought_bubble, "ThinkingOfYou", lastAction, sendStatus)
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    EmojiButton(com.moment.app.R.drawable.ic_punch_forward, "Punch", lastAction, sendStatus)
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    EmojiButton(com.moment.app.R.drawable.ic_cuddling_teddies, "Cuddle", lastAction, sendStatus)
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    EmojiButton(com.moment.app.R.drawable.ic_kiss_face, "Kiss", lastAction, sendStatus)
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    EmojiButton(com.moment.app.R.drawable.ic_pleading_face, "MissYou", lastAction, sendStatus)
                }
            }
        }
    }

    @Composable
    private fun EditorialProfileImage(bitmap: Bitmap?, vibe: String?) {
        Box(
            modifier = GlanceModifier.size(56.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = GlanceModifier
                    .size(52.dp)
                    .background(ImageProvider(com.moment.app.R.drawable.widget_profile_shadow))
            ) {
                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.size(52.dp)
                    )
                } else {
                    Box(
                        modifier = GlanceModifier.size(52.dp).background(androidx.glance.color.ColorProvider(Color(0xFFEFECE9), Color(0xFFEFECE9))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🧑", style = TextStyle(fontSize = 22.sp))
                    }
                }
            }
            if (vibe != null) {
                Box(
                    modifier = GlanceModifier
                        .size(18.dp)
                        .background(androidx.glance.color.ColorProvider(Color.White, Color.White))
                        .cornerRadius(9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = vibe, style = TextStyle(fontSize = 12.sp))
                }
            }
        }
    }

    @Composable
    private fun EmojiButton(iconResId: Int, actionType: String, lastAction: String, sendStatus: String) {
        val actionKey = ActionParameters.Key<String>("presenceType")
        val isActive = (actionType == lastAction && sendStatus == "SUCCESS")
        
        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .clickable(actionRunCallback<SendPresenceActionCallback>(actionParametersOf(actionKey to actionType))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(iconResId),
                contentDescription = actionType,
                modifier = GlanceModifier.size(if (isActive) 40.dp else 36.dp)
            )
        }
    }
}

class SendPresenceActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val presenceType = parameters[ActionParameters.Key<String>("presenceType")] ?: "ThinkingOfYou"
        
        // 1. Immediately update UI to sending state
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("send_status")] = "SENDING"
            prefs[stringPreferencesKey("last_action")] = presenceType
        }
        RelationshipWidget().update(context, glanceId)
        
        // 2. Dispatch network request directly
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(appContext, WidgetEntryPoint::class.java)
        val relationshipRepository = entryPoint.relationshipRepository()
        val api = entryPoint.momentApi()
        
        val typeInt = when (presenceType) {
            "ThinkingOfYou" -> 0
            "Punch" -> 1
            "Cuddle" -> 2
            "Kiss" -> 3
            "MissYou" -> 4
            else -> 0
        }
        
        var relData = relationshipRepository.relationshipState.firstOrNull()?.data
        if (relData == null) {
            relationshipRepository.refreshCurrentRelationship()
            relData = relationshipRepository.relationshipState.firstOrNull()?.data
        }
        
        if (relData != null) {
            try {
                val request = com.moment.app.data.remote.SendPresenceRequest(relData.id, typeInt)
                api.sendPresenceSignal(request)
            } catch (e: Exception) {
                // If direct network call fails, enqueue WorkManager as fallback
                val workRequest = OneTimeWorkRequestBuilder<SendPresenceWorker>()
                    .setInputData(androidx.work.Data.Builder().putString("presenceType", presenceType).build())
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
        
        // 3. Update UI to SUCCESS state
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("send_status")] = "SUCCESS"
        }
        RelationshipWidget().update(context, glanceId)
        
        // 4. Keep active state for 1200ms
        delay(1200)
        
        // 5. Return to idle
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[stringPreferencesKey("send_status")] = "IDLE"
            prefs[stringPreferencesKey("last_action")] = ""
        }
        RelationshipWidget().update(context, glanceId)
    }
}
