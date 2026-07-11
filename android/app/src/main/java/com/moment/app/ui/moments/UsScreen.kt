package com.moment.app.ui.moments

import android.text.format.DateUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
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
import com.moment.app.ui.theme.DeepMauve
import com.moment.app.ui.theme.SoftRose
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
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

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
    viewModel: UsViewModel = hiltViewModel(),
    authViewModel: com.moment.app.ui.auth.AuthViewModel = hiltViewModel(),
    onNavigateToPaywall: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.currentUser.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    
    var showUnpairDialog by remember { mutableStateOf(false) }
    var showVibeModal by remember { mutableStateOf(false) }
    
    
    Box(modifier = Modifier.fillMaxSize().background(SoftCream)) {
        when (val state = uiState) {
            is UsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = HeartRed)
            }
            is UsUiState.Error -> {
                Text(state.message, color = HeartRed, modifier = Modifier.align(Alignment.Center))
            }
            is UsUiState.NotPaired -> {
                Text("Waiting for your partner...", modifier = Modifier.align(Alignment.Center), color = TextMuted)
            }
            is UsUiState.Success -> {
                if (showEditNameDialog) {
                    AlertDialog(
                        onDismissRequest = { showEditNameDialog = false },
                        title = { Text("Name Our World") },
                        text = {
                            OutlinedTextField(
                                value = editNameInput,
                                onValueChange = { editNameInput = it },
                                label = { Text("What do we call our world?") },
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
                        title = { Text("Say Goodbye (Unpair)?") },
                        text = { Text("This will disconnect our worlds. You will no longer receive moments from your partner. This cannot be undone 💔") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.unpair()
                                    showUnpairDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorSoft)
                            ) { Text("Say Goodbye") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUnpairDialog = false }) { Text("Cancel", color = TextDeep) }
                        },
                        containerColor = White
                    )
                }
                
                if (showVibeModal) {
                    VibeSelectorModal(
                        currentVibe = ((authState as? com.moment.app.util.Resource.Success)?.data ?: state.currentUser)?.currentVibe,
                        onDismiss = { showVibeModal = false },
                        onVibeSelected = { emoji ->
                            authViewModel.updateVibe(emoji)
                            showVibeModal = false
                        }
                    )
                }

                val actualCurrentUser = (authState as? com.moment.app.util.Resource.Success)?.data ?: state.currentUser

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp) // padding for nav
                ) {
                    // 1. Header
                    item {
                        UsHeader(
                            relationship = state.relationship,
                            currentUser = actualCurrentUser,
                            onSetVibeClick = { 
                                if (actualCurrentUser?.isPremium == true) {
                                    showVibeModal = true 
                                } else {
                                    onNavigateToPaywall()
                                }
                            }
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



                    // 3. Little Things
                    item {
                        FadingDivider()
                        val signalsCount = state.relationship.signalsCount ?: emptyMap()
                        LittleThingsRow(signalsCount = signalsCount)
                    }

                    // 4. Daily Memory (Premium) removed for now
                    // 5. Settings Sections
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
                                    title = if (state.relationship.isPausedByMe) "Reconnect Space" else "Take Space",
                                    subtitle = if (state.relationship.isPausedByMe) "You are currently taking space" else "Temporarily pause sharing moments",
                                    onClick = { viewModel.togglePause() }
                                )
                                SpaceSettingItem(
                                    icon = Icons.Outlined.NoMeetingRoom,
                                    title = "Say Goodbye (Unpair)",
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

// --- Data class for warmth particles ---
private data class WarmthParticle(
    val id: Int,
    val startX: Float,  // 0..1 normalized
    val size: Float,    // dp value
    val speed: Float,   // duration multiplier
    val delay: Int,     // start delay ms
    val color: Color
)

@Composable
fun UsHeader(
    relationship: RelationshipDto,
    currentUser: UserDto?,
    onSetVibeClick: () -> Unit = {}
) {
    val formattedDate = try {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
        Instant.parse(relationship.createdAt).atZone(ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        relationship.createdAt.take(10)
    }

    // --- Aurora mesh: 3 independent breathing blobs ---
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val breathe1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "blob1"
    )
    val breathe2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "blob2"
    )
    val breathe3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "blob3"
    )

    // --- Overlap glow pulse ---
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f, targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow_pulse"
    )

    // --- Entrance animation ---
    var entered by remember { mutableStateOf(false) }
    val driftOffset by animateDpAsState(
        targetValue = if (entered) 0.dp else 16.dp,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "drift"
    )
    val nameAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "name_alpha"
    )
    val sinceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "since_alpha"
    )
    LaunchedEffect(Unit) { entered = true }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val c1 = SoftCream
                val c2 = RoseQuartz

                // Base vertical gradient
                drawRect(Brush.verticalGradient(listOf(c1, c2)))

                // Blob 1: top-left, RoseQuartz warmth
                val r1 = size.maxDimension * (0.5f + breathe1 * 0.2f)
                val center1 = Offset(size.width * 0.15f, size.height * 0.15f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(RoseQuartz.copy(alpha = 0.5f), Color.Transparent),
                        center = center1, radius = r1
                    ),
                    center = center1, radius = r1
                )

                // Blob 2: bottom-right, HeartRed whisper
                val r2 = size.maxDimension * (0.4f + breathe2 * 0.15f)
                val center2 = Offset(size.width * 0.85f, size.height * 0.8f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(HeartRed.copy(alpha = 0.12f), Color.Transparent),
                        center = center2, radius = r2
                    ),
                    center = center2, radius = r2
                )

                // Blob 3: center, DeepMauve subtle depth
                val r3 = size.maxDimension * (0.35f + breathe3 * 0.1f)
                val center3 = Offset(size.width * 0.5f, size.height * 0.5f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(DeepMauve.copy(alpha = 0.06f), Color.Transparent),
                        center = center3, radius = r3
                    ),
                    center = center3, radius = r3
                )
            }
            .padding(top = 48.dp, bottom = 40.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Overlapping profile pictures with warm glow ---
            Box(
                modifier = Modifier
                    .height(110.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Floating warmth particles behind the photos
                FloatingWarmthParticles(
                    modifier = Modifier
                        .width(160.dp)
                        .height(110.dp)
                )

                // Warm glow at the overlap center
                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                HeartRed.copy(alpha = glowAlpha),
                                SoftRose.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.6f
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.6f
                    )
                }

                // You (right) — drifts in from the right
                Box(modifier = Modifier.offset(x = 20.dp + driftOffset)) {
                    ProfilePictureCircle(
                        url = currentUser?.profilePictureUrl,
                        size = 80.dp,
                        modifier = Modifier
                            .shadow(12.dp, CircleShape, ambientColor = HeartRed.copy(alpha = 0.15f))
                            .border(3.dp, Color.White, CircleShape)
                    )
                    if (currentUser?.currentVibe != null) {
                        VibeBadge(
                            emoji = currentUser.currentVibe, 
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                        )
                    }
                }

                // Partner (left) — drifts in from the left, drawn last so it's on top
                Box(modifier = Modifier.offset(x = -(20.dp) - driftOffset)) {
                    ProfilePictureCircle(
                        url = relationship.partner.profilePictureUrl,
                        size = 80.dp,
                        modifier = Modifier
                            .shadow(12.dp, CircleShape, ambientColor = HeartRed.copy(alpha = 0.15f))
                            .border(3.dp, Color.White, CircleShape)
                    )
                    if (relationship.partner.currentVibe != null) {
                        VibeBadge(
                            emoji = relationship.partner.currentVibe, 
                            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-2).dp, y = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- Space name: bold, confident, serif ---
            Text(
                text = relationship.spaceName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                lineHeight = 40.sp,
                color = TextDeep.copy(alpha = nameAlpha),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- Premium Feature: Set Vibe ---
            Surface(
                color = White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSetVibeClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set your vibe", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    if (currentUser?.isPremium != true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Outlined.Lock, contentDescription = "Premium", tint = HeartRed.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- "together since" inline with fading lines ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    HeartRed.copy(alpha = 0.25f * sinceAlpha)
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "together since $formattedDate".lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDeep.copy(alpha = 0.5f * sinceAlpha),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    HeartRed.copy(alpha = 0.25f * sinceAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

// --- Floating warmth particles that drift upward from the overlap zone ---
@Composable
fun FloatingWarmthParticles(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = listOf(
            HeartRed.copy(alpha = 0.35f),
            RoseQuartz.copy(alpha = 0.4f),
            SoftRose.copy(alpha = 0.3f),
            HeartRed.copy(alpha = 0.2f),
            RoseQuartz.copy(alpha = 0.3f),
            SoftRose.copy(alpha = 0.25f),
            HeartRed.copy(alpha = 0.15f)
        )
        List(7) { i ->
            WarmthParticle(
                id = i,
                startX = 0.3f + Random.nextFloat() * 0.4f,  // cluster around center
                size = 2f + Random.nextFloat() * 3f,
                speed = 0.8f + Random.nextFloat() * 0.6f,
                delay = (Random.nextFloat() * 3000).toInt(),
                color = colors[i % colors.size]
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // Animate each particle's Y position (0 = bottom, 1 = top)
    val particlePhases = particles.map { p ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (4000 * p.speed).toInt(),
                    delayMillis = p.delay,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "particle_${p.id}"
        )
    }

    Canvas(modifier = modifier) {
        particles.forEachIndexed { index, particle ->
            val phase = particlePhases[index].value
            val x = size.width * particle.startX + sin(phase * 6.28f) * 8.dp.toPx()
            val y = size.height * (1f - phase)
            // Fade in at bottom, fade out at top
            val alpha = when {
                phase < 0.15f -> phase / 0.15f
                phase > 0.8f -> (1f - phase) / 0.2f
                else -> 1f
            }
            drawCircle(
                color = particle.color.copy(alpha = particle.color.alpha * alpha),
                radius = particle.size.dp.toPx() / 2f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun ProfilePictureCircle(url: String?, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
fun VibeBadge(emoji: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.1f)),
        shape = CircleShape,
        color = White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftCream)
    ) {
        Text(
            text = emoji,
            fontSize = 14.sp,
            modifier = Modifier.padding(6.dp)
        )
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
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Little Things ✨",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                letterSpacing = 2.sp
            ),
            color = TextDeep,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp)
        )

        val totalSignals = signalsCount.values.sum()

        if (totalSignals == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = HeartRed.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No little things yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDeep
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send a thought or a cuddle to brighten their day!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val cuddles = signalsCount["Cuddle"] ?: 0
            val kisses = signalsCount["Kiss"] ?: 0
            val missYous = signalsCount["MissYou"] ?: 0
            val thinking = signalsCount["ThinkingOfYou"] ?: 0
            val punches = signalsCount["Punch"] ?: 0

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LittleThingCard(com.moment.app.R.drawable.ic_thought_bubble, thinking.toString(), "Thoughts", SoftCream, Modifier.weight(1f))
                LittleThingCard(com.moment.app.R.drawable.ic_cuddling_teddies, cuddles.toString(), "Cuddles", RoseQuartz, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LittleThingCard(com.moment.app.R.drawable.ic_kiss_face, kisses.toString(), "Kisses", WarmBeige, Modifier.weight(1f))
                LittleThingCard(com.moment.app.R.drawable.ic_punch_forward, punches.toString(), "Punches", SoftCream, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LittleThingCard(com.moment.app.R.drawable.ic_pleading_face, missYous.toString(), "Miss You's", SoftRose, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun LittleThingCard(drawableRes: Int, count: String, label: String, bgColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Transparent)
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = drawableRes),
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDeep)
            Text(text = label, fontSize = 13.sp, color = TextDeep.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeSelectorModal(
    currentVibe: String?,
    onDismiss: () -> Unit,
    onVibeSelected: (String) -> Unit
) {
    val vibeOptions = listOf("💻", "💤", "🚗", "🥺", "🎮", "🍽️", "🎧", "💪", "🏃‍♂️", "☕", "📖", "✨")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SoftCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Set your vibe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDeep)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Let your partner know what you're up to.", color = TextMuted)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                items(vibeOptions.size) { index ->
                    val emoji = vibeOptions[index]
                    val isSelected = currentVibe == emoji
                    
                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable { onVibeSelected(emoji) },
                        shape = CircleShape,
                        color = if (isSelected) HeartRed.copy(alpha = 0.1f) else White,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, HeartRed) else androidx.compose.foundation.BorderStroke(1.dp, WarmBeige)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(emoji, fontSize = 28.sp)
                        }
                    }
                }
            }
        }
    }
}
