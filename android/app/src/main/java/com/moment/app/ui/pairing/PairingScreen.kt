package com.moment.app.ui.pairing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moment.app.ui.theme.HeartRed
import com.moment.app.ui.theme.SoftCream
import com.moment.app.ui.theme.TextDeep
import com.moment.app.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    viewModel: PairingViewModel = hiltViewModel(),
    onCheckStatus: () -> Unit // called to refresh the relationship status
) {
    val state by viewModel.pairingState.collectAsState()
    var isJoining by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        containerColor = SoftCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Our Space", fontWeight = FontWeight.Bold, color = TextDeep) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftCream)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val currentState = state) {
                is PairingState.Idle, is PairingState.Error -> {
                    Text(
                        text = "Moment is for two.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDeep,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Create a private space to share your background with someone special.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    if (currentState is PairingState.Error) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    if (!isJoining) {
                        Button(
                            onClick = { viewModel.createPairingKey() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                        ) {
                            Text("Create Our Space")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = { isJoining = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("I have an invite code", color = TextDeep)
                        }
                    } else {
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.uppercase() },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter pairing key") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.joinRelationship(joinCode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HeartRed),
                            enabled = joinCode.isNotBlank()
                        ) {
                            Text("Join Space")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { 
                                isJoining = false 
                                viewModel.resetState()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", color = TextMuted)
                        }
                    }
                }
                is PairingState.Loading -> {
                    CircularProgressIndicator(color = HeartRed)
                }
                is PairingState.Created -> {
                    Text(
                        text = "Your Pairing Key",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDeep
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = currentState.pairingKey,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = HeartRed,
                            modifier = Modifier.padding(32.dp),
                            letterSpacing = 4.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentState.pairingKey))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Key")
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = "Waiting for your partner to join...",
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onCheckStatus() },
                        colors = ButtonDefaults.textButtonColors(contentColor = HeartRed)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I've been paired!")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { viewModel.resetState() }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
                is PairingState.Joined -> {
                    Text(
                        text = "Successfully Joined!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = HeartRed
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = HeartRed)
                }
            }
        }
    }
}
