package com.pranayburra.moment.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.NoMeetingRoom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import com.pranayburra.moment.util.Resource
import com.pranayburra.moment.ui.theme.*
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.pranayburra.moment.widget.RelationshipWidgetReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceSettingsScreen(
    viewModel: SpaceSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val blockState by viewModel.blockState.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }

    var showUnpairDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReasonInput by remember { mutableStateOf("") }

    val rel = (uiState as? Resource.Success)?.data
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // This screen (reached via the gear-icon Settings path) used to gate the Report/Block
    // dialogs on `rel != null`, reading the partner's name straight off it. The moment
    // either action succeeds, the relationship flips to unpaired/null and the dialog - still
    // mid-confirmation - vanished instead of closing gracefully. Same bug already fixed on
    // the "Us" tab's equivalent screen (UsScreen.kt); snapshotting the name here and no
    // longer gating the dialogs on `rel` keeps them mounted through that transition.
    var partnerNameSnapshot by remember { mutableStateOf("") }
    LaunchedEffect(uiState) {
        (uiState as? Resource.Success)?.data?.let { partnerNameSnapshot = it.partner.displayName }
    }

    LaunchedEffect(reportState) {
        val current = reportState
        if (current is Resource.Success) {
            showReportDialog = false
            reportReasonInput = ""
            val blockSucceeded = current.data ?: false
            val message = if (blockSucceeded) {
                "Reported and unpaired"
            } else {
                "Report submitted, but we couldn't unpair automatically - unpair from the menu below to finish."
            }
            com.pranayburra.moment.util.showAppToast(context, message)
            viewModel.resetReportState()
        }
    }

    LaunchedEffect(blockState) {
        if (blockState is Resource.Success) {
            showUnpairDialog = false
            com.pranayburra.moment.util.showAppToast(context, "Blocked and unpaired")
            viewModel.resetBlockState()
            onNavigateBack()
        }
    }

    if (showEditNameDialog && rel != null) {
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

    // Rendered without gating on `rel` (unlike the content below) so they stay mounted
    // through the moment unpair/block/report actually succeeds and the relationship flips
    // to unpaired/null - partnerNameSnapshot (updated above whenever we're in a Success
    // state) keeps the partner's name available even after that data disappears.
    if (showUnpairDialog) {
        var alsoBlockOnUnpair by remember(showUnpairDialog) { mutableStateOf(false) }
        val isBlocking = blockState is Resource.Loading
        AlertDialog(
            onDismissRequest = { if (!isBlocking) showUnpairDialog = false },
            title = { Text("Close Space?") },
            text = {
                Column {
                    Text("This will permanently unpair you from your partner and close this space. This action cannot be undone.")
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
                    if (alsoBlockOnUnpair && blockState is Resource.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            (blockState as Resource.Error).message ?: "Failed to block user.",
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
                            viewModel.blockPartner()
                        } else {
                            viewModel.unpair()
                            showUnpairDialog = false
                            onNavigateBack()
                        }
                    },
                    enabled = !isBlocking,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorSoft)
                ) {
                    if (isBlocking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text(if (alsoBlockOnUnpair) "Block & Unpair" else "Close Space")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairDialog = false }, enabled = !isBlocking) { Text("Cancel", color = TextDeep) }
            },
            containerColor = White
        )
    }

    if (showReportDialog) {
        val isSubmitting = reportState is Resource.Loading
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showReportDialog = false },
            title = { Text("Report $partnerNameSnapshot") },
            text = {
                Column {
                    Text("Tell us what happened. Our team will review this report.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
                    if (reportState is Resource.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text((reportState as Resource.Error).message ?: "Failed to submit report.", color = ErrorSoft, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.reportPartner(reportReasonInput) },
                    enabled = !isSubmitting && reportReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorSoft)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    } else {
                        Text("Report & Unpair")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }, enabled = !isSubmitting) { Text("Cancel", color = TextDeep) }
            },
            containerColor = White
        )
    }

    Scaffold(
        containerColor = SoftCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Space Settings", fontWeight = FontWeight.Bold, color = TextDeep) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDeep)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftCream)
            )
        }
    ) { paddingValues ->
        if (rel != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                        .clip(RoundedCornerShape(24.dp))
                        .background(White)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SpaceSettingItem(
                            icon = Icons.Outlined.Edit,
                            title = "Name Our World",
                            subtitle = rel.spaceName,
                            onClick = {
                                editNameInput = rel.spaceName
                                showEditNameDialog = true
                            }
                        )
                    }
                }

                // Add Widget setting block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                        .clip(RoundedCornerShape(24.dp))
                        .background(White)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SpaceSettingItem(
                            icon = Icons.Outlined.FavoriteBorder,
                            title = "Add Our Portal",
                            subtitle = "Keep your partner close to your home screen",
                            onClick = {
                                coroutineScope.launch {
                                    val manager = GlanceAppWidgetManager(context)
                                    manager.requestPinGlanceAppWidget(RelationshipWidgetReceiver::class.java)
                                }
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                        .clip(RoundedCornerShape(24.dp))
                        .background(White)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SpaceSettingItem(
                            icon = Icons.Outlined.Pause,
                            title = if (rel.isPausedByMe) "Resume Wallpaper Updates" else "Take Space (Pause)",
                            subtitle = if (rel.isPausedByMe) "You are currently paused" else "Temporarily stop receiving moments",
                            onClick = { viewModel.togglePause() }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                        .clip(RoundedCornerShape(24.dp))
                        .background(White)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SpaceSettingItem(
                            icon = Icons.Outlined.NoMeetingRoom,
                            title = "Say Goodbye (Unpair)",
                            subtitle = "Unpair from ${rel.partner.displayName}",
                            color = ErrorSoft,
                            onClick = { showUnpairDialog = true }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                        .clip(RoundedCornerShape(24.dp))
                        .background(White)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SpaceSettingItem(
                            icon = Icons.Outlined.Flag,
                            title = "Report ${rel.partner.displayName}",
                            subtitle = "Let us know if something's wrong",
                            color = ErrorSoft,
                            onClick = { showReportDialog = true }
                        )
                    }
                }
            }
        } else if (uiState is Resource.Loading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HeartRed)
            }
        }
    }
}

@Composable
fun SpaceSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    color: androidx.compose.ui.graphics.Color = TextDeep,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
