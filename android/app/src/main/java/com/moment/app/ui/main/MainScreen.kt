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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
    
    var isBottomBarVisible by androidx.compose.runtime.remember { mutableStateOf(true) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var backPressCount by androidx.compose.runtime.remember { mutableStateOf(0) }
    
    LaunchedEffect(backPressCount) {
        if (backPressCount > 0) {
            kotlinx.coroutines.delay(2000)
            backPressCount = 0
        }
    }

    androidx.activity.compose.BackHandler {
        if (selectedTabTitle != MainTab.Us.title) {
            selectedTabTitle = MainTab.Us.title
        } else {
            if (backPressCount == 0) {
                backPressCount++
                android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    Scaffold(
        containerColor = SoftCream,
        bottomBar = {
            androidx.compose.animation.AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
            ) {
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
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                MainTab.Moments -> {
                    MomentsScreen(
                        onSendMoment = { onNavigateToCamera() },
                        onOverlayVisibilityChanged = { isVisible ->
                            isBottomBarVisible = !isVisible
                        }
                    )
                }
                MainTab.Us -> {
                    UsScreen(
                        onNavigateToPaywall = onNavigateToPaywall,
                        onOverlayVisibilityChanged = { isVisible ->
                            isBottomBarVisible = !isVisible
                        }
                    )
                }
                MainTab.Hub -> {
                    HubScreen(
                        onLogout = onLogout,
                        onNavigateToDeleteAccount = onNavigateToDeleteAccount
                    )
                }
            }
            
            val isOffline by rememberIsOffline()
            Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)) { // Padding for status bar
                OfflineBanner(isOffline = isOffline)
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

@Composable
fun rememberIsOffline(): State<Boolean> {
    val context = LocalContext.current
    val isOffline = remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        
        // Initial check
        val network = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(network)
        isOffline.value = caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOffline.value = false
            }
            override fun onLost(network: Network) {
                isOffline.value = true
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        connectivityManager?.registerNetworkCallback(request, callback)
        
        onDispose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }
    
    return isOffline
}

@Composable
fun OfflineBanner(isOffline: Boolean) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(vertical = 6.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No internet connection",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
