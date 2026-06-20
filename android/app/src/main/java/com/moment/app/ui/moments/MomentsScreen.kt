package com.moment.app.ui.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moment.app.ui.theme.HeartRed
import com.moment.app.ui.theme.TextDeep
import com.moment.app.ui.theme.TextMuted
import com.moment.app.ui.theme.WarmBeige

@Composable
fun MomentsScreen(
    viewModel: MomentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(WarmBeige.copy(alpha = 0.2f))) {
        when (val state = uiState) {
            is MomentsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = HeartRed)
            }
            is MomentsUiState.Error -> {
                Text(state.message, color = HeartRed, modifier = Modifier.align(Alignment.Center))
            }
            is MomentsUiState.NotPaired -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("You are not paired yet.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDeep)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Go to Settings to pair with your partner.", color = TextMuted)
                }
            }
            is MomentsUiState.Success -> {
                if (state.latestMoment != null) {
                    AsyncImage(
                        model = state.latestMoment.imageUrl,
                        contentDescription = "Latest Moment",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Overlay Gradient or text
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                            .padding(bottom = 120.dp) // Avoid dock and send button
                    ) {
                        val isMine = state.latestMoment.creatorId != state.partnerId
                        val locationText = if (isMine) {
                            "Currently on ${state.partnerName}'s screen"
                        } else {
                            "Currently on your screen"
                        }
                        
                        Text(
                            text = locationText,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.latestMoment.note != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.latestMoment.note,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (state.isPausedByPartner) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Pause, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Partner is paused", color = Color.White)
                            }
                        }
                    }

                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = HeartRed, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No moments yet.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDeep)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Send a moment to ${state.partnerName} to get started.", color = TextMuted)
                    }
                }
            }
        }
    }
}
