package com.pranayburra.moment.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pranayburra.moment.util.Resource

@Composable
fun SplashScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToOnboarding: (String, String) -> Unit,
    onNavigateToMain: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkExistingSession()
    }

    LaunchedEffect(sessionState, currentUser) {
        when (sessionState) {
            is Resource.Success -> {
                // Mirrors LoginScreen's own check: a valid session alone doesn't mean
                // onboarding (setting a username) actually completed - see
                // AuthViewModel.checkExistingSession() for why. Only route to Main once we
                // know the profile is actually complete; if the profile fetch itself failed,
                // currentUser stays non-Success here and we fall back to Main rather than
                // blocking the user on a network hiccup.
                val user = (currentUser as? Resource.Success)?.data
                if (user != null && user.username.isNullOrBlank()) {
                    onNavigateToOnboarding(user.displayName ?: "", user.profilePictureUrl ?: "")
                } else {
                    onNavigateToMain()
                }
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
