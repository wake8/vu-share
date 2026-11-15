package com.vunnam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunnam.network.NetworkUtils
import com.vunnam.network.P2pSocketTransferEngine
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun IncomingProposalDialog(
    proposal: P2pSocketTransferEngine.SocketTransferProposal,
    availableStorageBytes: Long = Long.MAX_VALUE,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val colors = VuShareTheme.colors
    val hasEnoughSpace = availableStorageBytes == Long.MAX_VALUE || availableStorageBytes >= proposal.totalBytes

    AlertDialog(
        onDismissRequest = onReject,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.elevatedSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colors.primaryAccent.copy(alpha = 0.16f))
                        .border(1.5.dp, colors.primaryAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = proposal.senderDevice.avatarEmoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Incoming Transfer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText
                    )
                    Text(
                        text = "From: ${proposal.senderDevice.nickname}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primaryAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This device wants to send you ${proposal.items.size} file(s) (${NetworkUtils.formatFileSize(proposal.totalBytes)}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.secondaryCard)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Safe",
                        tint = colors.success,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Single-pass SHA-256 verified & sanitized",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryText
                    )
                }
                if (availableStorageBytes != Long.MAX_VALUE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (hasEnoughSpace) colors.secondaryCard else colors.errorContainer)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Storage",
                            tint = if (hasEnoughSpace) colors.primaryAccent else colors.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasEnoughSpace) {
                                "Available space: ${NetworkUtils.formatFileSize(availableStorageBytes)}"
                            } else {
                                "Insufficient space (${NetworkUtils.formatFileSize(availableStorageBytes)} free)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasEnoughSpace) colors.secondaryText else colors.error,
                            fontWeight = if (hasEnoughSpace) FontWeight.Normal else FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = hasEnoughSpace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.onPrimaryAccent,
                    disabledContainerColor = colors.disabledSurface,
                    disabledContentColor = colors.disabledText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("accept_transfer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Accept", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("reject_transfer_button")
            ) {
                Text("Reject", color = colors.error, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
