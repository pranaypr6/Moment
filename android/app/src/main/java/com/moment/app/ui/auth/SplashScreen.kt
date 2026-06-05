package com.moment.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
            is Resource.Success -> onNavigateToMain()
            is Resource.Error -> onNavigateToLogin()
            else -> {} // Idle or Loading, do nothing
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Will replace this with animated logo in UI redesign phase
        Text(
            text = "MOMENT",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}
