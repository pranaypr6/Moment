package com.moment.app.ui.connections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moment.app.data.remote.ConnectionDto
import com.moment.app.ui.theme.*
import com.moment.app.util.TimeUtils

@Composable
fun ActiveConnectionItem(
    connection: ConnectionDto,
    onRevoke: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Remove Connection?") },
            text = { Text("You will no longer be able to send moments to ${connection.otherUser.displayName ?: connection.otherUser.username}.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRevoke()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HeartRed)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel", color = TextDeep) }
            },
            containerColor = White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = RoseQuartz.copy(alpha = 0.2f)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = HeartRed
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.otherUser.displayName ?: connection.otherUser.username ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDeep
                )
                Text(
                    text = "Connected ${TimeUtils.getRelativeTimeSpan(connection.connectedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextMuted)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove Connection", color = ErrorSoft) },
                        onClick = {
                            showMenu = false
                            showConfirmDialog = true
                        }
                    )
                }
            }
        }
    }
}
