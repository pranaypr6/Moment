package com.moment.app.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moment.app.ui.auth.AuthViewModel
import com.moment.app.ui.theme.*
import com.moment.app.util.Resource

@Composable
fun OnboardingScreen(
    initialName: String = "",
    initialProfilePictureUrl: String = "",
    viewModel: AuthViewModel = hiltViewModel(),
    onProfileCreated: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(initialName) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val usernameAvailable by viewModel.usernameAvailable.collectAsState()
    val profileState by viewModel.profileState.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    LaunchedEffect(profileState) {
        if (profileState is Resource.Success) {
            onProfileCreated()
        }
    }

    Scaffold(
        containerColor = SoftCream
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Profile Picture Badge/Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(RoseQuartz.copy(alpha = 0.3f))
                        .clickable {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (initialProfilePictureUrl.isNotBlank()) {
                        AsyncImage(
                            model = initialProfilePictureUrl,
                            contentDescription = "Google Profile Picture",
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

                    // Camera Icon Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
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
                
                // Tooltip Bubble
                if (selectedImageUri == null && initialProfilePictureUrl.isBlank()) {
                    Surface(
                        modifier = Modifier
                            .offset(x = 80.dp, y = (-30).dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = HeartRed,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "Add a photo to make\nyour screen beautiful!",
                            color = White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Welcome to\nMoment",
                style = MaterialTheme.typography.displayMedium,
                color = HeartRed,
                fontWeight = FontWeight.Bold,
                lineHeight = 44.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Every photo you send will instantly update their phone's wallpaper. Let's get you ready to share your first moment.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextDeep.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("What does your partner call you?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = SoftRose.copy(alpha = 0.3f),
                    focusedBorderColor = HeartRed,
                    unfocusedContainerColor = White,
                    focusedContainerColor = White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '_' }
                    viewModel.checkUsername(username)
                },
                label = { Text("Pick a unique ID") },
                placeholder = { Text("e.g. alex_2026") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                prefix = { Text("@", color = HeartRed, fontWeight = FontWeight.Bold) },
                isError = usernameAvailable == false && username.isNotEmpty(),
                trailingIcon = {
                    if (username.isNotEmpty()) {
                        when (usernameAvailable) {
                            true -> Icon(Icons.Default.Check, contentDescription = null, tint = SuccessSoft)
                            false -> Icon(Icons.Default.Close, contentDescription = null, tint = ErrorSoft)
                            null -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = HeartRed)
                        }
                    }
                },
                supportingText = {
                    if (usernameAvailable == false && username.isNotEmpty()) {
                        Text("This ID is already taken.")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = SoftRose.copy(alpha = 0.3f),
                    focusedBorderColor = HeartRed,
                    unfocusedContainerColor = White,
                    focusedContainerColor = White
                )
            )

            Spacer(modifier = Modifier.weight(1f))
            
            if (profileState is Resource.Error) {
                Text(
                    text = profileState.message ?: "Failed to create profile",
                    color = ErrorSoft,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    viewModel.createProfileWithImage(
                        username = username,
                        displayName = displayName,
                        bio = null,
                        defaultProfilePictureUrl = initialProfilePictureUrl.ifBlank { null },
                        imageUri = selectedImageUri,
                        context = context
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = usernameAvailable == true && displayName.isNotBlank() && profileState !is Resource.Loading,
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HeartRed,
                    disabledContainerColor = HeartRed.copy(alpha = 0.2f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (profileState is Resource.Loading) {
                    CircularProgressIndicator(
                        color = White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Start Sharing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
