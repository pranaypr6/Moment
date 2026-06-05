package com.moment.app.ui.connections

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.moment.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    initialInviteCode: String? = null,
    viewModel: ConnectionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val connectionsState by viewModel.connections.collectAsState()
    val inviteState by viewModel.inviteState.collectAsState()
    val inviteInfoState by viewModel.inviteInfo.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadConnections()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Invite Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Invite a Friend", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.createInvite() },
                        enabled = inviteState !is Resource.Loading
                    ) {
                        if (inviteState is Resource.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Generate Invite Link")
                        }
                    }

                    if (inviteState is Resource.Success) {
                        val inviteUrl = inviteState.data?.inviteUrl
                        if (inviteUrl != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(inviteUrl, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Connect with me on Moment! $inviteUrl")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                        }
                    } else if (inviteState is Resource.Error) {
                        Text(text = inviteState.message ?: "Error", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Your Connections", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = connectionsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is Resource.Error -> {
                    Text(text = state.message ?: "Failed to load", color = MaterialTheme.colorScheme.error)
                }
                is Resource.Success -> {
                    val connections = state.data ?: emptyList()
                    if (connections.isEmpty()) {
                        Text("No connections yet.", color = MaterialTheme.colorScheme.secondary)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(connections) { connection ->
                                ConnectionItem(
                                    connection = connection,
                                    onAccept = { viewModel.respondToRequest(connection.id, true) },
                                    onDecline = { viewModel.respondToRequest(connection.id, false) }
                                )
                            }
                        }
                    }
                }
                is Resource.Idle -> {}
            }
        }
    }
}

@Composable
fun ConnectionItem(
    connection: ConnectionDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = connection.otherUser.displayName ?: connection.otherUser.username ?: "Unknown User", fontWeight = FontWeight.Bold)
                Text(text = "Status: ${connection.status}", style = MaterialTheme.typography.bodySmall)
            }

            if (connection.status == "PENDING") {
                if (!connection.isRequester) {
                    Row {
                        TextButton(onClick = onDecline) { Text("Decline", color = MaterialTheme.colorScheme.error) }
                        Button(onClick = onAccept) { Text("Accept") }
                    }
                } else {
                    Text("Request Sent", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
