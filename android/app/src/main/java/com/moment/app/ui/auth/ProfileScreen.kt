package com.moment.app.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import coil.compose.AsyncImage
import com.moment.app.ui.theme.*
import com.moment.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit
) {
    val currentUserState by authViewModel.currentUser.collectAsState()
    val profileUpdateState by authViewModel.profileState.collectAsState()
    
    val context = LocalContext.current
    
    var isEditing by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

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
            isEditing = false
            selectedImageUri = null
        }
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
    
    Scaffold(
        containerColor = SoftCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextDeep) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftCream)
            )
        }
    ) { paddingValues ->
        if (currentUserState is Resource.Loading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HeartRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // 1. Identity & Edit Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                            .clip(RoundedCornerShape(32.dp))
                            .background(White)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(RoseQuartz.copy(alpha = 0.4f))
                                    .clickable {
                                        if (isEditing) photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                val user = (currentUserState as? Resource.Success)?.data
                                val imageUrl = selectedImageUri ?: user?.profilePictureUrl
                                
                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.padding(24.dp).fillMaxSize(),
                                        tint = HeartRed
                                    )
                                }
                                
                                if (isEditing) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = White)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            if (isEditing) {
                                OutlinedTextField(
                                    value = editDisplayName,
                                    onValueChange = { editDisplayName = it },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = HeartRed,
                                        unfocusedBorderColor = WarmBeige
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { 
                                        isEditing = false
                                        selectedImageUri = null
                                        editDisplayName = (currentUserState as? Resource.Success)?.data?.displayName ?: ""
                                    }) {
                                        Text("Cancel", color = TextMuted)
                                    }
                                    Button(
                                        onClick = { 
                                            authViewModel.updateProfile(editDisplayName, selectedImageUri, context)
                                        },
                                        enabled = profileUpdateState !is Resource.Loading,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (profileUpdateState is Resource.Loading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
                                        } else {
                                            Text("Save")
                                        }
                                    }
                                }
                            } else {
                                val user = (currentUserState as? Resource.Success)?.data
                                Text(
                                    text = user?.displayName ?: user?.username ?: "Your Name",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDeep
                                )
                                Text(
                                    text = user?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                TextButton(onClick = { isEditing = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit Profile")
                                }
                            }
                        }
                    }
                }

                // Removed pairing settings because they moved to PairingScreen and SpaceSettings

                // 2. Account Options
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                            .clip(RoundedCornerShape(24.dp))
                            .background(White)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            SettingsItem(
                                icon = Icons.Outlined.Notifications,
                                title = "Notifications",
                                onClick = { /* TODO */ }
                            )
                            SettingsItem(
                                icon = Icons.Outlined.Shield,
                                title = "Privacy & Security",
                                onClick = { /* TODO */ }
                            )
                            SettingsItem(
                                icon = Icons.Outlined.Info,
                                title = "About Moment",
                                onClick = { /* TODO */ }
                            )
                        }
                    }
                }

                // 3. Danger Zone
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Transparent)
                            .clip(RoundedCornerShape(24.dp))
                            .background(White)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            SettingsItem(
                                icon = Icons.Outlined.Logout,
                                title = "Logout",
                                color = TextDeep,
                                onClick = { showLogoutDialog = true }
                            )
                            SettingsItem(
                                icon = Icons.Outlined.DeleteForever,
                                title = "Delete Account",
                                color = ErrorSoft,
                                onClick = onNavigateToDeleteAccount
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    color: Color = TextDeep,
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
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WarmBeige)
    }
}
