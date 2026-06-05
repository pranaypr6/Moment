package com.moment.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moment.app.R
import com.moment.app.util.Resource
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToOnboarding: (String) -> Unit,
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = stringResource(id = R.string.default_web_client_id)
    
    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is Resource.Success) {
            val user = loginState.data?.user
            if (user?.username.isNullOrBlank()) {
                val name = user?.displayName ?: ""
                onNavigateToOnboarding(name)
            } else {
                onNavigateToMain()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Moment",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Leave moments on someone's screen.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = {
                coroutineScope.launch {
                    val idToken = GoogleAuthHelper.signInWithGoogle(context, webClientId)
                    if (idToken != null) {
                        viewModel.loginWithGoogle(idToken)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is Resource.Loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (loginState is Resource.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Sign in with Google")
            }
        }
        
        if (loginState is Resource.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = loginState.message ?: "Sign in failed",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

