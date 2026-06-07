package com.moment.app.ui.connections

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moment.app.data.remote.ConnectionDto
import com.moment.app.data.remote.ConnectionRequestDto
import com.moment.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    initialInviteCode: String? = null,
    viewModel: ConnectionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val connectionsState by viewModel.connections.collectAsState()
    val pendingRequestsState by viewModel.pendingRequests.collectAsState()
    val inviteState by viewModel.inviteState.collectAsState()
    val inviteInfoState by viewModel.inviteInfo.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (initialInviteCode != null) {
            viewModel.getInviteInfo(initialInviteCode)
        }
    }

    if (inviteInfoState is Resource.Success) {
        val user = inviteInfoState?.data
        if (user != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearInviteInfo() },
                title = { Text("Accept Connection?") },
                text = { Text("Do you want to connect with ${user.displayName ?: user.username}?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.requestConnection(user.id)
                    }) {
                        Text("Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearInviteInfo() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connections") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Invite Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Your Invite Code", style = MaterialTheme.typography.titleMedium)
                        
                        if (inviteState is Resource.Success) {
                            val invite = inviteState.data
                            if (invite != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = invite.inviteCode,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Connect with me on Moment! My code: ${invite.inviteCode} or use this link: ${invite.inviteUrl}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share Code & Link")
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.createInvite() },
                                enabled = inviteState !is Resource.Loading
                            ) {
                                if (inviteState is Resource.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Generate Invite Code")
                                }
                            }
                        }
                    }
                }
            }

            // Enter Code Section
            item {
                var manualCode by remember { mutableStateOf("") }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Connect via Code", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it.uppercase() },
                            label = { Text("Enter 8-digit code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { 
                                        if (manualCode.length >= 8) {
                                            viewModel.getInviteInfo(manualCode)
                                            manualCode = ""
                                        }
                                    },
                                    enabled = manualCode.length >= 8
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            }
                        )
                    }
                }
            }

            // Pending Requests Section
            val pending = (pendingRequestsState as? Resource.Success)?.data ?: emptyList()
            if (pending.isNotEmpty()) {
                item {
                    Text("Pending Requests", style = MaterialTheme.typography.titleLarge)
                }
                items(pending) { request ->
                    PendingRequestItem(
                        request = request,
                        onAccept = { viewModel.respondToRequest(request.id, true) },
                        onDecline = { viewModel.respondToRequest(request.id, false) }
                    )
                }
            }

            // Active Connections Section
            item {
                Text("Your Connections", style = MaterialTheme.typography.titleLarge)
            }

            when (val state = connectionsState) {
                is Resource.Loading -> {
                    item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                }
                is Resource.Error -> {
                    item { Text(text = state.message ?: "Failed to load", color = MaterialTheme.colorScheme.error) }
                }
                is Resource.Success -> {
                    val connections = state.data ?: emptyList()
                    if (connections.isEmpty()) {
                        item { Text("No connections yet.", color = MaterialTheme.colorScheme.secondary) }
                    } else {
                        items(connections) { connection ->
                            ActiveConnectionItem(
                                connection = connection,
                                onRevoke = { viewModel.revokeConnection(connection.targetUserId) }
                            )
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
