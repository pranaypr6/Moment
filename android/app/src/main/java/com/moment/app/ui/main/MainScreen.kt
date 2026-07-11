package com.moment.app.ui.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moment.app.ui.main.HubScreen
import com.moment.app.ui.moments.MomentsScreen
import com.moment.app.ui.moments.UsScreen
import com.moment.app.ui.theme.*
import kotlinx.coroutines.launch

sealed class MainTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Moments : MainTab("Moments", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome)
    object Us : MainTab("❤️ Us", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite)
    object Hub : MainTab("Hub", Icons.Outlined.GridView, Icons.Filled.GridView)
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
    onNavigateToSpaceSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    externalTargetTab: String? = null,
    onTargetTabConsumed: () -> Unit = {}
) {
    val appState by viewModel.appState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                    viewModel.checkStatus()
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
                onNavigateToSpaceSettings = onNavigateToSpaceSettings,
                onNavigateToPaywall = onNavigateToPaywall,
                externalTargetTab = externalTargetTab,
                onTargetTabConsumed = onTargetTabConsumed
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
    onNavigateToSpaceSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    externalTargetTab: String? = null,
    onTargetTabConsumed: () -> Unit = {}
) {
    var selectedTabTitle by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(MainTab.Us.title) }
    var selectedTab = when (selectedTabTitle) {
        MainTab.Moments.title -> MainTab.Moments
        MainTab.Hub.title -> MainTab.Hub
        else -> MainTab.Us
    }
    
    LaunchedEffect(externalTargetTab) {
        if (externalTargetTab != null) {
            selectedTabTitle = externalTargetTab
            onTargetTabConsumed()
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
                    onTabSelected = { selectedTabTitle = it.title }
                )
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                MainTab.Moments -> {
                    MomentsScreen(
                        onSendMoment = { onNavigateToCamera() }
                    )
                }
                MainTab.Us -> {
                    UsScreen(
                        onNavigateToPaywall = onNavigateToPaywall
                    )
                }
                MainTab.Hub -> {
                    HubScreen(
                        onLogout = onLogout,
                        onNavigateToDeleteAccount = onNavigateToDeleteAccount
                    )
                }
            }
        }
    }
}


// Removed CaptureOptionItem

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
                tab = MainTab.Hub,
                isSelected = selectedTab == MainTab.Hub,
                onClick = { onTabSelected(MainTab.Hub) },
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
            if (tab == MainTab.Us) {
                UsBrandMark(isSelected = isSelected, color = contentColor)
            } else {
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
}

@Composable
fun UsBrandMark(isSelected: Boolean, color: Color) {
    // Pulse animation for the heart
    val heartScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    // Inward shift for letters
    val letterOffset by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 300)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // "U"
        Text(
            text = "U",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(x = letterOffset)
                .rotate(10f) // Leans inward (rightward)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Icon(
            imageVector = if (isSelected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Us Tab",
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .scale(heartScale)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // "S"
        Text(
            text = "S",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(x = -letterOffset)
                .rotate(-10f) // Leans inward (leftward)
        )
    }
}
