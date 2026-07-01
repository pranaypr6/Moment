package com.moment.app.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun EmotionalOverlay(interactionType: String) {
    var isVisible by remember { mutableStateOf(true) }
    
    val emoji = when (interactionType) {
        "ThinkingOfYou" -> "💭"
        "Punch" -> "👊"
        "Cuddle" -> "🧸"
        "Kiss" -> "😘"
        "MissYou", "Rose" -> "🥺"
        else -> "❤️"
    }

    if (isVisible) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        
        LaunchedEffect(Unit) {
            delay(1200)
            isVisible = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 120.sp,
                modifier = Modifier.scale(scale)
            )
        }
    }
}
