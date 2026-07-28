package com.pranayburra.moment.ui.moments

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.NoMeetingRoom
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Block
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
import com.pranayburra.moment.data.local.MomentEntity
import com.pranayburra.moment.data.remote.RelationshipDto
import com.pranayburra.moment.data.remote.UserDto
import com.pranayburra.moment.ui.theme.HeartRed
import com.pranayburra.moment.ui.theme.SoftCream
import com.pranayburra.moment.ui.theme.DeepMauve
import com.pranayburra.moment.ui.theme.SoftRose
import com.pranayburra.moment.ui.theme.TextDeep
import com.pranayburra.moment.ui.theme.TextMuted
import com.pranayburra.moment.ui.theme.WarmBeige
import com.pranayburra.moment.ui.theme.RoseQuartz
import com.pranayburra.moment.ui.theme.White
import com.pranayburra.moment.ui.theme.ErrorSoft
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
    modifier: Modifier = Modifier,
    viewModel: UsViewModel = hiltViewModel(),
    authViewModel: com.pranayburra.moment.ui.auth.AuthViewModel = hiltViewModel(),
    spaceSettingsViewModel: com.pranayburra.moment.ui.settings.SpaceSettingsViewModel = hiltViewModel(),
    onOverlayVisibilityChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.currentUser.collectAsState()
    val vibeUpdateState by authViewModel.vibeUpdateState.collectAsState()
    val reportState by spaceSettingsViewModel.reportState.collectAsState()
    val blockState by spaceSettingsViewModel.blockState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(vibeUpdateState) {
        when (val current = vibeUpdateState) {
            is com.pranayburra.moment.util.Resource.Success -> {
                val cleared = current.data?.currentVibe.isNullOrBlank()
                val message = if (cleared) "Vibe cleared" else "Vibe updated"
                com.pranayburra.moment.util.showAppToast(context, message)
                authViewModel.resetVibeUpdateState()
            }
            is com.pranayburra.moment.util.Resource.Error -> {
                com.pranayburra.moment.util.showAppToast(context, current.message ?: "Failed to update vibe")
                authViewModel.resetVibeUpdateState()
            }
            else -> {}
        }
    }

    UsScreenContent(
        modifier = modifier,
        uiState = uiState,
        authState = authState,
        reportState = reportState,
        blockState = blockState,
        onUpdateSpaceName = { viewModel.updateSpaceName(it) },
        onUnpair = { viewModel.unpair() },
        onUpdateVibe = { authViewModel.updateVibe(it) },
        onTogglePause = { viewModel.togglePause() },
        onUpdateAnniversary = { viewModel.updateAnniversaryDate(it) },
        onReportPartner = { spaceSettingsViewModel.reportPartner(it) },
        onResetReportState = { spaceSettingsViewModel.resetReportState() },
        onBlockPartner = { spaceSettingsViewModel.blockPartner() },
        onResetBlockState = { spaceSettingsViewModel.resetBlockState() },
        onOverlayVisibilityChanged = onOverlayVisibilityChanged
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsScreenContent(
    modifier: Modifier = Modifier,
    uiState: UsUiState,
    authState: com.pranayburra.moment.util.Resource<UserDto>,
    reportState: com.pranayburra.moment.util.Resource<Boolean> = com.pranayburra.moment.util.Resource.Idle(),
    blockState: com.pranayburra.moment.util.Resource<Unit> = com.pranayburra.moment.util.Resource.Idle(),
    onUpdateSpaceName: (String) -> Unit,
    onUnpair: () -> Unit,
    onUpdateVibe: (String) -> Unit,
    onTogglePause: () -> Unit,
    onUpdateAnniversary: (String) -> Unit,
    onReportPartner: (String) -> Unit = {},
    onResetReportState: () -> Unit = {},
    onBlockPartner: () -> Unit = {},
    onResetBlockState: () -> Unit = {},
    onOverlayVisibilityChanged: (Boolean) -> Unit = {}
) {
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    
    var showUnpairDialog by remember { mutableStateOf(false) }
    var showVibeModal by remember { mutableStateOf(false) }
    var isSettingsExpanded by remember { mutableStateOf(false) }
    var showAnniversaryDatePicker by remember { mutableStateOf(false) }
    var selectedProfileUrl by remember { mutableStateOf<String?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReasonInput by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    // The Unpair/Block and Report dialogs used to live inside the `is UsUiState.Success`
    // branch below, reading the partner's name straight off `state.relationship`. The
    // moment either action succeeds, the relationship flips to unpaired and that whole
    // branch (dialog included) gets torn out of composition immediately - before the
    // dialog got a chance to close on its own terms, so it just vanished mid-action instead
    // of finishing gracefully. Caching the name here (and rendering both dialogs outside
    // the `when` block, see below) keeps them mounted through that transition.
    var partnerNameSnapshot by remember { mutableStateOf("") }
    LaunchedEffect(uiState) {
        (uiState as? UsUiState.Success)?.let { partnerNameSnapshot = it.relationship.partner.displayName }
    }

    LaunchedEffect(selectedProfileUrl) {
        onOverlayVisibilityChanged(selectedProfileUrl != null)
    }

    LaunchedEffect(reportState) {
        val current = reportState
        if (current is com.pranayburra.moment.util.Resource.Success) {
            showReportDialog = false
            reportReasonInput = ""
            val blockSucceeded = current.data ?: false
            val message = if (blockSucceeded) {
                "Reported and unpaired"
            } else {
                "Report submitted, but we couldn't unpair automatically - unpair from the menu below to finish."
            }
            com.pranayburra.moment.util.showAppToast(context, message)
            onResetReportState()
        }
    }

    LaunchedEffect(blockState) {
        if (blockState is com.pranayburra.moment.util.Resource.Success) {
            showUnpairDialog = false
            com.pranayburra.moment.util.showAppToast(context, "Blocked and unpaired")
            onResetBlockState()
        }
    }


    Box(modifier = modifier.fillMaxSize().background(SoftCream)) {
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
                                    onUpdateSpaceName(editNameInput)
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

                if (showAnniversaryDatePicker) {
                    val datePickerState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showAnniversaryDatePicker = false },
                        confirmButton = {
                            val isFuture = datePickerState.selectedDateMillis?.let { it > System.currentTimeMillis() } ?: false
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val date = java.time.Instant.ofEpochMilli(millis)
                                        onUpdateAnniversary(date.toString())
                                    }
                                    showAnniversaryDatePicker = false
                                },
                                enabled = !isFuture
                            ) {
                                Text("Save", color = if (isFuture) Color.Gray else HeartRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAnniversaryDatePicker = false }) {
                                Text("Cancel", color = TextDeep)
                            }
                        },
                        colors = DatePickerDefaults.colors(containerColor = White)
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                if (showVibeModal) {
                    val activeVibe = (authState as? com.pranayburra.moment.util.Resource.Success)?.data?.currentVibe
                        ?: state.currentUser?.currentVibe
                    VibeSelectorModal(
                        currentVibe = activeVibe,
                        onDismiss = { showVibeModal = false },
                        onVibeSelected = { emoji ->
                            onUpdateVibe(emoji)
                            showVibeModal = false
                        }
                    )
                }

                val actualCurrentUser = (authState as? com.pranayburra.moment.util.Resource.Success)?.data ?: state.currentUser

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
                                showVibeModal = true 
                            },
                            onProfileClick = { url ->
                                selectedProfileUrl = url
                            }
                        )
                        val daysTogether = try {
                            val startString = state.relationship.anniversaryDate ?: state.relationship.pairedAt ?: state.relationship.createdAt
                            val start = java.time.Instant.parse(startString)
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

                    // 5. Settings Sections
                    item {
                        FadingDivider()
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSettingsExpanded = !isSettingsExpanded }
                                .padding(horizontal = 32.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MANAGE RELATIONSHIP",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                                color = TextMuted.copy(alpha = 0.8f)
                            )
                            Icon(
                                imageVector = if (isSettingsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Expand Settings",
                                tint = TextMuted
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isSettingsExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
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
                                        .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
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
                                        SpaceSettingItem(
                                            icon = Icons.Outlined.CalendarToday,
                                            title = "Set Anniversary Date",
                                            subtitle = "Change the start date of your world",
                                            onClick = { showAnniversaryDatePicker = true }
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
                                        .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(White)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        var isPausing by remember(state.relationship.isPausedByMe) { mutableStateOf(false) }
                                        SpaceSettingItem(
                                            icon = Icons.Outlined.Pause,
                                            title = if (state.relationship.isPausedByMe) "Reconnect Space" else "Take Space",
                                            subtitle = if (state.relationship.isPausedByMe) "You are currently taking space" else "Temporarily pause sharing moments",
                                            isLoading = isPausing,
                                            onClick = {
                                                isPausing = true
                                                onTogglePause()
                                            }
                                        )
                                        SpaceSettingItem(
                                            icon = Icons.Outlined.NoMeetingRoom,
                                            title = "Say Goodbye (Unpair)",
                                            subtitle = "Unpair from ${state.relationship.partner.displayName}",
                                            color = ErrorSoft,
                                            onClick = { showUnpairDialog = true }
                                        )
                                        SpaceSettingItem(
                                            icon = Icons.Outlined.Flag,
                                            title = "Report ${state.relationship.partner.displayName}",
                                            subtitle = "Let us know if something's wrong",
                                            color = ErrorSoft,
                                            onClick = { showReportDialog = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rendered outside the `when` above (rather than nested inside the Success branch)
        // so they stay mounted through the moment unpair/block/report actually succeeds and
        // the relationship flips to unpaired - otherwise the whole Success branch, dialog
        // included, gets torn down mid-action. partnerNameSnapshot (updated above whenever
        // we're in a Success state) keeps the partner's name available even after that data
        // disappears from uiState.
        if (showUnpairDialog) {
            var alsoBlockOnUnpair by remember(showUnpairDialog) { mutableStateOf(false) }
            val isBlocking = blockState is com.pranayburra.moment.util.Resource.Loading
            AlertDialog(
                onDismissRequest = { if (!isBlocking) showUnpairDialog = false },
                title = { Text("Say Goodbye (Unpair)?") },
                text = {
                    Column {
                        Text("This will disconnect our worlds. You will no longer receive moments from your partner. This cannot be undone 💔")
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isBlocking) { alsoBlockOnUnpair = !alsoBlockOnUnpair }
                        ) {
                            Checkbox(
                                checked = alsoBlockOnUnpair,
                                onCheckedChange = { alsoBlockOnUnpair = it },
                                enabled = !isBlocking,
                                colors = CheckboxDefaults.colors(checkedColor = ErrorSoft)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Also block $partnerNameSnapshot from pairing with me again",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDeep
                            )
                        }
                        if (alsoBlockOnUnpair && blockState is com.pranayburra.moment.util.Resource.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                (blockState as com.pranayburra.moment.util.Resource.Error).message ?: "Failed to block user.",
                                color = ErrorSoft,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (alsoBlockOnUnpair) {
                                onBlockPartner()
                            } else {
                                onUnpair()
                                showUnpairDialog = false
                            }
                        },
                        enabled = !isBlocking,
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorSoft)
                    ) {
                        if (isBlocking) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                        } else {
                            Text(if (alsoBlockOnUnpair) "Block & Unpair" else "Say Goodbye")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnpairDialog = false }, enabled = !isBlocking) {
                        Text("Cancel", color = TextDeep)
                    }
                },
                containerColor = White
            )
        }

        if (showReportDialog) {
            val isSubmittingReport = reportState is com.pranayburra.moment.util.Resource.Loading
            AlertDialog(
                onDismissRequest = { if (!isSubmittingReport) showReportDialog = false },
                title = { Text("Report $partnerNameSnapshot") },
                text = {
                    Column {
                        Text(
                            "Tell us what happened. Our team will review this report.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Submitting this will also unpair you from $partnerNameSnapshot and prevent them from pairing with you again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorSoft
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reportReasonInput,
                            onValueChange = { reportReasonInput = it },
                            label = { Text("Reason") },
                            minLines = 3
                        )
                        if (reportState is com.pranayburra.moment.util.Resource.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                (reportState as com.pranayburra.moment.util.Resource.Error).message ?: "Failed to submit report.",
                                color = ErrorSoft,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onReportPartner(reportReasonInput) },
                        enabled = !isSubmittingReport && reportReasonInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorSoft)
                    ) {
                        if (isSubmittingReport) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                        } else {
                            Text("Report & Unpair")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }, enabled = !isSubmittingReport) {
                        Text("Cancel", color = TextDeep)
                    }
                },
                containerColor = White
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = selectedProfileUrl != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.9f),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            selectedProfileUrl?.let { url ->
                ProfilePictureOverlay(
                    url = url,
                    onDismiss = { selectedProfileUrl = null }
                )
            }
        }
    }
}

@Composable
fun ProfilePictureOverlay(url: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null, onClick = {}),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            androidx.compose.material3.IconButton(
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
fun SpaceSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    color: Color = TextDeep,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick, enabled = !isLoading)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = color,
                strokeWidth = 2.dp
            )
        } else {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
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
    onSetVibeClick: () -> Unit = {},
    onProfileClick: (String?) -> Unit = {}
) {
    val formattedDate = try {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
        val dateString = relationship.anniversaryDate ?: relationship.pairedAt ?: relationship.createdAt
        Instant.parse(dateString).atZone(ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        (relationship.anniversaryDate ?: relationship.pairedAt ?: relationship.createdAt).take(10)
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
                .height(140.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Floating warmth particles behind the photos
            FloatingWarmthParticles(
                modifier = Modifier
                    .width(180.dp)
                    .height(140.dp)
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
                            radius = size.maxDimension * 0.7f
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.7f
                    )
                }

            // You (right) — drifts in from the right
            Box(modifier = Modifier.offset(x = 24.dp + driftOffset)) {
                ProfilePictureCircle(
                    url = currentUser?.profilePictureUrl,
                    size = 110.dp,
                    modifier = Modifier
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { onProfileClick(currentUser?.profilePictureUrl) }
                )
                    if (!currentUser?.currentVibe.isNullOrBlank()) {
                        VibeBadge(
                            emoji = currentUser!!.currentVibe!!, 
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                        )
                    }
                }

            // Partner (left) — drifts in from the left, drawn last so it's on top
            Box(modifier = Modifier.offset(x = -(24.dp) - driftOffset)) {
                ProfilePictureCircle(
                    url = relationship.partner.profilePictureUrl,
                    size = 110.dp,
                    modifier = Modifier
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { onProfileClick(relationship.partner.profilePictureUrl) }
                )
                    if (!relationship.partner.currentVibe.isNullOrBlank()) {
                        VibeBadge(
                            emoji = relationship.partner.currentVibe!!, 
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
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSetVibeClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isVibeSet = !currentUser?.currentVibe.isNullOrBlank()
                    Text(
                        text = "Update your vibe",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isVibeSet) HeartRed else TextMuted
                    )
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
            .background(Color.White.copy(alpha = 0.2f)),
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "LITTLE THINGS",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = TextMuted.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
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
            val items = listOf(
                Triple(com.pranayburra.moment.R.drawable.ic_thought_bubble, signalsCount["ThinkingOfYou"] ?: 0, "Thoughts"),
                Triple(com.pranayburra.moment.R.drawable.ic_punch_forward, signalsCount["Punch"] ?: 0, "Punches"),
                Triple(com.pranayburra.moment.R.drawable.ic_cuddling_teddies, signalsCount["Cuddle"] ?: 0, "Cuddles"),
                Triple(com.pranayburra.moment.R.drawable.ic_kiss_face, signalsCount["Kiss"] ?: 0, "Kisses"),
                Triple(com.pranayburra.moment.R.drawable.ic_pleading_face, signalsCount["MissYou"] ?: 0, "Miss You's")
            )

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items.size) { index ->
                    val (icon, count, label) = items[index]
                    LittleThingCard(icon, count.toString(), label)
                }
            }
        }
    }
}

@Composable
fun LittleThingCard(drawableRes: Int, count: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(100.dp)
            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = drawableRes),
            contentDescription = label,
            modifier = Modifier.size(40.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDeep)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
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
    val isVibeSet = !currentVibe.isNullOrBlank()

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
                    val isSelected = isVibeSet && currentVibe == emoji
                    
                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .clickable { 
                                if (isSelected) onVibeSelected("") else onVibeSelected(emoji)
                            },
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

            if (isVibeSet) {
                TextButton(
                    onClick = { onVibeSelected("") },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("Clear Vibe", color = ErrorSoft)
                }
            }

            Spacer(modifier = Modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars))
        }
    }
}
