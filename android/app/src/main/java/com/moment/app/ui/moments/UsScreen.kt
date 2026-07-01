package com.moment.app.ui.moments

import android.text.format.DateUtils
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.NoMeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moment.app.data.local.MomentEntity
import com.moment.app.data.remote.RelationshipDto
import com.moment.app.data.remote.UserDto
import com.moment.app.ui.theme.HeartRed
import com.moment.app.ui.theme.SoftCream
import com.moment.app.ui.theme.TextDeep
import com.moment.app.ui.theme.TextMuted
import com.moment.app.ui.theme.WarmBeige
import com.moment.app.ui.theme.RoseQuartz
import com.moment.app.ui.theme.White
import com.moment.app.ui.theme.ErrorSoft
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RelationshipTheme(
    val gradientColors: List<Color>,
    val pulseColor: Color,
    val textColor: Color
)

val RoseTheme = RelationshipTheme(
    gradientColors = listOf(SoftCream, RoseQuartz),
    pulseColor = HeartRed,
    textColor = TextDeep
)

@Composable
fun UsScreen(
    viewModel: UsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    
    var showUnpairDialog by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                if (showEditNameDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditNameDialog = false },
                        title = { Text("Rename Space") },
                        text = {
                            OutlinedTextField(
                                value = editNameInput,
                                onValueChange = { editNameInput = it },
                                label = { Text("Space Name") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.updateSpaceName(editNameInput)
                                    showEditNameDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel", color = TextDeep) }
                        },
                        containerColor = White
                    )
                }

                if (showUnpairDialog) {
                    AlertDialog(
                        onDismissRequest = { showUnpairDialog = false },
                        title = { Text("Close Space?") },
                        text = { Text("This will permanently unpair you from your partner and close this space. This action cannot be undone.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.unpair()
                                    showUnpairDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorSoft)
                            ) { Text("Close Space") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUnpairDialog = false }) { Text("Cancel", color = TextDeep) }
                        },
                        containerColor = White
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp) // padding for nav
                ) {
                    // 1. Header
                    item {
                        UsHeader(
                            relationship = state.relationship,
                            currentUser = state.currentUser
                        )
                        val daysTogether = try {
                            val start = java.time.Instant.parse(state.relationship.createdAt)
                            val now = java.time.Instant.now()
                            java.time.temporal.ChronoUnit.DAYS.between(start, now).coerceAtLeast(0)
                        } catch (e: Exception) {
                            0
                        }
                        val totalMoments = state.relationship.totalMoments ?: 0
                        val signalsCount = state.relationship.signalsCount ?: emptyMap()
                        val totalLittleThings = signalsCount.values.sum()
                        TogetherPills(days = daysTogether, momentsCount = totalMoments, littleThingsCount = totalLittleThings)
                    }

                    // 2. Featured Memories (Moments We Kept)
                    if (state.favorites.isNotEmpty()) {
                        item {
                            FadingDivider()
                            Text(
                                text = "Moments We Kept",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                    letterSpacing = 2.sp
                                ),
                                color = TextDeep,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.favorites) { moment ->
                                    FavoriteMemoryCard(moment = moment, onFavoriteClick = { viewModel.toggleFavorite(moment.id) })
                                }
                            }
                        }
                    }

                    // 3. Little Things
                    item {
                        FadingDivider()
                        val signalsCount = state.relationship.signalsCount ?: emptyMap()
                        LittleThingsRow(signalsCount = signalsCount)
                    }

                    // 4. Settings Sections
                    item {
                        FadingDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "RELATIONSHIP",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                            color = TextMuted.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                                .clip(RoundedCornerShape(24.dp))
                                .background(White)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                SpaceSettingItem(
                                    icon = Icons.Outlined.Edit,
                                    title = state.relationship.spaceName,
                                    onClick = {
                                        editNameInput = state.relationship.spaceName
                                        showEditNameDialog = true
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "PRIVACY & BOUNDARIES",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                            color = TextMuted.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                                .clip(RoundedCornerShape(24.dp))
                                .background(White)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                SpaceSettingItem(
                                    icon = Icons.Outlined.Pause,
                                    title = if (state.relationship.isPausedByMe) "Resume Wallpaper Updates" else "Take Space (Pause)",
                                    subtitle = if (state.relationship.isPausedByMe) "You are currently paused" else "Temporarily stop receiving moments",
                                    onClick = { viewModel.togglePause() }
                                )
                                SpaceSettingItem(
                                    icon = Icons.Outlined.NoMeetingRoom,
                                    title = "Close Space",
                                    subtitle = "Unpair from ${state.relationship.partner.displayName}",
                                    color = ErrorSoft,
                                    onClick = { showUnpairDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpaceSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    color: Color = TextDeep,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = color)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@Composable
fun UsHeader(
    relationship: RelationshipDto,
    currentUser: UserDto?,
    theme: RelationshipTheme = RoseTheme
) {
    val daysTogether = try {
        val start = Instant.parse(relationship.createdAt)
        val now = Instant.now()
        ChronoUnit.DAYS.between(start, now).coerceAtLeast(0)
    } catch (e: Exception) {
        0
    }

    val formattedDate = try {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
        Instant.parse(relationship.createdAt).atZone(ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        relationship.createdAt.take(10)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(theme.gradientColors))
            .padding(top = 32.dp, bottom = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Relationship Name (Hero)
            Text(
                text = relationship.spaceName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light, // Premium, elegant sans-serif
                color = theme.textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp)) // Reduced

            // Profile Pictures and Pulse Row (Edge-to-Edge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                ProfilePictureCircle(url = currentUser?.profilePictureUrl, size = 64.dp)
                
                PulseConnectionLine(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    color = theme.pulseColor
                )
                
                ProfilePictureCircle(url = relationship.partner.profilePictureUrl, size = 64.dp)
            }
            
            Spacer(modifier = Modifier.height(24.dp)) // Reduced
            
            // Days Together
            Text(
                text = if (daysTogether <= 0) "Our Journey Begins ✨" else "$daysTogether Days Together",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = theme.textColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Together Since
            Text(
                text = "Together Since $formattedDate",
                style = MaterialTheme.typography.labelLarge,
                color = theme.textColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PulseConnectionLine(modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing), // Slower, more elegant
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val path = Path()
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val amplitude = height / 2f

        val points = 200
        val cycles = 3f // Number of heartbeats visible at once

        for (i in 0..points) {
            val x = (i.toFloat() / points) * width
            
            // Normalized time from 0 to cycles, shifting by phase
            val t = (i.toFloat() / points) * cycles + phase
            val frac = t - kotlin.math.floor(t)

            fun lerp(start: Float, stop: Float, fraction: Float): Float {
                return (1 - fraction) * start + fraction * stop
            }

            // Elegant, subdued ECG shape
            val yOffset = when {
                frac < 0.35f -> 0f
                frac < 0.4f -> lerp(0f, -0.2f, (frac - 0.35f) / 0.05f)
                frac < 0.45f -> lerp(-0.2f, 0.8f, (frac - 0.4f) / 0.05f)
                frac < 0.5f -> lerp(0.8f, -0.4f, (frac - 0.45f) / 0.05f)
                frac < 0.55f -> lerp(-0.4f, 0f, (frac - 0.5f) / 0.05f)
                else -> 0f
            }

            // -y is up in Canvas
            val y = centerY - (yOffset * amplitude)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Apply an alpha fade at the edges so it emerges gracefully from the profile pictures
        val fadeBrush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.15f to color,
            0.85f to color,
            1f to Color.Transparent
        )

        drawPath(
            path = path,
            brush = fadeBrush,
            style = Stroke(
                width = 2.dp.toPx(), // Thinner, more elegant stroke
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@Composable
fun ProfilePictureCircle(url: String?, size: Dp = 64.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .shadow(8.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = "Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun FavoriteMemoryCard(moment: MomentEntity, onFavoriteClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(195.dp)
            .height(255.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
    ) {
        AsyncImage(
            model = moment.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp) // Premium thick white border
                .clip(RoundedCornerShape(16.dp))
        )
        
        // Dark vignette overlay at bottom to make heart icon pop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        startY = 200f
                    )
                )
        )
        
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = HeartRed,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun EmptyScrapbook() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = HeartRed.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "You are connected! ❤️",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextDeep
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the Moment button below to take your first photo and magically update their wallpaper.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun TogetherPills(days: Long, momentsCount: Int, littleThingsCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TogetherPill(icon = "❤️", text = "$days Day${if(days != 1L) "s" else ""}")
        Spacer(modifier = Modifier.width(8.dp))
        TogetherPill(icon = "✨", text = "$littleThingsCount Little Thing${if(littleThingsCount != 1) "s" else ""}")
        Spacer(modifier = Modifier.width(8.dp))
        TogetherPill(icon = "📷", text = "$momentsCount Moment${if(momentsCount != 1) "s" else ""}")
    }
}

@Composable
fun TogetherPill(icon: String, text: String) {
    Row(
        modifier = Modifier
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = TextDeep)
    }
}

@Composable
fun LittleThingsRow(signalsCount: Map<String, Int>) {
    Column {
        Text(
            text = "Little Things ❤️",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                letterSpacing = 2.sp
            ),
            color = TextDeep,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val cuddles = signalsCount["Cuddle"] ?: 0
            val kisses = signalsCount["Kiss"] ?: 0
            val missYous = signalsCount["MissYou"] ?: 0
            val thinking = signalsCount["ThinkingOfYou"] ?: 0
            val punches = signalsCount["Punch"] ?: 0

            item { LittleThingCard("💭", thinking.toString(), "Thoughts") }
            item { LittleThingCard("👊", punches.toString(), "Punches") }
            item { LittleThingCard("🧸", cuddles.toString(), "Cozy Cuddles") }
            item { LittleThingCard("😘", kisses.toString(), "Sweet Kisses") }
            item { LittleThingCard("🥺", missYous.toString(), "Miss You's") }
        }
    }
}

@Composable
fun LittleThingCard(icon: String, count: String, label: String) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = icon, fontSize = 28.sp)
        Column {
            Text(text = count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDeep)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted, lineHeight = 14.sp)
        }
    }
}

@Composable
fun FadingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 32.dp)
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, RoseQuartz, Color.Transparent)
                )
            )
    )
}
