package com.moment.app.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moment.app.ui.auth.ProfileScreen
import com.moment.app.ui.moments.MomentsScreen
import com.moment.app.ui.moments.UsScreen
import com.moment.app.ui.theme.*
import kotlinx.coroutines.launch

sealed class MainTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Moments : MainTab("Moments", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome)
    object Us : MainTab("❤️ Us", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite)
    object Me : MainTab("Me", Icons.Outlined.Person, Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialInviteCode: String? = null,
    viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateToCamera: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToSpaceSettings: () -> Unit
) {
    val appState by viewModel.appState.collectAsState()

    when (val state = appState) {
        is AppState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HeartRed)
            }
        }
        is AppState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Failed to load relationship state", color = ErrorSoft)
            }
        }
        is AppState.None -> {
            com.moment.app.ui.pairing.PairingScreen(
                onCheckStatus = {
                    // Handled inside PairingViewModel but we can also trigger refresh
                }
            )
        }
        is AppState.PostUnpair -> {
            com.moment.app.ui.pairing.PostUnpairScreen(
                relationship = state.relationship,
                onContinue = { viewModel.acknowledgeUnpair() }
            )
        }
        is AppState.Active -> {
            MainTabsContent(
                onNavigateToCamera = onNavigateToCamera,
                onNavigateToEditor = onNavigateToEditor,
                onLogout = onLogout,
                onNavigateToDeleteAccount = onNavigateToDeleteAccount,
                onNavigateToSpaceSettings = onNavigateToSpaceSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsContent(
    onNavigateToCamera: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToSpaceSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf<MainTab>(MainTab.Moments) }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onNavigateToEditor(uri.toString())
            }
        }
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = WarmBeige) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    "Capture a Moment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDeep,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                CaptureOptionItem(
                    icon = Icons.Outlined.PhotoCamera,
                    title = "Take Photo",
                    subtitle = "Capture a spontaneous memory",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                            onNavigateToCamera()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                CaptureOptionItem(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "Choose From Gallery",
                    subtitle = "Pick a special memory",
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showBottomSheet = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                )
            }
        }
    }

    Scaffold(
        containerColor = SoftCream,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                FloatingBottomDock(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
                
                // Primary Action: Premium Pill Button
                // Positioned intentionally above the dock with elegant spacing
                SendMomentPill(
                    modifier = Modifier.padding(bottom = 88.dp), // Precisely floats above the dock
                    onClick = { showBottomSheet = true }
                )
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                MainTab.Moments -> {
                    MomentsScreen()
                }
                MainTab.Us -> {
                    UsScreen(
                        onNavigateToSpaceSettings = onNavigateToSpaceSettings
                    )
                }
                MainTab.Me -> {
                    ProfileScreen(
                        onLogout = onLogout,
                        onNavigateToDeleteAccount = onNavigateToDeleteAccount
                    )
                }
            }
        }
    }
}

@Composable
fun SendMomentPill(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .wrapContentSize()
            .shadow(16.dp, RoundedCornerShape(100.dp)),
        color = HeartRed,
        shape = RoundedCornerShape(100.dp),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Send Moment",
                color = White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun CaptureOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = WarmBeige.copy(alpha = 0.3f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = White
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp), tint = HeartRed)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDeep)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}

@Composable
fun FloatingBottomDock(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .height(68.dp)
            .fillMaxWidth(0.85f)
            .shadow(8.dp, RoundedCornerShape(34.dp)),
        color = White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(34.dp),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                tab = MainTab.Moments,
                isSelected = selectedTab == MainTab.Moments,
                onClick = { onTabSelected(MainTab.Moments) },
                modifier = Modifier.weight(1f)
            )
            DockItem(
                tab = MainTab.Us,
                isSelected = selectedTab == MainTab.Us,
                onClick = { onTabSelected(MainTab.Us) },
                modifier = Modifier.weight(1f)
            )
            DockItem(
                tab = MainTab.Me,
                isSelected = selectedTab == MainTab.Me,
                onClick = { onTabSelected(MainTab.Me) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DockItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isSelected) HeartRed else TextMuted
    val backgroundColor = if (isSelected) RoseQuartz.copy(alpha = 0.15f) else Color.Transparent

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                contentDescription = tab.title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            AnimatedVisibility(visible = isSelected) {
                Text(
                    text = tab.title,
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
