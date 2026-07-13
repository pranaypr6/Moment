package com.moment.app.ui.main

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
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
import com.moment.app.util.Resource
import com.moment.app.ui.auth.AuthViewModel
import com.moment.app.widget.RelationshipWidgetReceiver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    spaceSettingsViewModel: com.moment.app.ui.settings.SpaceSettingsViewModel = hiltViewModel(),
    hubViewModel: HubViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit
) {
    val currentUserState by authViewModel.currentUser.collectAsState()
    val profileUpdateState by authViewModel.profileState.collectAsState()
    val spaceState by spaceSettingsViewModel.uiState.collectAsState()
    val momentNotifs by hubViewModel.momentNotifs.collectAsState()
    val reactionNotifs by hubViewModel.reactionNotifs.collectAsState()
    val widgetAlerts by hubViewModel.widgetAlerts.collectAsState()

    HubScreenContent(
        modifier = modifier,
        currentUserState = currentUserState,
        profileUpdateState = profileUpdateState,
        spaceState = spaceState,
        momentNotifs = momentNotifs,
        reactionNotifs = reactionNotifs,
        widgetAlerts = widgetAlerts,
        onFetchProfile = { authViewModel.fetchProfile() },
        onUpdateProfile = { name, uri, ctx -> authViewModel.updateProfile(name, uri, ctx) },
        onLogoutClick = {
            authViewModel.logout()
            onLogout()
        },
        onNavigateToDeleteAccount = onNavigateToDeleteAccount,
        onSetMomentNotifs = { hubViewModel.setMomentNotifs(it) },
        onSetReactionNotifs = { hubViewModel.setReactionNotifs(it) },
        onSetWidgetAlerts = { hubViewModel.setWidgetAlerts(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreenContent(
    modifier: Modifier = Modifier,
    currentUserState: Resource<com.moment.app.data.remote.UserDto>,
    profileUpdateState: Resource<com.moment.app.data.remote.UserDto>,
    spaceState: Resource<com.moment.app.data.remote.RelationshipDto?>,
    momentNotifs: Boolean,
    reactionNotifs: Boolean,
    widgetAlerts: Boolean,
    onFetchProfile: () -> Unit,
    onUpdateProfile: (String, Uri?, android.content.Context) -> Unit,
    onLogoutClick: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onSetMomentNotifs: (Boolean) -> Unit,
    onSetReactionNotifs: (Boolean) -> Unit,
    onSetWidgetAlerts: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showSettingsSheet by remember { mutableStateOf(false) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    var showWidgetModal by remember { mutableStateOf(false) }
    var showWidgetSuccess by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    LaunchedEffect(Unit) {
        onFetchProfile()
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
            title = { Text("Bring us to your Home Screen 🏡", fontWeight = FontWeight.Bold, color = TextDeep) },
            text = { Text("Add our little widget to your phone's home screen so we're always just a glance away! ✨", color = TextMuted) },
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
                    colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                ) { Text("Add Widget") }
            },
            dismissButton = {
                TextButton(onClick = { showWidgetModal = false }) { Text("Cancel", color = TextDeep) }
            },
            containerColor = White,
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
            title = { Text("Success", color = HeartRed) },
            text = { Text("Widget pin requested successfully ❤️", color = TextDeep) },
            confirmButton = {
                TextButton(onClick = { showWidgetSuccess = false }) { Text("OK", color = TextDeep) }
            },
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Take a Break (Logout)?") },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = TextDeep) }
            },
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (isEditingProfile) {
        Dialog(onDismissRequest = { isEditingProfile = false }) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = White,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Profile", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold, 
                        color = TextDeep
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Profile Picture Selection
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SoftCream)
                            .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentAlignment = Alignment.Center
                    ) {
                        val userForEdit = (currentUserState as? Resource.Success)?.data
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!userForEdit?.profilePictureUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userForEdit?.profilePictureUrl,
                                contentDescription = "Current Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Profile",
                                modifier = Modifier.size(48.dp),
                                tint = HeartRed.copy(alpha = 0.5f)
                            )
                        }

                        // Camera Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Picture",
                                tint = White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = editDisplayName,
                        onValueChange = { editDisplayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeartRed,
                            unfocusedBorderColor = WarmBeige
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditingProfile = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateProfile(editDisplayName, selectedImageUri, context)
                                isEditingProfile = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HeartRed),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold, color = White)
                        }
                    }
                }
            }
        }
    }
    
    // Bottom Sheet for Personal Settings
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = White,
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
                    color = TextDeep,
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
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(SuccessSoft),
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
                        color = TextDeep
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HeartRed,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { isEditingProfile = true }.padding(4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Account Actions
                HubActionItem(
                    icon = Icons.Outlined.Logout,
                    title = "Take a Break (Logout)",
                    onClick = { showLogoutDialog = true },
                    tint = TextDeep
                )
                
                HubActionItem(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Delete Account",
                    onClick = onNavigateToDeleteAccount,
                    tint = ErrorSoft,
                    hideChevron = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDeep,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // About
                HubActionItem(
                    icon = Icons.Outlined.Info,
                    title = "Our Story (About Moment)",
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
        containerColor = SoftCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = { },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftCream),
                actions = {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextDeep)
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
                WidgetPreviewHero(
                    currentUser = (currentUserState as? Resource.Success)?.data,
                    relationship = (spaceState as? Resource.Success)?.data,
                    onAddWidgetClick = { showWidgetModal = true }
                )
            }

            // 3. Notifications
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "GENTLE BOUNDARIES",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                        color = TextMuted.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(White)
                    ) {
                        Column {
                            NotificationSettingRow(
                                icon = Icons.Outlined.Notifications,
                                title = "Tell me when they leave a moment",
                                checked = momentNotifs,
                                onCheckedChange = { onSetMomentNotifs(it) }
                            )
                            Divider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                            NotificationSettingRow(
                                icon = Icons.Outlined.FavoriteBorder,
                                title = "Let me know when they love it",
                                checked = reactionNotifs,
                                onCheckedChange = { onSetReactionNotifs(it) }
                            )
                            Divider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                            NotificationSettingRow(
                                icon = Icons.Outlined.Widgets,
                                title = "Alert me about the little things",
                                checked = widgetAlerts,
                                onCheckedChange = { onSetWidgetAlerts(it) }
                            )
                        }
                    }
                }
            }

            // Profile and About sections moved to Settings

        }
    }
}

@Composable
fun BentoActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = White
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp).heightIn(min = 100.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = TextDeep.copy(alpha = 0.05f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, contentDescription = null, tint = TextDeep, modifier = Modifier.padding(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextDeep
            )
        }
    }
}

@Composable
fun NotificationSettingRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextDeep, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextDeep,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f),
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = HeartRed,
                uncheckedThumbColor = White,
                uncheckedTrackColor = Color(0xFFD7CEC8),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun WidgetPreviewHero(
    currentUser: com.moment.app.data.remote.UserDto?,
    relationship: com.moment.app.data.remote.RelationshipDto?,
    onAddWidgetClick: () -> Unit
) {
    val daysTogether = try {
        if (relationship != null) {
            val start = java.time.Instant.parse(relationship.createdAt)
            val now = java.time.Instant.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, now).coerceAtLeast(0)
            if (days == 0L) "Our journey begins." else if (days == 1L) "1 day, still us." else "$days days, still us."
        } else {
            "Our journey begins."
        }
    } catch (e: Exception) {
        "Our journey begins."
    }

    val subtitle = try {
        if (relationship != null) {
            val parseSdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            parseSdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = parseSdf.parse(relationship.createdAt) ?: java.util.Date()
            val outSdf = java.text.SimpleDateFormat("MMMM d '•' yyyy", java.util.Locale.US)
            outSdf.timeZone = java.util.TimeZone.getDefault()
            "SINCE ${outSdf.format(date).uppercase(java.util.Locale.US)}".map { it.toString() }.joinToString("\u2009")
        } else {
            "TODAY".map { it.toString() }.joinToString("\u2009")
        }
    } catch (e: Exception) {
        "TODAY".map { it.toString() }.joinToString("\u2009")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .background(White, RoundedCornerShape(24.dp))
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Relationship Widget",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextDeep
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Widget Preview Box - EXACTLY matching Glance widget style
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(1.8f) // Rectangular shape matching 4x2 widget
                .clip(RoundedCornerShape(24.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF9F5), Color(0xFFFADADD))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Row: Circular Profile Pictures + Long Embedded Heartbeat Center
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialProfileImage(currentUser?.profilePictureUrl)
                    
                    // Heart Divider (Embedded style, longer lines)
                    Row(
                        modifier = Modifier.padding(horizontal = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(38.dp).height(1.dp).background(Color(0xFFD7CEC8)))
                        Text(
                            text = "♥",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFE99EA5),
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Box(modifier = Modifier.width(38.dp).height(1.dp).background(Color(0xFFD7CEC8)))
                    }
                    
                    EditorialProfileImage(relationship?.partner?.profilePictureUrl)
                }
                
                // Hero Text
                Text(
                    text = daysTogether,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFF1F1A18),
                        fontSize = 24.sp, // Reduced by ~8% to increase elegance
                        fontWeight = FontWeight.Medium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                
                // Subtitle Text (Spaced Uppercase)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF8B847C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    modifier = Modifier.padding(bottom = 12.dp) // Perfect gap before reactions
                )

                // Quick Affections Grid (Single Row, perfectly spaced)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmojiImage(com.moment.app.R.drawable.ic_thought_bubble)
                    EmojiImage(com.moment.app.R.drawable.ic_punch_forward)
                    EmojiImage(com.moment.app.R.drawable.ic_cuddling_teddies)
                    EmojiImage(com.moment.app.R.drawable.ic_kiss_face)
                    EmojiImage(com.moment.app.R.drawable.ic_pleading_face)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Stay connected from your home screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddWidgetClick,
            colors = ButtonDefaults.buttonColors(containerColor = HeartRed),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text("Add Widget to Home Screen", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
fun EditorialProfileImage(url: String?) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape) // In case the background image isn't perfectly round
            .border(1.dp, Color.Black.copy(alpha = 0.05f), CircleShape)
            .background(Color.White)
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFEFECE9)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🧑", fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun EmojiImage(iconResId: Int) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
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
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}
