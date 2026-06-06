package com.moment.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moment.app.R
import com.moment.app.ui.theme.HeartRed
import com.moment.app.ui.theme.SoftCream
import com.moment.app.ui.theme.RoseQuartz
import com.moment.app.ui.theme.TextDeep
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftCream)
    ) {
        // Soft, organic gradient blobs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(RoseQuartz.copy(alpha = 0.4f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = 1000f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Moment",
                    style = MaterialTheme.typography.displayLarge,
                    color = HeartRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "A shared space for just the two of you.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextDeep.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val idToken = GoogleAuthHelper.signInWithGoogle(context, webClientId)
                            if (idToken != null) {
                                viewModel.loginWithGoogle(idToken)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    enabled = loginState !is Resource.Loading,
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeartRed,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (loginState is Resource.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Get Started",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Secure sign-in with Google",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextDeep.copy(alpha = 0.4f)
                )

                if (loginState is Resource.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = loginState.message ?: "Something went wrong",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(
                text = "Privacy first. Always.",
                style = MaterialTheme.typography.labelSmall,
                color = TextDeep.copy(alpha = 0.3f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
