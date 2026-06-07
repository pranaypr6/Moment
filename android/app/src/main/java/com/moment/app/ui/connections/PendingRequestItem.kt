package com.moment.app.ui.connections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moment.app.data.remote.ConnectionRequestDto

@Composable
fun PendingRequestItem(
    request: ConnectionRequestDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = request.otherUser.displayName ?: request.otherUser.username ?: "Unknown User", fontWeight = FontWeight.Bold)
                Text(text = "wants to connect", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                TextButton(onClick = onDecline) { Text("Decline", color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = onAccept,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) { Text("Accept") }
            }
        }
    }
}
