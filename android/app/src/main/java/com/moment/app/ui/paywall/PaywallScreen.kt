package com.moment.app.ui.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moment.app.util.Resource
import com.moment.app.ui.auth.AuthViewModel

val MidnightDark = Color(0xFF121212)
val RoseGoldLight = Color(0xFFE99EA5)
val RoseGoldDark = Color(0xFFC77A82)

@Composable
fun PaywallScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsState()
    
    // Automatically close when premium is unlocked
    LaunchedEffect(profileState) {
        if (profileState is Resource.Success) {
            val user = (profileState as Resource.Success).data
            if (user?.isPremium == true) {
                onBackClick()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightDark)
    ) {
        // Gradient glow at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            RoseGoldDark.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Close button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 100.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = RoseGoldLight,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Moment Plus",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock the ultimate emotional connection experience.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Feature List
            FeatureRow("Vibe Status", "Let them know how you're feeling.")
            Spacer(modifier = Modifier.height(24.dp))
            FeatureRow("Unlimited Signals", "Send unlimited love signals every day.")

            Spacer(modifier = Modifier.weight(1f))

            // Loading or Button
            if (profileState is Resource.Loading) {
                CircularProgressIndicator(color = RoseGoldLight)
            } else {
                // Call to Action
                Button(
                    onClick = { viewModel.upgradeToPremium() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoseGoldLight,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Unlock Moment Plus",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This is a mock purchase. No real money will be charged.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FeatureRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, RoseGoldLight.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = RoseGoldLight)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Text(text = description, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}
