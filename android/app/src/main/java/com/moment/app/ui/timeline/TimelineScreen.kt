package com.moment.app.ui.timeline

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moment.app.data.remote.MomentDto
import com.moment.app.ui.theme.*
import com.moment.app.util.Resource
import com.moment.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel(),
    onNavigateToSendMoment: () -> Unit,
    onNavigateToConnections: () -> Unit
) {
    val timelineState by viewModel.timelineState.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val context = LocalContext.current
    
    var isBatteryOptimized by remember { 
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName))
    }

    LaunchedEffect(Unit) {
        viewModel.loadTimeline(refresh = true)
    }

    Scaffold(
        containerColor = SoftCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Moment", 
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeartRed,
                        letterSpacing = (-1).sp
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToConnections,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RoseQuartz.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = HeartRed, modifier = Modifier.padding(8.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SoftCream
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onNavigateToSendMoment,
                containerColor = HeartRed,
                contentColor = White,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Send Moment", modifier = Modifier.size(36.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isBatteryOptimized) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    color = RoseQuartz.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = HeartRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Make moments instant", style = MaterialTheme.typography.titleSmall, color = TextDeep)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "To receive surprises instantly, set battery to 'Unrestricted'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDeep.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { isBatteryOptimized = false }) { 
                                Text("Later", color = TextDeep.copy(alpha = 0.5f)) 
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                            ) { Text("Set Now") }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = timelineState) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = HeartRed, strokeWidth = 3.dp)
                    }
                    is Resource.Error -> {
                        Text(
                            text = state.message ?: "Could not load moments",
                            color = ErrorSoft,
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is Resource.Success -> {
                        val moments = state.data?.moments ?: emptyList()
                        if (moments.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(100.dp),
                                    shape = CircleShape,
                                    color = RoseQuartz.copy(alpha = 0.2f)
                                ) {
                                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.padding(24.dp), tint = HeartRed.copy(alpha = 0.3f))
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Waiting for the first moment...",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextDeep.copy(alpha = 0.8f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Invite your partner to fill this space with memories.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextDeep.copy(alpha = 0.5f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp, start = 20.dp, end = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                items(moments) { moment ->
                                    MomentCard(moment = moment, currentUserId = currentUserId)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun MomentCard(moment: MomentDto, currentUserId: String?) {
    var isLoading by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp),
        shape = RoundedCornerShape(40.dp),
        color = White,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = moment.imageUrl,
                contentDescription = "Wallpaper Moment",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { isLoading = false }
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WarmBeige)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                        strokeWidth = 2.dp,
                        color = HeartRed
                    )
                }
            }

            // Soft, romantic vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(28.dp)
            ) {
                val isSender = currentUserId != null && currentUserId == moment.sender.id
                val contextText = if (isSender) {
                    "Shared with ${moment.receiver.displayName ?: moment.receiver.username}"
                } else {
                    "Shared by ${moment.sender.displayName ?: moment.sender.username}"
                }

                Text(
                    text = contextText,
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = TimeUtils.getRelativeTimeSpan(moment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal
                )

                if (!moment.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = moment.note,
                        style = MaterialTheme.typography.bodyLarge,
                        color = White.copy(alpha = 0.9f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 24.sp
                    )
                }
            }
            
            // Status Badge - Heart style
            Surface(
                color = White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotColor = when(moment.status) {
                        "APPLIED" -> SuccessSoft
                        "FAILED" -> ErrorSoft
                        else -> HeartRed
                    }
                    Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = moment.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDeep,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
