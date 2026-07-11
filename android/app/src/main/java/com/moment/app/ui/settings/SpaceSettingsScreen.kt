package com.moment.app.ui.settings

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
import com.moment.app.util.Resource
import com.moment.app.ui.theme.*
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.moment.app.widget.RelationshipWidgetReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceSettingsScreen(
    viewModel: SpaceSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    
    var showUnpairDialog by remember { mutableStateOf(false) }

    val rel = (uiState as? Resource.Success)?.data
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                        onNavigateBack()
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
                        SpaceSettingItem(
                            icon = Icons.Outlined.ColorLens,
                            title = "Our Colors",
                            subtitle = rel.themeId.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                            onClick = { /* TODO: Theme Picker */ }
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
