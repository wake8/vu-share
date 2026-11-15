package com.vunnam.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vunnam.model.TransferDirection
import com.vunnam.model.TransferStatus

@Entity(tableName = "transfer_history")
data class TransferRecordEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val fileName: String,
    val filePath: String?,
    val fileSize: Long,
    val mimeType: String,
    val remoteDeviceName: String,
    val remoteDeviceId: String,
    val direction: TransferDirection,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransferStatus,
    val sha256Checksum: String? = null,
    val speedAvgBps: Long = 0L,
    val durationMs: Long = 0L
)
