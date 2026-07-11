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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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

enum class SendAnimationState {
    IDLE,
    MOVE_TO_CENTER,
    ENVELOPE_APPEARS,
    STAMP,
    FLY_OUT
}

@Composable
fun MomentsScreen(
    viewModel: MomentsViewModel = hiltViewModel(),
    onSendMoment: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionSuccessState by viewModel.actionSuccessState.collectAsState()
    val listState = rememberLazyListState()
    var selectedMomentId by remember { mutableStateOf<String?>(null) }
    var isTimelineView by remember { mutableStateOf(true) }

    BackHandler(enabled = selectedMomentId != null) {
        selectedMomentId = null
    }
    BackHandler(enabled = !isTimelineView && selectedMomentId == null) {
        isTimelineView = true
    }
    
    var animationState by remember { mutableStateOf(SendAnimationState.IDLE) }
    var animatingAction by remember { mutableStateOf<EmotionalAction?>(null) }
    var iconOffsets by remember { mutableStateOf(mutableMapOf<EmotionalAction, androidx.compose.ui.geometry.Offset>()) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

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
                    Text("You haven't connected your worlds yet.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDeep)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Head to 'Our World' to connect with your partner.", color = TextMuted)
                }
            }
            is MomentsUiState.Success -> {
                // TIMELINE
                val feedMoments = remember(state.groupedMoments, state.latestMoment?.id) {
                    state.groupedMoments.mapValues { (_, moments) ->
                        moments.filter { it.id != state.latestMoment?.id }
                    }.filterValues { it.isNotEmpty() }
                }

                val flattenedFeed = remember(feedMoments) {
                    val list = mutableListOf<Any>()
                    feedMoments.forEach { (groupName, moments) ->
                        list.add(groupName)
                        list.addAll(moments)
                    }
                    list
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 160.dp, top = 96.dp),
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
                                onClick = { selectedMomentId = state.latestMoment.id }, onFavoriteClick = { viewModel.toggleFavorite(state.latestMoment.id) }
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

                    // EMOTIONAL ACTIONS ROW
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val view = androidx.compose.ui.platform.LocalView.current
                            
                            @Composable
                            fun EmotionIcon(drawableRes: Int, action: EmotionalAction) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val baseScale = if (action == EmotionalAction.Cuddle) 0.85f else 1f
                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.85f * baseScale else baseScale,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy, 
                                        stiffness = Spring.StiffnessLow
                                    )
                                )

                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = drawableRes),
                                    contentDescription = action.name,
                                    modifier = Modifier
                                        .onGloballyPositioned { coordinates ->
                                            val rootOffset = coordinates.positionInRoot()
                                            val iconCenter = androidx.compose.ui.geometry.Offset(
                                                rootOffset.x + coordinates.size.width / 2f,
                                                rootOffset.y + coordinates.size.height / 2f
                                            )
                                            val centerRelativeX = iconCenter.x - screenWidthPx / 2f
                                            val centerRelativeY = iconCenter.y - screenHeightPx / 2f
                                            iconOffsets[action] = androidx.compose.ui.geometry.Offset(centerRelativeX, centerRelativeY)
                                        }
                                        .size(50.dp)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                            animatingAction = action
                                            // We let the LaunchedEffect handle state change to ensure entrance animation plays
                                            viewModel.sendEmotionalAction(action)
                                        }
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            }
                            
                            EmotionIcon(R.drawable.ic_thought_bubble, EmotionalAction.ThinkingOfYou)
                            EmotionIcon(R.drawable.ic_punch_forward, EmotionalAction.Punch)
                            EmotionIcon(R.drawable.ic_cuddling_teddies, EmotionalAction.Cuddle)
                            EmotionIcon(R.drawable.ic_kiss_face, EmotionalAction.Kiss)
                            EmotionIcon(R.drawable.ic_pleading_face, EmotionalAction.MissYou)
                        }
                    }

                    // PRIMARY ACTION
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(
                            modifier = Modifier
                                .shadow(16.dp, RoundedCornerShape(100.dp), spotColor = HeartRed.copy(alpha = 0.5f), ambientColor = HeartRed.copy(alpha = 0.2f)),
                            color = HeartRed,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable(onClick = onSendMoment)
                                    .padding(horizontal = 36.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.PhotoCamera, 
                                    contentDescription = null, 
                                    tint = Color.White, 
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Leave a Moment",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }

                    itemsIndexed(
                        items = flattenedFeed,
                        key = { _, item -> if (item is String) item else (item as MomentEntity).id }
                    ) { index, item ->
                        val isLeft = index % 2 == 0
                        if (item is String) {
                            TimelineDateNode(
                                text = item,
                                isLeft = isLeft
                            )
                        } else if (item is MomentEntity) {
                            StaggeredTimelineMoment(
                                moment = item,
                                isLeft = isLeft,
                                partnerId = state.partnerId,
                                partnerName = state.partnerName,
                                onFavoriteClick = { viewModel.toggleFavorite(item.id) },
                                onClick = { selectedMomentId = item.id }
                            )
                        }
                    }
                }
                
                // Success toast/animation for action
                AnimatedVisibility(
                    visible = actionSuccessState != null && actionSuccessState?.contains("too many") == true,
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

                // Memory Box Tab Content
                if (!isTimelineView) {
                    val allMoments = state.groupedMoments.values.flatten()
                    val favorites = allMoments.filter { it.isFavorite }.sortedByDescending { it.createdAt }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SoftCream)
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(72.dp)) // Leave space for toggle
                            
                            
                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(
                                    items = favorites,
                                    key = { _, moment -> moment.id }
                                ) { index, moment ->
                                    var isVisible by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) {
                                        kotlinx.coroutines.delay(index * 50L)
                                        isVisible = true
                                    }
                                    AnimatedVisibility(
                                        visible = isVisible,
                                        enter = fadeIn(tween(400)) + slideInVertically(tween(400), initialOffsetY = { 50 })
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.75f)
                                                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White)
                                                .clickable { selectedMomentId = moment.id }
                                        ) {
                                            AsyncImage(
                                                model = moment.imageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            
                                            val interactionSource = remember { MutableInteractionSource() }
                                            val isPressed by interactionSource.collectIsPressedAsState()
                                            val scale by animateFloatAsState(
                                                targetValue = if (isPressed) 0.7f else 1f,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(12.dp)
                                                    .scale(scale)
                                                    .background(Color.White, CircleShape)
                                                    .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.2f))
                                                    .clickable(
                                                        interactionSource = interactionSource,
                                                        indication = null
                                                    ) { viewModel.toggleFavorite(moment.id) }
                                                    .padding(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Favorite,
                                                    contentDescription = "Favorited",
                                                    tint = HeartRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Toggle Switch (Top Center)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.shadow(4.dp, RoundedCornerShape(100.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            val activeBg = Color.White
                            val inactiveBg = Color.Transparent
                            val activeText = HeartRed
                            val inactiveText = TextMuted

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (isTimelineView) activeBg else inactiveBg)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { isTimelineView = true }
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "Our Story", 
                                    color = if (isTimelineView) activeText else inactiveText, 
                                    fontWeight = if (isTimelineView) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                    fontSize = 15.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (!isTimelineView) activeBg else inactiveBg)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { isTimelineView = false }
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "Favorites", 
                                    color = if (!isTimelineView) activeText else inactiveText, 
                                    fontWeight = if (!isTimelineView) FontWeight.Bold else FontWeight.Medium,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }




            }
        }
        
        // --- CHOREOGRAPHED SEND ANIMATION ---
        if (animatingAction != null) {
            val action = animatingAction!!
            val drawableRes = when (action) {
                EmotionalAction.ThinkingOfYou -> R.drawable.ic_thought_bubble
                EmotionalAction.Punch -> R.drawable.ic_punch_forward
                EmotionalAction.Cuddle -> R.drawable.ic_cuddling_teddies
                EmotionalAction.Kiss -> R.drawable.ic_kiss_face
                EmotionalAction.MissYou -> R.drawable.ic_pleading_face
            }
            
            // Emoji Animation States
            val emojiScale by animateFloatAsState(
                targetValue = when (animationState) {
                    SendAnimationState.IDLE -> 1f 
                    SendAnimationState.MOVE_TO_CENTER -> 2.5f
                    SendAnimationState.ENVELOPE_APPEARS -> 2.5f
                    SendAnimationState.STAMP -> 1f
                    SendAnimationState.FLY_OUT -> 1f
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            
            val startOffsetX = with(density) { iconOffsets[action]?.x?.toDp() ?: 0.dp }
            val startOffsetY = with(density) { iconOffsets[action]?.y?.toDp() ?: 300.dp }

            val emojiOffsetY by animateDpAsState(
                targetValue = when (animationState) {
                    SendAnimationState.IDLE -> startOffsetY
                    SendAnimationState.MOVE_TO_CENTER, SendAnimationState.ENVELOPE_APPEARS -> 0.dp
                    SendAnimationState.STAMP -> 10.dp 
                    SendAnimationState.FLY_OUT -> (-800).dp 
                },
                animationSpec = when (animationState) {
                    SendAnimationState.FLY_OUT -> tween(1800, easing = FastOutLinearInEasing)
                    else -> tween(600, easing = FastOutSlowInEasing)
                }
            )
            
            val emojiOffsetX by animateDpAsState(
                targetValue = when (animationState) {
                    SendAnimationState.IDLE -> startOffsetX
                    SendAnimationState.MOVE_TO_CENTER, SendAnimationState.ENVELOPE_APPEARS, SendAnimationState.STAMP -> 0.dp
                    SendAnimationState.FLY_OUT -> 400.dp 
                },
                animationSpec = when (animationState) {
                    SendAnimationState.FLY_OUT -> keyframes {
                        durationMillis = 1800
                        0.dp at 0
                        (-100).dp at 500 with FastOutSlowInEasing
                        250.dp at 1100 with FastOutSlowInEasing
                        50.dp at 1500 with FastOutSlowInEasing
                        400.dp at 1800
                    }
                    else -> tween(600, easing = FastOutSlowInEasing)
                }
            )

            // Envelope Animation States (Kite-like flying physics)
            val envelopeOffsetX by animateDpAsState(
                targetValue = when (animationState) {
                    SendAnimationState.IDLE, SendAnimationState.MOVE_TO_CENTER -> (-300).dp 
                    SendAnimationState.ENVELOPE_APPEARS, SendAnimationState.STAMP -> 0.dp 
                    SendAnimationState.FLY_OUT -> 400.dp 
                },
                animationSpec = when (animationState) {
                    SendAnimationState.FLY_OUT -> keyframes {
                        durationMillis = 1800
                        0.dp at 0
                        (-100).dp at 500 with FastOutSlowInEasing
                        250.dp at 1100 with FastOutSlowInEasing
                        50.dp at 1500 with FastOutSlowInEasing
                        400.dp at 1800
                    }
                    SendAnimationState.ENVELOPE_APPEARS -> tween(800, easing = LinearOutSlowInEasing)
                    else -> tween(600)
                }
            )
            
            val envelopeOffsetY by animateDpAsState(
                targetValue = when (animationState) {
                    SendAnimationState.IDLE, SendAnimationState.MOVE_TO_CENTER -> 400.dp
                    SendAnimationState.ENVELOPE_APPEARS, SendAnimationState.STAMP -> 0.dp
                    SendAnimationState.FLY_OUT -> (-800).dp
                },
                animationSpec = when (animationState) {
                    SendAnimationState.FLY_OUT -> tween(1800, easing = FastOutLinearInEasing)
                    SendAnimationState.ENVELOPE_APPEARS -> keyframes {
                        durationMillis = 800
                        400.dp at 0
                        (-40).dp at 500 with FastOutSlowInEasing 
                        15.dp at 650 with FastOutSlowInEasing 
                        0.dp at 800
                    }
                    else -> tween(600)
                }
            )

            val envelopeScale by animateFloatAsState(
                targetValue = when (animationState) {
                    SendAnimationState.IDLE, SendAnimationState.MOVE_TO_CENTER -> 0.2f
                    SendAnimationState.ENVELOPE_APPEARS, SendAnimationState.STAMP -> 1f
                    SendAnimationState.FLY_OUT -> 0.5f
                },
                animationSpec = when (animationState) {
                    SendAnimationState.ENVELOPE_APPEARS -> tween(800)
                    else -> tween(600)
                }
            )

            val envelopeRotation by animateFloatAsState(
                targetValue = when (animationState) {
                    SendAnimationState.IDLE, SendAnimationState.MOVE_TO_CENTER -> -45f
                    SendAnimationState.ENVELOPE_APPEARS, SendAnimationState.STAMP -> 0f
                    SendAnimationState.FLY_OUT -> 30f
                },
                animationSpec = when (animationState) {
                    SendAnimationState.FLY_OUT -> keyframes {
                        durationMillis = 1800
                        0f at 0
                        -30f at 500 with FastOutSlowInEasing
                        30f at 1100 with FastOutSlowInEasing
                        -15f at 1500 with FastOutSlowInEasing
                        30f at 1800
                    }
                    SendAnimationState.ENVELOPE_APPEARS -> keyframes {
                        durationMillis = 800
                        -45f at 0
                        20f at 500
                        -10f at 650
                        0f at 800
                    }
                    else -> tween(600)
                }
            )
            
            // Fades
            val alpha by animateFloatAsState(
                targetValue = if (animationState == SendAnimationState.FLY_OUT) 0f else 1f,
                animationSpec = tween(600, delayMillis = 1200)
            )
            
            val envelopeAlpha by animateFloatAsState(
                targetValue = when (animationState) {
                    SendAnimationState.IDLE, SendAnimationState.MOVE_TO_CENTER -> 0f
                    else -> alpha
                },
                animationSpec = tween(400)
            )

            val bgAlpha by animateFloatAsState(
                targetValue = if (animationState == SendAnimationState.FLY_OUT || animationState == SendAnimationState.IDLE) 0f else 0.6f,
                animationSpec = tween(400)
            )

            LaunchedEffect(animatingAction) {
                if (animationState == SendAnimationState.IDLE) {
                    kotlinx.coroutines.delay(50) // Allow composition to register initial state
                    animationState = SendAnimationState.MOVE_TO_CENTER
                }
            }

            LaunchedEffect(animationState) {
                when (animationState) {
                    SendAnimationState.MOVE_TO_CENTER -> {
                        kotlinx.coroutines.delay(500)
                        animationState = SendAnimationState.ENVELOPE_APPEARS
                    }
                    SendAnimationState.ENVELOPE_APPEARS -> {
                        kotlinx.coroutines.delay(800)
                        animationState = SendAnimationState.STAMP
                    }
                    SendAnimationState.STAMP -> {
                        kotlinx.coroutines.delay(400)
                        animationState = SendAnimationState.FLY_OUT
                    }
                    SendAnimationState.FLY_OUT -> {
                        kotlinx.coroutines.delay(1800)
                        animationState = SendAnimationState.IDLE
                        animatingAction = null
                    }
                    SendAnimationState.IDLE -> {}
                }
            }

            val letterText = when (action) {
                EmotionalAction.ThinkingOfYou -> "Just thinking of you..."
                EmotionalAction.Punch -> "Incoming punch!"
                EmotionalAction.Cuddle -> "I wanna hug you..."
                EmotionalAction.Kiss -> "Sending a kiss"
                EmotionalAction.MissYou -> "I really miss you..."
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = bgAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = envelopeOffsetX, y = envelopeOffsetY)
                        .graphicsLayer {
                            scaleX = envelopeScale
                            scaleY = envelopeScale
                            rotationZ = envelopeRotation
                            this.alpha = envelopeAlpha
                        }
                        .width(260.dp)
                        .height(180.dp)
                        .shadow(24.dp, RoundedCornerShape(8.dp), spotColor = Color.Black.copy(alpha = 0.4f))
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFFFFFDF9), Color(0xFFF3EBE1))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                ) {
                    // Envelope Fold Details
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Bottom-to-center diagonal fold lines
                        val bottomFolds = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, h)
                            lineTo(w / 2f, h * 0.6f)
                            lineTo(w, h)
                        }
                        drawPath(
                            path = bottomFolds,
                            color = Color.Black.copy(alpha = 0.05f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5.dp.toPx(),
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )

                        // Top Flap Tip
                        val topFlap = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(w / 2f, h * 0.52f)
                            lineTo(w, 0f)
                        }
                        
                        // Fake drop-shadow under the top flap
                        drawPath(
                            path = topFlap,
                            color = Color.Black.copy(alpha = 0.08f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 6.dp.toPx(),
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                cap = StrokeCap.Round
                            )
                        )

                        // Top Flap Edge Highlight (embossed paper look)
                        drawPath(
                            path = topFlap,
                            color = Color.White.copy(alpha = 0.7f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx(),
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                        
                        // Top Flap Edge Crease
                        drawPath(
                            path = topFlap,
                            color = Color.Black.copy(alpha = 0.12f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.dp.toPx(),
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )
                    }

                    // Authentic Handwriting
                    Text(
                        text = letterText,
                        color = Color(0xFF3E2723), // Dark Sepia Ink
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Cursive,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 26.dp, start = 24.dp, end = 24.dp)
                            .graphicsLayer { rotationZ = -3f } // Slightly crooked for natural feel
                    )
                }
                
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = drawableRes),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = emojiOffsetX, y = emojiOffsetY)
                        .graphicsLayer {
                            scaleX = emojiScale
                            scaleY = emojiScale
                            rotationZ = if (animationState == SendAnimationState.STAMP || animationState == SendAnimationState.FLY_OUT) envelopeRotation else 0f
                            this.alpha = alpha
                        }
                        .size(60.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        val allMoments = (uiState as? MomentsUiState.Success)?.groupedMoments?.values?.flatten() ?: emptyList()
        val latestHero = (uiState as? MomentsUiState.Success)?.latestMoment
        val currentSelectedMoment = selectedMomentId?.let { id ->
            if (id == latestHero?.id) latestHero else allMoments.find { it.id == id }
        }

        AnimatedVisibility(
            visible = currentSelectedMoment != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            currentSelectedMoment?.let { moment ->
                MomentDetailOverlay(
                    moment = moment,
                    partnerName = (uiState as? MomentsUiState.Success)?.partnerName ?: "Partner",
                    onDismiss = { selectedMomentId = null },
                    onFavoriteClick = { viewModel.toggleFavorite(moment.id) }
                )
            }
        }
    }
}

@Composable
fun TimelineDateNode(text: String, isLeft: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .drawBehind {
                drawLine(
                    color = Color(0xFFC2B4D6).copy(alpha = 0.5f), // Soft purple line
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                )
                drawCircle(
                    color = Color(0xFFA5B2D6), // Soft blue center
                    radius = 5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (isLeft) {
                Box(modifier = Modifier.weight(1f).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(text = text.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextDeep)
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(1f).padding(start = 24.dp), contentAlignment = Alignment.CenterStart) {
                    Text(text = text.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextDeep)
                }
            }
        }
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
            
            // Vignette and Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(100.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            Text(
                text = heroText,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
            
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
fun StaggeredTimelineMoment(
    moment: MomentEntity,
    isLeft: Boolean,
    partnerId: String,
    partnerName: String,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    val isMine = moment.creatorId != partnerId
    val relationLabel = if (isMine) "You" else partnerName
    val timeLabel = TimeUtils.getRelativeTimeSpan(moment.createdAt)
    val displayLabel = "$relationLabel • $timeLabel"

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = Color(0xFFC2B4D6).copy(alpha = 0.5f),
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cardContent = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    ),
                horizontalAlignment = if (isLeft) Alignment.End else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                ) {
                    AsyncImage(
                        model = moment.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (!moment.note.isNullOrBlank()) {
                    Text(
                        text = moment.note,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDeep,
                        textAlign = if (isLeft) TextAlign.End else TextAlign.Start,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = if (isLeft) TextAlign.End else TextAlign.Start
                )
            }
        }

        if (isLeft) {
            Box(modifier = Modifier.weight(1f).padding(start = 24.dp, end = 24.dp)) {
                cardContent()
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.weight(1f).padding(start = 24.dp, end = 24.dp)) {
                cardContent()
            }
        }
    }
}

@Composable
fun MomentDetailOverlay(moment: MomentEntity, partnerName: String, onDismiss: () -> Unit, onFavoriteClick: () -> Unit) {
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
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite",
                        tint = if (moment.isFavorite) HeartRed else Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(120.dp))
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
                modifier = Modifier.size(120.dp)
            )
        }
    }
}
