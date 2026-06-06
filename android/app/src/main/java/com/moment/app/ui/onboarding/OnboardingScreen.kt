package com.moment.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moment.app.ui.auth.AuthViewModel
import com.moment.app.ui.theme.*
import com.moment.app.util.Resource

@Composable
fun OnboardingScreen(
    initialName: String = "",
    viewModel: AuthViewModel = hiltViewModel(),
    onProfileCreated: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(initialName) }
    val usernameAvailable by viewModel.usernameAvailable.collectAsState()
    val profileState by viewModel.profileState.collectAsState()

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
            Spacer(modifier = Modifier.height(48.dp))
            
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(28.dp),
                color = RoseQuartz.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("👋", fontSize = 32.sp)
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
                text = "Let's set up your profile for your partner.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextDeep.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("How should they call you?") },
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
                    viewModel.createProfile(username, displayName, null, null)
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
