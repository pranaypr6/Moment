package com.moment.app.ui.main

import androidx.compose.material3.MaterialTheme

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.glance.appwidget.GlanceAppWidgetManager
import coil.compose.AsyncImage
import com.moment.app.ui.theme.*
import com.moment.app.ui.moments.ThemePicker
import com.moment.app.util.Resource
import com.moment.app.ui.auth.AuthViewModel
import com.moment.app.ui.settings.SpaceSettingsViewModel
import com.moment.app.widget.RelationshipWidgetReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    spaceSettingsViewModel: SpaceSettingsViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit
) {
    val currentUserState by authViewModel.currentUser.collectAsState()
    val profileUpdateState by authViewModel.profileState.collectAsState()
    val spaceState by spaceSettingsViewModel.uiState.collectAsState()
    val themeId = spaceState.data?.themeId ?: "blush"

    // Removed the manual SoftCream hardcoding for Scaffold/TopBar
    // It will fall back to MaterialTheme.colorScheme.background from MomentTheme
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showSettingsSheet by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    var showWidgetModal by remember { mutableStateOf(false) }
    var showWidgetSuccess by remember { mutableStateOf(false) }
    
    var showRenameDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    LaunchedEffect(Unit) {
        authViewModel.fetchProfile()
    }

    LaunchedEffect(currentUserState) {
        if (currentUserState is Resource.Success) {
            editDisplayName = currentUserState.data?.displayName ?: ""
        }
    }

    LaunchedEffect(profileUpdateState) {
        if (profileUpdateState is Resource.Success) {
            isEditingProfile = false
            selectedImageUri = null
        }
    }
    
    if (showWidgetModal) {
        AlertDialog(
            onDismissRequest = { showWidgetModal = false },
            title = { Text("Moment Widget", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Add this widget to your home screen to stay connected to your partner in one tap.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showWidgetModal = false
                        coroutineScope.launch {
                            try {
                                val manager = GlanceAppWidgetManager(context)
                                val success = manager.requestPinGlanceAppWidget(
                                    RelationshipWidgetReceiver::class.java,
                                    successCallback = null
                                )
                                if (success) {
                                    showWidgetSuccess = true
                                }
                            } catch (e: Exception) {
                                // Ignore or show error
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Add Widget") }
            },
            dismissButton = {
                TextButton(onClick = { showWidgetModal = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    if (showWidgetSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showWidgetSuccess = false
        }
        AlertDialog(
            onDismissRequest = { showWidgetSuccess = false },
            title = { Text("Success", color = MaterialTheme.colorScheme.primary) },
            text = { Text("Widget pin requested successfully ❤️", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(onClick = { showWidgetSuccess = false }) { Text("OK", color = MaterialTheme.colorScheme.onSurface) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Space", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = editNameInput,
                    onValueChange = { editNameInput = it },
                    label = { Text("Space Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HeartRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        spaceSettingsViewModel.updateSpaceName(editNameInput)
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout?") },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (isEditingProfile) {
        AlertDialog(
            onDismissRequest = { isEditingProfile = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeartRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (selectedImageUri != null) "Image Selected" else "Select New Profile Picture")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.updateProfile(editDisplayName, selectedImageUri, context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { isEditingProfile = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Bottom Sheet for Personal Settings
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = WarmBeige) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Account",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val user = (currentUserState as? Resource.Success)?.data
                
                // Profile Header matching attached image
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val imageUrl = user?.profilePictureUrl
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFF7CB342)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.displayName?.take(1)?.uppercase() ?: "U",
                                color = White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user?.displayName ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD6737B), // Muted red/pink from screenshot
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { isEditingProfile = true }.padding(4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Account Actions
                HubActionItem(
                    icon = Icons.Outlined.Logout,
                    title = "Logout",
                    onClick = { showLogoutDialog = true },
                    tint = MaterialTheme.colorScheme.onSurface
                )
                
                HubActionItem(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Delete Account",
                    onClick = onNavigateToDeleteAccount,
                    tint = MaterialTheme.colorScheme.error,
                    hideChevron = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // About
                HubActionItem(
                    icon = Icons.Outlined.Info,
                    title = "About Moment",
                    onClick = { uriHandler.openUri("https://moment-app.com/about") }
                )
                HubActionItem(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy",
                    onClick = { uriHandler.openUri("https://moment-app.com/privacy") }
                )
                HubActionItem(
                    icon = Icons.Outlined.Description,
                    title = "Terms",
                    onClick = { uriHandler.openUri("https://moment-app.com/terms") }
                )
                HubActionItem(
                    icon = Icons.Outlined.Update,
                    title = "Version 1.0.0",
                    onClick = { },
                    hideChevron = true
                )
            }
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            
            // 1. Widget
            item {
                WidgetPreviewHero(onAddWidgetClick = { showWidgetModal = true })
            }

            // 2. Appearance
            item {
                HubSection(title = "Appearance") {
                    Spacer(modifier = Modifier.height(16.dp))
                    ThemePicker(
                        currentThemeId = themeId,
                        onThemeSelected = { newThemeId -> spaceSettingsViewModel.updateTheme(newThemeId) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HubActionItem(
                        icon = Icons.Outlined.Wallpaper,
                        title = "Display Style",
                        onClick = { /* Stub */ }
                    )
                }
            }

            // 3. Notifications
            item {
                HubSection(title = "Notifications") {
                    HubActionItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Moment Notifications",
                        onClick = { /* Stub */ }
                    )
                    HubActionItem(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = "Reaction Notifications",
                        onClick = { /* Stub */ }
                    )
                    HubActionItem(
                        icon = Icons.Outlined.Widgets,
                        title = "Widget Notifications",
                        onClick = { /* Stub */ }
                    )
                }
            }

            // Profile and About sections moved to Settings

        }
    }
}

@Composable
fun HubSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f))
                .clip(RoundedCornerShape(24.dp)),
            color = White
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun HubActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = TextDeep,
    hideChevron: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        if (!hideChevron) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProfileHeroCard(
    user: com.moment.app.data.remote.UserDto?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageUrl = user?.profilePictureUrl
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier.size(56.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user?.displayName?.take(1)?.uppercase() ?: "U",
                    color = White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = user?.displayName ?: "User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "@${user?.username ?: "username"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WidgetPreviewHero(onAddWidgetClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f))
            .background(White, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Relationship Widget",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(com.moment.app.ui.theme.RoseQuartz, com.moment.app.ui.theme.WarmBeige)))
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = White, modifier = Modifier.align(Alignment.Center).size(48.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Stay connected from your home screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddWidgetClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Widget", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
