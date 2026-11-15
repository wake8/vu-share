package com.vunnam.model

import android.net.Uri

enum class FileCategory(val displayName: String) {
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    MUSIC("Music"),
    DOCUMENTS("Documents"),
    APPS("Apps"),
    FILES("All Files")
}

enum class TransferStatus {
    PENDING,
    CONNECTING,
    TRANSFERRING,
    PAUSED,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED
}

enum class TransferDirection {
    SEND,
    RECEIVE
}

enum class DeviceType {
    PHONE,
    TABLET,
    LAPTOP,
    UNKNOWN
}

enum class ConnectionType {
    NEARBY_CONNECTIONS,
    WIFI_LAN_SOCKET,
    WIFI_DIRECT,
    HOTSPOT
}

data class SelectableFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val category: FileCategory,
    val dateModified: Long = System.currentTimeMillis(),
    val appPackageName: String? = null,
    val isSelected: Boolean = false
)

data class TransferItem(
    val id: String,
    val uri: Uri?,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val bytesTransferred: Long = 0L,
    val status: TransferStatus = TransferStatus.PENDING,
    val direction: TransferDirection = TransferDirection.SEND,
    val sha256Checksum: String? = null,
    val verifiedChecksum: Boolean = false,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val localFilePath: String? = null,
    val errorMessage: String? = null
) {
    val progressPercent: Float
        get() = if (fileSize > 0) (bytesTransferred.toFloat() / fileSize.toFloat()).coerceIn(0f, 1f) else 0f
}

data class DeviceTransferProgress(
    val deviceId: String,
    val deviceName: String,
    val avatarEmoji: String = "🚀",
    val status: TransferStatus = TransferStatus.CONNECTING,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBps: Long = 0L,
    val errorMessage: String? = null
) {
    val progressPercent: Float
        get() = if (totalBytes > 0) (bytesTransferred.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class NearbyDevice(
    val id: String,
    val nickname: String,
    val avatarId: Int = 0,
    val avatarEmoji: String = "🚀",
    val deviceType: DeviceType = DeviceType.PHONE,
    val connectionType: ConnectionType = ConnectionType.WIFI_LAN_SOCKET,
    val ipAddress: String? = null,
    val port: Int = 8988,
    val signalStrengthPercent: Int = 90,
    val isFavorite: Boolean = false,
    val isBlocked: Boolean = false,
    val isSelected: Boolean = false,
    val statusText: String = "Ready"
)

data class TransferSession(
    val sessionId: String,
    val targetDevices: List<NearbyDevice>,
    val items: List<TransferItem>,
    val direction: TransferDirection,
    val timestamp: Long = System.currentTimeMillis(),
    val totalBytes: Long = items.sumOf { it.fileSize } * targetDevices.size.coerceAtLeast(1),
    val transferredBytes: Long = 0L,
    val status: TransferStatus = TransferStatus.PENDING,
    val speedBps: Long = 0L,
    val etaSeconds: Long = 0L,
    val isGroupTransfer: Boolean = false,
    val isPaused: Boolean = false,
    val deviceProgress: Map<String, DeviceTransferProgress> = emptyMap(),
    val errorMessage: String? = null
) {
    val overallProgress: Float
        get() = if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class QrPairingData(
    val version: Int = 1,
    val deviceId: String,
    val nickname: String,
    val avatarEmoji: String,
    val hostIp: String,
    val port: Int,
    val token: String,
    val expiresAt: Long
)
