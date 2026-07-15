package com.moment.app.ui.paywall

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

val MidnightDark = Color(0xFF121212)
val RoseGoldLight = Color(0xFFE99EA5)
val RoseGoldDark = Color(0xFFC77A82)

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val packages by viewModel.packages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val purchaseSuccess by viewModel.purchaseSuccess.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity
    
    // Automatically close when premium is unlocked
    LaunchedEffect(purchaseSuccess) {
        if (purchaseSuccess) {
            onBackClick()
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

            Spacer(modifier = Modifier.height(32.dp))

            // Feature List
            FeatureRow("Vibe Status", "Let them know how you're feeling.")
            Spacer(modifier = Modifier.height(16.dp))
            FeatureRow("Unlimited Signals", "Send unlimited love signals every day.")

            Spacer(modifier = Modifier.weight(1f))

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Loading or Packages
            if (isLoading) {
                CircularProgressIndicator(color = RoseGoldLight)
            } else {
                if (packages.isEmpty()) {
                    Text(
                        text = "No packages available right now.",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    packages.forEach { pkg ->
                        Button(
                            onClick = { 
                                activity?.let { viewModel.purchasePackage(it, pkg) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(30.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoseGoldLight,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Subscribe to ${pkg.product.title} - ${pkg.product.price.formatted}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Subscriptions auto-renew until canceled.",
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
