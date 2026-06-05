package com.moment.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moment.app.ui.auth.AuthViewModel

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
        if (profileState is com.moment.app.util.Resource.Success) {
            onProfileCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Welcome to Moment",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Choose a username to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                viewModel.checkUsername(it)
            },
            label = { Text("Username") },
            placeholder = { Text("@username") },
            modifier = Modifier.fillMaxWidth(),
            isError = usernameAvailable == false,
            supportingText = {
                if (usernameAvailable == false) {
                    Text("Username taken, too short, or network error.")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (profileState is com.moment.app.util.Resource.Error) {
            Text(
                text = profileState.message ?: "Failed to create profile",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                viewModel.createProfile(username, displayName, null, null)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = usernameAvailable == true && displayName.isNotBlank() && profileState !is com.moment.app.util.Resource.Loading
        ) {
            if (profileState is com.moment.app.util.Resource.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Complete Profile")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
