package com.moment.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.moment.app.util.Resource

@Composable
fun SplashScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.checkExistingSession()
    }

    LaunchedEffect(sessionState) {
        when (sessionState) {
            is Resource.Success -> {
                onNavigateToMain()
            }
            is Resource.Error -> {
                onNavigateToLogin()
            }
            else -> {}
        }
    }

    // A simple blank background that matches the theme so the transition is seamless
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}
