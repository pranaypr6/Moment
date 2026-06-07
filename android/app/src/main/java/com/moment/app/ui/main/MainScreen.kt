package com.moment.app.ui.main

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
import com.moment.app.ui.connections.CircleScreen
import com.moment.app.ui.timeline.TimelineScreen
import com.moment.app.ui.theme.*

sealed class MainTab(val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Moments : MainTab("Moments", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome)
    object Circle : MainTab("Circle", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite)
    object Profile : MainTab("Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun MainScreen(
    initialInviteCode: String? = null,
    onNavigateToSendMoment: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit
) {
    var selectedTab by remember { mutableStateOf<MainTab>(MainTab.Moments) }

    Scaffold(
        containerColor = SoftCream,
        bottomBar = {
            FloatingBottomDock(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                MainTab.Moments -> {
                    TimelineScreen(
                        onNavigateToSendMoment = onNavigateToSendMoment
                    )
                }
                MainTab.Circle -> {
                    CircleScreen(
                        initialInviteCode = initialInviteCode
                    )
                }
                MainTab.Profile -> {
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
fun FloatingBottomDock(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .height(72.dp)
                .fillMaxWidth(0.9f)
                .shadow(12.dp, RoundedCornerShape(36.dp)),
            color = White.copy(alpha = 0.95f),
            shape = RoundedCornerShape(36.dp),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockItem(
                    tab = MainTab.Moments,
                    isSelected = selectedTab == MainTab.Moments,
                    onClick = { onTabSelected(MainTab.Moments) }
                )
                DockItem(
                    tab = MainTab.Circle,
                    isSelected = selectedTab == MainTab.Circle,
                    onClick = { onTabSelected(MainTab.Circle) }
                )
                DockItem(
                    tab = MainTab.Profile,
                    isSelected = selectedTab == MainTab.Profile,
                    onClick = { onTabSelected(MainTab.Profile) }
                )
            }
        }
    }
}

@Composable
fun DockItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) HeartRed else TextMuted
    val backgroundColor = if (isSelected) RoseQuartz.copy(alpha = 0.2f) else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
            contentDescription = tab.title,
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
        AnimatedVisibility(visible = isSelected) {
            Text(
                text = tab.title,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
