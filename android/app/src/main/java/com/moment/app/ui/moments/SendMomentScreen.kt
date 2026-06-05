package com.moment.app.ui.moments

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moment.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMomentScreen(
    viewModel: SendMomentViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var note by remember { mutableStateOf("") }
    var selectedConnectionId by remember { mutableStateOf<String?>(null) }
    var wallpaperTarget by remember { mutableStateOf("HOME") }
    var expanded by remember { mutableStateOf(false) }

    val sendState by viewModel.sendState.collectAsState()
    val connectionsState by viewModel.connections.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    LaunchedEffect(sendState) {
        if (sendState is Resource.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Moment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Picker
            if (selectedImageUri == null) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Select Image")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    shape = RoundedCornerShape(16.dp),
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection Dropdown
            val connections = connectionsState.data ?: emptyList()
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                val selectedName = connections.find { it.otherUser.id == selectedConnectionId }?.otherUser?.displayName ?: "Select Connection"
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Send to") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (connections.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No active connections") },
                            onClick = { expanded = false }
                        )
                    } else {
                        connections.forEach { conn ->
                            DropdownMenuItem(
                                text = { Text(conn.otherUser.displayName ?: conn.otherUser.username ?: "Unknown") },
                                onClick = {
                                    selectedConnectionId = conn.otherUser.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 250) note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                supportingText = { Text("${note.length}/250") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Selection
            Text("Wallpaper Target", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(selected = wallpaperTarget == "HOME", onClick = { wallpaperTarget = "HOME" }, label = { Text("Home") })
                FilterChip(selected = wallpaperTarget == "LOCK", onClick = { wallpaperTarget = "LOCK" }, label = { Text("Lock") })
                FilterChip(selected = wallpaperTarget == "BOTH", onClick = { wallpaperTarget = "BOTH" }, label = { Text("Both") })
            }

            Spacer(modifier = Modifier.weight(1f))

            if (sendState is Resource.Error) {
                Text(
                    text = sendState.message ?: "Failed to send",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Send Button
            Button(
                onClick = {
                    if (selectedImageUri != null && selectedConnectionId != null) {
                        viewModel.sendMoment(
                            context = context,
                            imageUri = selectedImageUri!!,
                            receiverUserId = selectedConnectionId!!,
                            note = note,
                            wallpaperTarget = wallpaperTarget
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedImageUri != null && selectedConnectionId != null && sendState !is Resource.Loading
            ) {
                if (sendState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Send Moment")
                }
            }
        }
    }
}
