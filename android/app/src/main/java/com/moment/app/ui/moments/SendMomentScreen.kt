package com.moment.app.ui.moments

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moment.app.ui.theme.*
import com.moment.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMomentScreen(
    initialImageUri: Uri,
    viewModel: SendMomentViewModel = hiltViewModel(),
    onFinish: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    var wallpaperTarget by remember { mutableStateOf("HOME") }

    val sendState by viewModel.sendState.collectAsState()

    LaunchedEffect(sendState) {
        if (sendState is Resource.Success) {
            com.moment.app.util.HapticFeedbackManager.playSuccess(context)
            viewModel.resetState()
            onFinish()
        }
    }

    Scaffold(
        containerColor = SoftCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Send with Love", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDeep) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDeep)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftCream)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview Card - More portrait oriented
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f/16f) // Force portrait aspect ratio for wallpaper preview
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(32.dp),
                color = White,
                shadowElevation = 4.dp
            ) {
                AsyncImage(
                    model = initialImageUri,
                    contentDescription = "Edited Moment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Note
            Text(
                "Add a whisper (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextDeep,
                modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 250) note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add a little note...") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HeartRed,
                    unfocusedBorderColor = WarmBeige,
                    focusedContainerColor = White,
                    unfocusedContainerColor = White
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Target Selection
            Text(
                "Wallpaper Target",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextDeep,
                modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TargetChip(
                    title = "Home",
                    isSelected = wallpaperTarget == "HOME",
                    onClick = { wallpaperTarget = "HOME" },
                    modifier = Modifier.weight(1f)
                )
                TargetChip(
                    title = "Lock",
                    isSelected = wallpaperTarget == "LOCK",
                    onClick = { wallpaperTarget = "LOCK" },
                    modifier = Modifier.weight(1f)
                )
                TargetChip(
                    title = "Both",
                    isSelected = wallpaperTarget == "BOTH",
                    onClick = { wallpaperTarget = "BOTH" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (sendState is Resource.Error) {
                Text(
                    text = (sendState as Resource.Error).message ?: "Failed to send",
                    color = ErrorSoft,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Send Button
            Button(
                onClick = {
                    viewModel.sendMoment(
                        context = context,
                        imageUri = initialImageUri,
                        note = note,
                        wallpaperTarget = wallpaperTarget
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = sendState !is Resource.Loading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
            ) {
                if (sendState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send to their screen", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun TargetChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) HeartRed else White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, WarmBeige)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) White else TextDeep,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
