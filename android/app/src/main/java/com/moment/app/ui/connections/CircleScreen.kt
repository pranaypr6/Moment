package com.moment.app.ui.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moment.app.ui.theme.*
import com.moment.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleScreen(
    initialInviteCode: String? = null,
    viewModel: ConnectionViewModel = hiltViewModel()
) {
    val connectionsState by viewModel.connections.collectAsState()
    val pendingRequestsState by viewModel.pendingRequests.collectAsState()
    val sentRequestsState by viewModel.sentRequests.collectAsState()
    val inviteState by viewModel.inviteState.collectAsState()
    val inviteInfoState by viewModel.inviteInfo.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        if (initialInviteCode != null) {
            viewModel.getInviteInfo(initialInviteCode)
        }
        viewModel.loadConnections()
        viewModel.loadPendingRequests()
        viewModel.loadSentRequests()
    }

    // Feedback & Toast logic
    LaunchedEffect(inviteInfoState) {
        if (inviteInfoState is Resource.Error) {
            android.widget.Toast.makeText(context, inviteInfoState?.message ?: "Invalid code", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetInviteStates()
        }
    }

    if (inviteInfoState is Resource.Success) {
        val user = inviteInfoState?.data
        val connections = (connectionsState as? Resource.Success)?.data ?: emptyList()
        val sentRequests = (sentRequestsState as? Resource.Success)?.data ?: emptyList()
        
        val isAlreadyConnected = connections.any { it.targetUserId == user?.id }
        val isAlreadySent = sentRequests.any { it.otherUser.id == user?.id }

        if (user != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearInviteInfo() },
                title = { Text(if (isAlreadyConnected || isAlreadySent) "Already Connected" else "Connect with Partner?") },
                text = { 
                    Text(
                        when {
                            isAlreadyConnected -> "You are already connected with ${user.displayName ?: user.username}."
                            isAlreadySent -> "You have already sent a request to ${user.displayName ?: user.username}."
                            else -> "Do you want to send a connection request to ${user.displayName ?: user.username}?"
                        }
                    )
                },
                confirmButton = {
                    if (!isAlreadyConnected && !isAlreadySent) {
                        Button(onClick = {
                            viewModel.requestConnection(user.id)
                            android.widget.Toast.makeText(context, "Request sent!", android.widget.Toast.LENGTH_SHORT).show()
                        }, colors = ButtonDefaults.buttonColors(containerColor = HeartRed)) {
                            Text("Send Request")
                        }
                    } else {
                        Button(onClick = { viewModel.clearInviteInfo() }) {
                            Text("OK")
                        }
                    }
                },
                dismissButton = {
                    if (!isAlreadyConnected && !isAlreadySent) {
                        TextButton(onClick = { viewModel.clearInviteInfo() }) {
                            Text("Cancel", color = TextDeep)
                        }
                    }
                },
                containerColor = White,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }

    Scaffold(
        containerColor = SoftCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Your Circle", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDeep
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftCream)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Pending Section (Horizontal letters style)
            val pending = (pendingRequestsState as? Resource.Success)?.data ?: emptyList()
            if (pending.isNotEmpty()) {
                item {
                    Text("Received Requests", style = MaterialTheme.typography.titleSmall, color = HeartRed, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(pending) { request ->
                            PendingRequestItem(
                                request = request,
                                onAccept = { viewModel.respondToRequest(request.id, true) },
                                onDecline = { viewModel.respondToRequest(request.id, false) }
                            )
                        }
                    }
                }
            }

            // 2. Active Connections (The "Altar")
            item {
                Text("Your People", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDeep)
            }

            when (val state = connectionsState) {
                is Resource.Loading -> {
                    item { CircularProgressIndicator(color = HeartRed) }
                }
                is Resource.Success -> {
                    val connections = state.data ?: emptyList()
                    if (connections.isEmpty() && pending.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(32.dp)
                            ) {
                                Text(
                                    "The space between you is waiting to be filled with memories.",
                                    modifier = Modifier.padding(32.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
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

            // 3. Add Someone Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = White,
                    shape = RoundedCornerShape(32.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Bring someone in", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var manualCode by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it.uppercase() },
                            placeholder = { Text("Enter 8-digit code") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = WarmBeige,
                                focusedBorderColor = HeartRed
                            ),
                            trailingIcon = {
                                if (manualCode.length >= 8) {
                                    IconButton(onClick = { 
                                        viewModel.getInviteInfo(manualCode)
                                        manualCode = ""
                                    }, colors = IconButtonDefaults.iconButtonColors(contentColor = HeartRed)) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Divider(color = WarmBeige)
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Or share your ID", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (inviteState is Resource.Success) {
                            val code = inviteState.data?.inviteCode ?: ""
                            Text(
                                text = code,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = HeartRed,
                                letterSpacing = 4.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { 
                                        clipboardManager.setText(AnnotatedString(code))
                                        android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmBeige),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextDeep, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy", color = TextDeep)
                                }
                                
                                Button(
                                    onClick = { 
                                        val invite = inviteState.data
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, "Connect with me on Moment! My code: ${invite?.inviteCode}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share")
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.createInvite() }, 
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Generate My Code")
                            }
                        }
                    }
                }
            }

            // 4. Sent Requests
            val sent = (sentRequestsState as? Resource.Success)?.data ?: emptyList()
            if (sent.isNotEmpty()) {
                item {
                    Text("Sent Invitations", style = MaterialTheme.typography.titleSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                }
                items(sent) { request ->
                    SentRequestItem(request = request)
                }
            }
        }
    }
}
