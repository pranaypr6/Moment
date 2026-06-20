package com.moment.app.ui.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moment.app.data.local.MomentEntity
import com.moment.app.ui.theme.HeartRed
import com.moment.app.ui.theme.SoftCream
import com.moment.app.ui.theme.TextDeep
import com.moment.app.ui.theme.TextMuted
import com.moment.app.ui.theme.WarmBeige

@Composable
fun UsScreen(
    viewModel: UsViewModel = hiltViewModel(),
    onNavigateToSpaceSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(SoftCream)) {
        when (val state = uiState) {
            is UsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = HeartRed)
            }
            is UsUiState.Error -> {
                Text(state.message, color = HeartRed, modifier = Modifier.align(Alignment.Center))
            }
            is UsUiState.NotPaired -> {
                Text("Not paired yet.", modifier = Modifier.align(Alignment.Center), color = TextMuted)
            }
            is UsUiState.Success -> {
                val coverMoment = state.moments.find { it.id == state.relationship.coverMomentId }
                    ?: state.moments.firstOrNull()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        // Cover Image
                        if (coverMoment != null) {
                            AsyncImage(
                                model = coverMoment.imageUrl,
                                contentDescription = "Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                            startY = 150f
                                        )
                                    )
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(WarmBeige))
                        }

                        // Settings Gear
                        IconButton(
                            onClick = onNavigateToSpaceSettings,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Space Settings",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // Info
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Partner Photo
                                if (state.relationship.partner.profilePictureUrl != null) {
                                    AsyncImage(
                                        model = state.relationship.partner.profilePictureUrl,
                                        contentDescription = "Partner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = state.relationship.spaceName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Together Since ${state.relationship.createdAt.take(10)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp) // padding for nav
                    ) {
                        items(state.moments) { moment ->
                            ScrapbookItem(moment = moment, onFavoriteClick = { viewModel.toggleFavorite(moment.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScrapbookItem(moment: MomentEntity, onFavoriteClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray)
    ) {
        AsyncImage(
            model = moment.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
        ) {
            Icon(
                imageVector = if (moment.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (moment.isFavorite) HeartRed else Color.White
            )
        }
    }
}
