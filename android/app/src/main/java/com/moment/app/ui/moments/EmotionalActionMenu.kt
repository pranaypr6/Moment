package com.moment.app.ui.moments

import androidx.compose.material3.MaterialTheme

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.moment.app.ui.theme.HeartRed
import kotlin.math.cos
import kotlin.math.sin

enum class EmotionalAction(val actionName: String, val emoji: String) {
    ThinkingOfYou("ThinkingOfYou", "❤️"),
    Poke("Poke", "👊"),
    Hug("Hug", "🤗"),
    Kiss("Kiss", "😘"),
    MissYou("MissYou", "🌹")
}

@Composable
fun EmotionalActionMenu(
    onActionSelected: (EmotionalAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val view = LocalView.current
    
    val actions = EmotionalAction.values()
    val radius = 120f // pixels to offset radially

    // Animation for the main fab rotation
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(300.dp), // Provide enough space for radial expansion
        contentAlignment = Alignment.BottomEnd
    ) {
        // Radial Items
        actions.forEachIndexed { index, action ->
            val angle = (Math.PI / 2) * (index.toFloat() / (actions.size - 1)) // 0 to 90 degrees (quarter circle)
            // Since it's BottomEnd, we want to expand Up and Left.
            // X goes left (negative), Y goes up (negative)
            val targetX = -(radius * cos(angle)).toFloat()
            val targetY = -(radius * sin(angle)).toFloat()

            val animatedX by animateFloatAsState(
                targetValue = if (expanded) targetX else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            val animatedY by animateFloatAsState(
                targetValue = if (expanded) targetY else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            val alpha by animateFloatAsState(
                targetValue = if (expanded) 1f else 0f,
                animationSpec = tween(if (expanded) 300 else 150)
            )

            if (alpha > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(animatedX.toInt(), animatedY.toInt()) }
                        .size(48.dp)
                        .scale(alpha)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            expanded = false
                            onActionSelected(action)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = action.emoji, style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        // Main FAB
        Surface(
            modifier = Modifier
                .size(56.dp)
                .rotate(rotation)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    expanded = !expanded
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Quick Actions",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).rotate(-45f) // Normally looks like a + when rotation is 0
                )
            }
        }
    }
}
