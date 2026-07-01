package com.moment.app.ui.moments

import android.text.format.DateUtils
import com.moment.app.util.TimeUtils
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.moment.app.util.HapticFeedbackManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlin.math.sin
import kotlin.math.abs
import com.moment.app.R
import com.moment.app.data.local.MomentEntity
import com.moment.app.ui.theme.HeartRed
import com.moment.app.ui.theme.TextDeep
import com.moment.app.ui.theme.TextMuted
import com.moment.app.ui.theme.WarmBeige
import com.moment.app.ui.theme.SoftCream

@Composable
fun MomentsScreen(
    viewModel: MomentsViewModel = hiltViewModel(),
    onSendMoment: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionSuccessState by viewModel.actionSuccessState.collectAsState()
    val listState = rememberLazyListState()
    var selectedMoment by remember { mutableStateOf<MomentEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(SoftCream)) {
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 160.dp, top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // HERO MOMENT
                    item {
                        if (state.latestMoment != null) {
                            ImmersiveHeroMoment(
                                moment = state.latestMoment,
                                isPaused = state.isPausedByPartner,
                                partnerId = state.partnerId,
                                partnerName = state.partnerName,
                                onClick = { selectedMoment = state.latestMoment }, onFavoriteClick = { viewModel.toggleFavorite(state.latestMoment.id) }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(480.dp)
                                    .padding(horizontal = 32.dp, vertical = 24.dp)
                                    .shadow(16.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Transparent)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = HeartRed.copy(alpha = 0.5f), modifier = Modifier.size(72.dp))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Start the Story", 
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ), 
                                        color = TextDeep
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Your scrapbook is waiting.\nTap below to take your first photo and magically update their wallpaper.",
                                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // PRIMARY ACTION
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(
                            modifier = Modifier
                                .shadow(24.dp, RoundedCornerShape(100.dp), ambientColor = HeartRed.copy(alpha = 0.8f), spotColor = Color.Transparent),
                            color = HeartRed,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable(onClick = onSendMoment)
                                    .padding(horizontal = 32.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Leave a Moment",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }

                    // TIMELINE
                    val feedMoments = state.groupedMoments.mapValues { (_, moments) ->
                        moments.filter { it.id != state.latestMoment?.id }
                    }.filterValues { it.isNotEmpty() }

                    var globalIndex = 2 // item count above

                    feedMoments.forEach { (groupName, moments) ->
                        item {
                            CinematicHeader(
                                text = groupName,
                                listState = listState,
                                itemIndex = globalIndex
                            )
                            globalIndex++
                        }

                        itemsIndexed(moments) { _, moment ->
                            ImmersiveTimelineMoment(
                                moment = moment,
                                partnerId = state.partnerId,
                                partnerName = state.partnerName,
                                listState = listState,
                                itemIndex = globalIndex,
                                onFavoriteClick = { viewModel.toggleFavorite(moment.id) },
                                onClick = { selectedMoment = moment }
                            )
                            globalIndex++
                        }
                    }
                }
                
                // Floating Emotional Action Menu
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 100.dp, end = 16.dp),
                    contentAlignment = Alignment.CenterEnd // Positioned at middle right near hero
                ) {
                    EmotionalActionMenu(
                        onActionSelected = { action ->
                            viewModel.sendEmotionalAction(action)
                        }
                    )
                }

                // Success toast/animation for action
                AnimatedVisibility(
                    visible = actionSuccessState != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 }),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = actionSuccessState ?: "",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
        
        AnimatedVisibility(
            visible = selectedMoment != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            selectedMoment?.let { moment ->
                MomentDetailOverlay(
                    moment = moment,
                    partnerName = (uiState as? MomentsUiState.Success)?.partnerName ?: "Partner",
                    onDismiss = { selectedMoment = null }
                )
            }
        }
    }
}

@Composable
fun CinematicHeader(text: String, listState: LazyListState, itemIndex: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = TextDeep,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ImmersiveHeroMoment(moment: MomentEntity, isPaused: Boolean, partnerId: String, partnerName: String, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    val isMine = moment.creatorId != partnerId
    val heroText = if (isMine) "On $partnerName's screen" else "On your screen"

    val context = LocalContext.current
    var showHeartBurst by remember { mutableStateOf(false) }
    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            kotlinx.coroutines.delay(600)
            showHeartBurst = false
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hero_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = heroText,
            style = MaterialTheme.typography.titleLarge,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .shadow(8.dp, RoundedCornerShape(32.dp), ambientColor = HeartRed.copy(alpha = 0.3f), spotColor = Color.Transparent)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onDoubleTap = {
                            HapticFeedbackManager.playHeartbeat(context)
                            showHeartBurst = true
                            onFavoriteClick()
                        }
                    )
                }
        ) {
            AsyncImage(
                model = moment.imageUrl,
                contentDescription = "Latest Moment",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            HeartBurstOverlay(visible = showHeartBurst)
            
            if (isPaused) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Pause, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partner paused updates", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        if (!moment.note.isNullOrBlank()) {
            Text(
                text = moment.note,
                style = MaterialTheme.typography.titleLarge,
                color = TextDeep,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        val relativeTime = TimeUtils.getRelativeTimeSpan(moment.createdAt)
        Text(
            text = relativeTime,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
fun ImmersiveTimelineMoment(
    moment: MomentEntity,
    partnerId: String,
    partnerName: String,
    listState: LazyListState,
    itemIndex: Int,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val isMine = moment.creatorId != partnerId
    val relationLabel = if (isMine) "You" else partnerName

    val context = LocalContext.current
    var showHeartBurst by remember { mutableStateOf(false) }
    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            kotlinx.coroutines.delay(600)
            showHeartBurst = false
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    // Calculate Parallax Scroll Offset
    val layoutInfo = listState.layoutInfo
    val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == itemIndex }
    
    val parallaxOffset = if (visibleItem != null) {
        val screenCenter = layoutInfo.viewportSize.height / 2f
        val itemCenter = visibleItem.offset + (visibleItem.size / 2f)
        (itemCenter - screenCenter) * 0.15f // 15% parallax scroll
    } else 0f

    // Calculate entrance scale/alpha based on scroll position
    val scrollScale = if (visibleItem != null) {
        val screenHeight = layoutInfo.viewportSize.height
        val itemTop = visibleItem.offset
        val threshold = screenHeight * 0.85f
        if (itemTop > threshold) {
            0.9f + 0.1f * (1f - ((itemTop - threshold) / (screenHeight - threshold)).coerceIn(0f, 1f))
        } else 1f
    } else 1f

    val scrollAlpha = if (visibleItem != null) {
        val screenHeight = layoutInfo.viewportSize.height
        val itemTop = visibleItem.offset
        val threshold = screenHeight * 0.85f
        if (itemTop > threshold) {
            1f - ((itemTop - threshold) / (screenHeight - threshold)).coerceIn(0f, 1f)
        } else 1f
    } else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .graphicsLayer {
                scaleX = pressScale * scrollScale
                scaleY = pressScale * scrollScale
                alpha = scrollAlpha
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = {
                        HapticFeedbackManager.playHeartbeat(context)
                        showHeartBurst = true
                        onFavoriteClick()
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
        ) {
            AsyncImage(
                model = moment.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.2f
                        scaleY = 1.2f
                        translationY = parallaxOffset
                    }
            )
            HeartBurstOverlay(visible = showHeartBurst)
            
            // Favorite Action
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = if (moment.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Keep",
                    tint = if (moment.isFavorite) HeartRed else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!moment.note.isNullOrBlank()) {
            Text(
                text = moment.note,
                style = MaterialTheme.typography.titleLarge,
                color = TextDeep,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val relativeTime = TimeUtils.getRelativeTimeSpan(moment.createdAt)
        Text(
            text = "$relationLabel • $relativeTime",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
fun MomentDetailOverlay(moment: MomentEntity, partnerName: String, onDismiss: () -> Unit) {
    val isMine = moment.creatorId != partnerName // Check if it's the partner's id or just an approximation. We assume standard usage.
    val relationLabel = if (isMine) "You" else partnerName

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f/4f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
            ) {
                AsyncImage(
                    model = moment.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (!moment.note.isNullOrBlank()) {
                Text(
                    text = moment.note,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            val relativeTime = TimeUtils.getRelativeTimeSpan(moment.createdAt)
            
            Text(
                text = relativeTime,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun HeartBurstOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), initialScale = 0.3f) + fadeIn(),
        exit = scaleOut(tween(300), targetScale = 1.5f) + fadeOut(tween(300)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = HeartRed,
                modifier = Modifier.size(120.dp).shadow(12.dp, CircleShape)
            )
        }
    }
}
