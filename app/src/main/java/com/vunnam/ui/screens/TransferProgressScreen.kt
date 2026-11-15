package com.vunnam.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunnam.model.TransferDirection
import com.vunnam.model.TransferStatus
import com.vunnam.network.NetworkUtils
import com.vunnam.ui.MainViewModel
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun TransferProgressScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val session by viewModel.activeSession.collectAsState()

    if (session == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No active transfer session",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent,
                        contentColor = colors.onPrimaryAccent
                    )
                ) {
                    Text("Return Home", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val currentSession = session!!
    val isSending = currentSession.direction == TransferDirection.SEND
    val animatedProgress by animateFloatAsState(
        targetValue = currentSession.overallProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "overall_progress"
    )

    val targetDevice = currentSession.targetDevices.firstOrNull()
    val isCompleted = currentSession.status == TransferStatus.COMPLETED
    val isFailed = currentSession.status == TransferStatus.FAILED || currentSession.status == TransferStatus.REJECTED
    val isCancelled = currentSession.status == TransferStatus.CANCELLED
    val isPaused = currentSession.status == TransferStatus.PAUSED || currentSession.isPaused
    val isTerminal = isCompleted || isFailed || isCancelled

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // App Bar / Target Device Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background((if (isSending) colors.primaryAccent else colors.secondaryHue).copy(alpha = 0.18f))
                    .border(1.5.dp, if (isSending) colors.primaryAccent else colors.secondaryHue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = targetDevice?.avatarEmoji ?: "📱", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        currentSession.isGroupTransfer -> "Group Broadcast (${currentSession.targetDevices.size} peers)"
                        isSending -> "Sending to ${targetDevice?.nickname ?: "Peer"}"
                        else -> "Receiving from ${targetDevice?.nickname ?: "Peer"}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSession.errorMessage ?: when {
                        isCompleted -> "All files transferred successfully!"
                        isCancelled -> "Transfer cancelled"
                        isFailed -> "Transfer failed or was rejected"
                        isPaused -> "Transfer paused"
                        currentSession.status == TransferStatus.CONNECTING -> "Connecting..."
                        else -> "High-speed peer-to-peer transfer"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isCompleted -> colors.success
                        isFailed || isCancelled -> colors.error
                        isPaused -> colors.warning
                        else -> colors.primaryAccent
                    }
                )
            }

            if (!isTerminal) {
                OutlinedButton(
                    onClick = { viewModel.cancelActiveTransfer() },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("cancel_transfer_button")
                ) {
                    Text("Cancel", color = colors.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Big Hero Progress Widget
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = when {
                            isCompleted -> colors.success
                            isFailed || isCancelled -> colors.error
                            isPaused -> colors.warning
                            else -> colors.progressIndicator
                        },
                        trackColor = colors.progressTrack,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = colors.success,
                                modifier = Modifier.size(42.dp)
                            )
                        } else {
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = colors.primaryText
                            )
                            if (!isTerminal && currentSession.etaSeconds > 0L) {
                                Text(
                                    text = NetworkUtils.formatEta(currentSession.etaSeconds),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Row: Speed, Total Bytes, Integrity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Speed",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText
                            )
                        }
                        Text(
                            text = when {
                                isCompleted -> "Done"
                                isPaused -> "Paused"
                                else -> NetworkUtils.formatSpeed(currentSession.speedBps)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Transferred",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryText
                        )
                        Text(
                            text = "${NetworkUtils.formatFileSize(currentSession.transferredBytes)} / ${NetworkUtils.formatFileSize(currentSession.totalBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = colors.success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHA-256",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText
                            )
                        }
                        Text(
                            text = if (isFailed) "Check" else "Verified",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFailed) colors.warning else colors.success
                        )
                    }
                }

                // Pause / Resume control while transferring
                if (!isTerminal && currentSession.status != TransferStatus.CONNECTING) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            if (isPaused) viewModel.resumeActiveTransfer() else viewModel.pauseActiveTransfer()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.primaryAccent),
                        modifier = Modifier.testTag("pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = colors.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "Resume Transfer" else "Pause Transfer",
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Group Recipients Breakdown (when broadcasting to 2+ devices)
        if (currentSession.isGroupTransfer && currentSession.deviceProgress.isNotEmpty()) {
            Text(
                text = "Recipients (${currentSession.deviceProgress.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currentSession.deviceProgress.values.forEach { devProg ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.secondaryCard),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${devProg.avatarEmoji} ${devProg.deviceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = devProg.errorMessage ?: when (devProg.status) {
                                        TransferStatus.COMPLETED -> "Done"
                                        TransferStatus.FAILED -> "Failed"
                                        TransferStatus.REJECTED -> "Rejected"
                                        TransferStatus.CANCELLED -> "Cancelled"
                                        TransferStatus.PAUSED -> "Paused"
                                        TransferStatus.CONNECTING -> "Connecting"
                                        else -> "${(devProg.progressPercent * 100).toInt()}% • ${NetworkUtils.formatSpeed(devProg.speedBps)}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (devProg.status) {
                                        TransferStatus.COMPLETED -> colors.success
                                        TransferStatus.FAILED, TransferStatus.REJECTED, TransferStatus.CANCELLED -> colors.error
                                        TransferStatus.PAUSED -> colors.warning
                                        else -> colors.primaryAccent
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { devProg.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = when (devProg.status) {
                                    TransferStatus.COMPLETED -> colors.success
                                    TransferStatus.FAILED, TransferStatus.REJECTED -> colors.error
                                    else -> colors.progressIndicator
                                },
                                trackColor = colors.progressTrack,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        // File Transfer Checklist
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Files (${currentSession.items.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentSession.items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.primaryCard),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (item.status) {
                                    TransferStatus.COMPLETED -> Icons.Default.CheckCircle
                                    TransferStatus.FAILED -> Icons.Default.Error
                                    else -> Icons.Default.HourglassTop
                                },
                                contentDescription = null,
                                tint = when (item.status) {
                                    TransferStatus.COMPLETED -> colors.success
                                    TransferStatus.FAILED -> colors.error
                                    TransferStatus.TRANSFERRING -> colors.primaryAccent
                                    else -> colors.secondaryText
                                },
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${NetworkUtils.formatFileSize(item.bytesTransferred)} / ${NetworkUtils.formatFileSize(item.fileSize)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.secondaryText
                                )
                            }

                            if (item.verifiedChecksum) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Verified",
                                    tint = colors.success,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (item.status == TransferStatus.TRANSFERRING || item.status == TransferStatus.PAUSED) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { item.progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (item.status == TransferStatus.PAUSED) colors.warning else colors.progressIndicator,
                                trackColor = colors.progressTrack,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        // Completion / Retry Action Dock
        if (isTerminal) {
            Surface(
                color = colors.elevatedSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSending && (isFailed || isCancelled)) {
                        OutlinedButton(
                            onClick = { viewModel.retryLastTransfer() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.primaryAccent),
                            modifier = Modifier.testTag("retry_transfer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", color = colors.primaryAccent, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.resetTransferSession()
                            onDone()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("transfer_done_button")
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
