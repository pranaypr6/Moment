package com.moment.app.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.moment.app.ui.connections.ActiveConnectionItem
import com.moment.app.ui.connections.ConnectionViewModel
import com.moment.app.ui.connections.PendingRequestItem
import com.moment.app.ui.connections.SentRequestItem
import com.moment.app.ui.theme.*
import com.moment.app.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    initialInviteCode: String? = null,
    viewModel: ConnectionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val connectionsState by viewModel.connections.collectAsState()
    val pendingRequestsState by viewModel.pendingRequests.collectAsState()
    val sentRequestsState by viewModel.sentRequests.collectAsState()
    val inviteState by viewModel.inviteState.collectAsState()
    val inviteInfoState by viewModel.inviteInfo.collectAsState()
    val currentUserState by authViewModel.currentUser.collectAsState()
    val profileUpdateState by authViewModel.profileState.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var isEditing by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    LaunchedEffect(Unit) {
        authViewModel.fetchProfile()
        if (initialInviteCode != null) {
            viewModel.getInviteInfo(initialInviteCode)
        }
    }

    LaunchedEffect(currentUserState) {
        if (currentUserState is Resource.Success) {
            editDisplayName = currentUserState.data?.displayName ?: ""
        }
    }

    LaunchedEffect(profileUpdateState) {
        if (profileUpdateState is Resource.Success) {
            isEditing = false
            selectedImageUri = null
        }
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
                title = { Text("Profile", style = MaterialTheme.typography.titleLarge, color = TextDeep) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDeep)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Text("Logout", color = ErrorSoft, fontWeight = FontWeight.Bold)
                    }
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // 1. Identity & Edit Section
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = White,
                    shape = RoundedCornerShape(32.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(RoseQuartz.copy(alpha = 0.4f))
                                .clickable {
                                    if (isEditing) photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            val user = (currentUserState as? Resource.Success)?.data
                            val imageUrl = selectedImageUri ?: user?.profilePictureUrl
                            
                            if (imageUrl != null) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                                    tint = HeartRed
                                )
                            }
                            
                            if (isEditing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = White)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editDisplayName,
                                onValueChange = { editDisplayName = it },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { 
                                    isEditing = false
                                    selectedImageUri = null
                                    editDisplayName = (currentUserState as? Resource.Success)?.data?.displayName ?: ""
                                }) {
                                    Text("Cancel", color = TextMuted)
                                }
                                Button(
                                    onClick = { 
                                        authViewModel.updateProfile(editDisplayName, selectedImageUri, context)
                                    },
                                    enabled = profileUpdateState !is Resource.Loading,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (profileUpdateState is Resource.Loading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Save")
                                    }
                                }
                            }
                        } else {
                            val user = (currentUserState as? Resource.Success)?.data
                            Text(
                                text = user?.displayName ?: user?.username ?: "Your Name",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDeep
                            )
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            TextButton(onClick = { isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile")
                            }
                        }
                    }
                }
            }

            // 2. Invite Section
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = White,
                    shape = RoundedCornerShape(32.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Your Moment ID",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDeep
                        )
                        
                        if (inviteState is Resource.Success) {
                            val code = inviteState.data?.inviteCode ?: ""
                            Text(
                                text = code,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = HeartRed,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { 
                                        clipboardManager.setText(AnnotatedString(code))
                                        android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
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
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share")
                                }
                            }
                        } else {
                            Button(onClick = { viewModel.createInvite() }, shape = RoundedCornerShape(16.dp)) {
                                Text("Generate My Code")
                            }
                        }
                    }
                }
            }

            // 3. Add via Code Section
            item {
                var manualCode by remember { mutableStateOf("") }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = White,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Connect with someone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
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
                    }
                }
            }

            // 4. Relationships
            item {
                Text("Your Circle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDeep)
            }

            // Incoming Pending
            val pending = (pendingRequestsState as? Resource.Success)?.data ?: emptyList()
            if (pending.isNotEmpty()) {
                item {
                    Text("Received Requests", style = MaterialTheme.typography.titleSmall, color = HeartRed, fontWeight = FontWeight.Bold)
                }
                items(pending) { request ->
                    PendingRequestItem(
                        request = request,
                        onAccept = { viewModel.respondToRequest(request.id, true) },
                        onDecline = { viewModel.respondToRequest(request.id, false) }
                    )
                }
            }

            // Outgoing Pending
            val sent = (sentRequestsState as? Resource.Success)?.data ?: emptyList()
            if (sent.isNotEmpty()) {
                item {
                    Text("Sent Requests", style = MaterialTheme.typography.titleSmall, color = TextMuted, fontWeight = FontWeight.Bold)
                }
                items(sent) { request ->
                    SentRequestItem(request = request)
                }
            }

            // Active
            when (val state = connectionsState) {
                is Resource.Loading -> {
                    item { 
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = HeartRed) 
                        }
                    }
                }
                is Resource.Success -> {
                    val connections = state.data ?: emptyList()
                    if (connections.isEmpty() && pending.isEmpty() && sent.isEmpty()) {
                        item {
                            Text(
                                "Your circle is empty. Invite your partner to get started.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                            )
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
        }
    }
}
