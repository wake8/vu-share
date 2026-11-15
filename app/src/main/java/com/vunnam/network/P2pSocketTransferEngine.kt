package com.vunnam.network

import android.content.Context
import com.vunnam.database.AppDatabase
import com.vunnam.database.TransferRecordEntity
import com.vunnam.model.ConnectionType
import com.vunnam.model.DeviceTransferProgress
import com.vunnam.model.DeviceType
import com.vunnam.model.NearbyDevice
import com.vunnam.model.TransferDirection
import com.vunnam.model.TransferItem
import com.vunnam.model.TransferSession
import com.vunnam.model.TransferStatus
import com.vunnam.storage.FileSecuritySanitizer
import com.vunnam.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.UUID

class P2pSocketTransferEngine(
    private val context: Context,
    private val storageManager: StorageManager,
    private val database: AppDatabase
) {
    companion object {
        const val DEFAULT_PORT = 8988
        const val UDP_DISCOVERY_PORT = 8989
        const val MAGIC_HEADER = "VUSHARE_P2P"
        const val CONNECT_TIMEOUT_MS = 10_000
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMutex = Mutex()

    // State flows
    private val _activeSession = MutableStateFlow<TransferSession?>(null)
    val activeSession: StateFlow<TransferSession?> = _activeSession.asStateFlow()

    private val _incomingProposal = MutableStateFlow<SocketTransferProposal?>(null)
    val incomingProposal: StateFlow<SocketTransferProposal?> = _incomingProposal.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<NearbyDevice>> = _discoveredDevices.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    @Volatile
    var historyRecordingEnabled: Boolean = true

    @Volatile
    var maxConcurrentGroupTransfers: Int = 3

    private var serverSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null
    private var discoveryJob: Job? = null
    private var broadcastJob: Job? = null
    private var currentTransferJob: Job? = null

    @Volatile
    private var isCancelled = false

    @Volatile
    private var isPaused = false

    private data class LastSendRequest(
        val targetDevices: List<NearbyDevice>,
        val items: List<TransferItem>,
        val localNickname: String,
        val avatarId: Int,
        val avatarEmoji: String
    )

    private var lastSendRequest: LastSendRequest? = null

    data class SocketTransferProposal(
        val sessionId: String,
        val clientSocket: Socket,
        val input: DataInputStream,
        val output: DataOutputStream,
        val protocolVersion: Int,
        val senderDevice: NearbyDevice,
        val items: List<TransferItem>,
        val totalBytes: Long
    )

    // ---------------------------------------------------------
    // UDP Beacon Discovery for Local Wi-Fi / Hotspot
    // ---------------------------------------------------------

    fun startDiscovery(localNickname: String, avatarId: Int, avatarEmoji: String, localPort: Int = DEFAULT_PORT) {
        if (_isDiscovering.value) return
        _isDiscovering.value = true
        _discoveredDevices.value = emptyList()

        discoveryJob = engineScope.launch {
            try {
                udpSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(UDP_DISCOVERY_PORT))
                    broadcast = true
                    soTimeout = 2000
                }
                val buffer = ByteArray(2048)

                while (isActive && _isDiscovering.value) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        udpSocket?.receive(packet)
                        val message = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                        val senderIp = packet.address.hostAddress ?: continue
                        val localIps = NetworkUtils.getAllLocalIpAddresses()
                        if (senderIp in localIps) continue // Skip self across all active interfaces

                        handleUdpMessage(message, senderIp, localNickname, avatarId, avatarEmoji, localPort)
                    } catch (_: Exception) {
                        // Timeout or socket closed, continue loop
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Periodically broadcast discovery ping
        broadcastJob = engineScope.launch {
            while (isActive && _isDiscovering.value) {
                sendUdpBeacon("DISCOVER", localNickname, avatarId, avatarEmoji, localPort)
                delay(2500)
            }
        }
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
        discoveryJob?.cancel()
        discoveryJob = null
        broadcastJob?.cancel()
        broadcastJob = null
        try {
            udpSocket?.close()
        } catch (_: Exception) {
        }
        udpSocket = null
    }

    private suspend fun sendUdpBeacon(
        type: String,
        nickname: String,
        avatarId: Int,
        avatarEmoji: String,
        port: Int
    ) = withContext(Dispatchers.IO) {
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val json = JSONObject().apply {
                    put("type", type)
                    put("name", nickname)
                    put("avatar", avatarId)
                    put("emoji", avatarEmoji)
                    put("port", port)
                }
                val data = "$MAGIC_HEADER:${json}".toByteArray(StandardCharsets.UTF_8)
                val addresses = NetworkUtils.getBroadcastAddresses()
                for (address in addresses) {
                    try {
                        val packet = DatagramPacket(data, data.size, address, UDP_DISCOVERY_PORT)
                        socket.send(packet)
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun handleUdpMessage(
        message: String,
        senderIp: String,
        localNickname: String,
        avatarId: Int,
        avatarEmoji: String,
        localPort: Int
    ) {
        if (!message.startsWith(MAGIC_HEADER)) return
        try {
            val jsonString = message.substringAfter("$MAGIC_HEADER:")
            val json = JSONObject(jsonString)
            val type = json.optString("type")
            val name = json.optString("name", "VU Peer")
            val remoteAvatar = json.optInt("avatar", 0)
            val remoteEmoji = json.optString("emoji", "🚀")
            val port = json.optInt("port", DEFAULT_PORT)
            val deviceId = "$senderIp:$port"

            val device = NearbyDevice(
                id = deviceId,
                nickname = name,
                avatarId = remoteAvatar,
                avatarEmoji = remoteEmoji,
                deviceType = DeviceType.PHONE,
                connectionType = ConnectionType.WIFI_LAN_SOCKET,
                ipAddress = senderIp,
                port = port,
                statusText = "Online via Wi-Fi"
            )

            _discoveredDevices.update { existing ->
                existing.filter { it.id != deviceId } + device
            }

            if (type == "DISCOVER") {
                engineScope.launch {
                    sendUdpBeacon("PRESENCE", localNickname, avatarId, avatarEmoji, localPort)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------------------------------------------------------
    // TCP Server (Receiver)
    // ---------------------------------------------------------

    fun startServer(port: Int = DEFAULT_PORT) {
        if (_isServerRunning.value) return
        _isServerRunning.value = true

        engineScope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                while (isActive && _isServerRunning.value) {
                    val socket = serverSocket?.accept() ?: break
                    handleIncomingClientSocket(socket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isServerRunning.value = false
            }
        }
    }

    fun stopServer() {
        _isServerRunning.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    private fun handleIncomingClientSocket(socket: Socket) {
        engineScope.launch {
            try {
                TransferStreamTuning.configureSocket(socket, TransferStreamTuning.LARGE_BUFFER_BYTES)
                val input = DataInputStream(
                    BufferedInputStream(socket.getInputStream(), TransferStreamTuning.MEDIUM_BUFFER_BYTES)
                )
                val output = DataOutputStream(
                    BufferedOutputStream(socket.getOutputStream(), TransferStreamTuning.MEDIUM_BUFFER_BYTES)
                )

                val messageJson = input.readUTF()
                val json = JSONObject(messageJson)

                if (json.optString("action") == "TRANSFER_REQUEST") {
                    val protocolVersion = json.optInt("protocolVersion", 1)
                    val sessionId = json.getString("sessionId")
                    val senderName = json.optString("senderName", "Remote Device")
                    val senderAvatar = json.optInt("senderAvatar", 0)
                    val senderEmoji = json.optString("senderEmoji", "🚀")
                    val filesArray = json.getJSONArray("files")

                    val items = mutableListOf<TransferItem>()
                    for (i in 0 until filesArray.length()) {
                        val f = filesArray.getJSONObject(i)
                        items.add(
                            TransferItem(
                                id = f.getString("id"),
                                uri = null,
                                fileName = f.getString("name"),
                                fileSize = f.getLong("size"),
                                mimeType = f.optString("mime", "application/octet-stream"),
                                sha256Checksum = f.optString("sha256").takeIf { it.isNotBlank() },
                                direction = TransferDirection.RECEIVE,
                                status = TransferStatus.PENDING
                            )
                        )
                    }

                    val senderDevice = NearbyDevice(
                        id = "${socket.inetAddress.hostAddress}:${socket.port}",
                        nickname = senderName,
                        avatarId = senderAvatar,
                        avatarEmoji = senderEmoji,
                        ipAddress = socket.inetAddress.hostAddress
                    )

                    _incomingProposal.value = SocketTransferProposal(
                        sessionId = sessionId,
                        clientSocket = socket,
                        input = input,
                        output = output,
                        protocolVersion = protocolVersion,
                        senderDevice = senderDevice,
                        items = items,
                        totalBytes = items.sumOf { it.fileSize }
                    )
                } else {
                    socket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    fun acceptIncomingTransfer(proposal: SocketTransferProposal) {
        engineScope.launch {
            _incomingProposal.value = null
            isCancelled = false
            isPaused = false
            val socket = proposal.clientSocket
            val items = proposal.items.toMutableList()
            val targetDir = storageManager.getDownloadDirectory()
            val availableBytes = storageManager.getAvailableBytes()

            if (availableBytes != Long.MAX_VALUE && proposal.totalBytes > availableBytes) {
                try {
                    val rejectResponse = JSONObject().apply {
                        put("action", "TRANSFER_ACCEPT")
                        put("accepted", false)
                        put("reason", "insufficient_storage")
                    }
                    proposal.output.writeUTF(rejectResponse.toString())
                    proposal.output.flush()
                    socket.close()
                } catch (_: Exception) {
                }
                _activeSession.value = TransferSession(
                    sessionId = proposal.sessionId,
                    targetDevices = listOf(proposal.senderDevice),
                    items = items.map { it.copy(status = TransferStatus.FAILED) },
                    direction = TransferDirection.RECEIVE,
                    status = TransferStatus.FAILED,
                    errorMessage = "Insufficient storage space (${NetworkUtils.formatFileSize(availableBytes)} available)"
                )
                return@launch
            }

            val sessionBufferSize = TransferStreamTuning.selectAdaptiveBufferSize(
                fileSize = items.maxOfOrNull { it.fileSize } ?: 0L,
                totalSessionBytes = proposal.totalBytes,
                concurrentStreams = 1
            )
            TransferStreamTuning.configureSocket(socket, sessionBufferSize)
            val sharedBuffer = ByteArray(sessionBufferSize)

            // Detect resumable partial files from interrupted transfers
            val resumeOffsetsJson = JSONObject()
            val partFiles = mutableMapOf<String, File>()
            val resumeOffsets = mutableMapOf<String, Long>()
            for (item in items) {
                val sanitizedName = FileSecuritySanitizer.sanitizeFilename(item.fileName)
                val partFile = File(targetDir, ".vupart_${sanitizedName}_${item.fileSize}")
                partFiles[item.id] = partFile
                val existingLen = if (proposal.protocolVersion >= 2 && partFile.exists() && partFile.length() in 1L until item.fileSize) {
                    partFile.length()
                } else {
                    0L
                }
                resumeOffsets[item.id] = existingLen
                if (existingLen > 0L) {
                    resumeOffsetsJson.put(item.id, existingLen)
                }
            }

            val initialDeviceProgress = mapOf(
                proposal.senderDevice.id to DeviceTransferProgress(
                    deviceId = proposal.senderDevice.id,
                    deviceName = proposal.senderDevice.nickname,
                    avatarEmoji = proposal.senderDevice.avatarEmoji,
                    status = TransferStatus.TRANSFERRING,
                    totalBytes = proposal.totalBytes
                )
            )

            val initialSession = TransferSession(
                sessionId = proposal.sessionId,
                targetDevices = listOf(proposal.senderDevice),
                items = items,
                direction = TransferDirection.RECEIVE,
                totalBytes = proposal.totalBytes,
                status = TransferStatus.TRANSFERRING,
                deviceProgress = initialDeviceProgress
            )
            _activeSession.value = initialSession

            currentTransferJob = engineScope.launch {
                try {
                    val output = proposal.output
                    val input = proposal.input

                    val acceptResponse = JSONObject().apply {
                        put("action", "TRANSFER_ACCEPT")
                        put("accepted", true)
                        put("protocolVersion", TransferStreamTuning.PROTOCOL_VERSION)
                        put("resumeOffsets", resumeOffsetsJson)
                    }
                    output.writeUTF(acceptResponse.toString())
                    output.flush()

                    var completedBytesOffset = 0L
                    val sessionStartTime = System.currentTimeMillis()
                    var anyFailed = false

                    for (i in items.indices) {
                        if (isCancelled) break
                        val currentItem = items[i]
                        val sanitizedName = FileSecuritySanitizer.sanitizeFilename(currentItem.fileName)
                        val partFile = partFiles[currentItem.id] ?: File(targetDir, ".vupart_${sanitizedName}_${currentItem.fileSize}")
                        val resumeOffset = resumeOffsets[currentItem.id] ?: 0L

                        items[i] = currentItem.copy(
                            status = TransferStatus.TRANSFERRING,
                            bytesTransferred = resumeOffset
                        )
                        _activeSession.update { cur ->
                            cur?.copy(items = items.toList())
                        }

                        val result = TransferStreamTuning.receiveStreamSinglePassSha256(
                            input = input,
                            partFile = partFile,
                            expectedSize = currentItem.fileSize,
                            resumeOffset = resumeOffset,
                            hasV2Trailer = proposal.protocolVersion >= 2,
                            legacyExpectedSha256 = currentItem.sha256Checksum,
                            buffer = sharedBuffer,
                            isPaused = { isPaused },
                            isCancelled = { isCancelled },
                            onProgress = { bytesReceived, speedBps, etaSec ->
                                val totalTransferred = completedBytesOffset + bytesReceived
                                val totalRemaining = (proposal.totalBytes - totalTransferred).coerceAtLeast(0L)
                                val sessionEta = if (speedBps > 0L) totalRemaining / speedBps else etaSec
                                items[i] = items[i].copy(
                                    bytesTransferred = bytesReceived,
                                    speedBytesPerSec = speedBps,
                                    etaSeconds = etaSec,
                                    status = if (isPaused) TransferStatus.PAUSED else TransferStatus.TRANSFERRING
                                )
                                _activeSession.update { cur ->
                                    cur?.copy(
                                        items = items.toList(),
                                        transferredBytes = totalTransferred,
                                        speedBps = speedBps,
                                        etaSeconds = sessionEta,
                                        isPaused = isPaused,
                                        status = if (isPaused) TransferStatus.PAUSED else TransferStatus.TRANSFERRING,
                                        deviceProgress = mapOf(
                                            proposal.senderDevice.id to DeviceTransferProgress(
                                                deviceId = proposal.senderDevice.id,
                                                deviceName = proposal.senderDevice.nickname,
                                                avatarEmoji = proposal.senderDevice.avatarEmoji,
                                                status = if (isPaused) TransferStatus.PAUSED else TransferStatus.TRANSFERRING,
                                                bytesTransferred = totalTransferred,
                                                totalBytes = proposal.totalBytes,
                                                speedBps = speedBps
                                            )
                                        )
                                    )
                                }
                            }
                        )

                        completedBytesOffset += result.bytesReceived
                        val itemStatus = when {
                            isCancelled -> {
                                partFile.delete()
                                TransferStatus.CANCELLED
                            }
                            result.verified -> TransferStatus.COMPLETED
                            else -> {
                                anyFailed = true
                                if (result.completed) {
                                    // Hash mismatch: discard corrupted file
                                    partFile.delete()
                                }
                                TransferStatus.FAILED
                            }
                        }

                        var finalFile: File? = null
                        if (itemStatus == TransferStatus.COMPLETED) {
                            val uniqueDest = FileSecuritySanitizer.getUniqueDestinationFile(targetDir, sanitizedName)
                            if (partFile.renameTo(uniqueDest)) {
                                finalFile = uniqueDest
                            } else {
                                partFile.copyTo(uniqueDest, overwrite = true)
                                partFile.delete()
                                finalFile = uniqueDest
                            }
                        }

                        items[i] = items[i].copy(
                            status = itemStatus,
                            bytesTransferred = result.bytesReceived,
                            sha256Checksum = result.sha256,
                            verifiedChecksum = result.verified,
                            localFilePath = finalFile?.absolutePath,
                            errorMessage = if (itemStatus == TransferStatus.FAILED && result.completed) "SHA-256 verification failed" else null
                        )
                        _activeSession.update { cur ->
                            cur?.copy(
                                items = items.toList(),
                                transferredBytes = completedBytesOffset
                            )
                        }

                        if (historyRecordingEnabled && itemStatus != TransferStatus.CANCELLED) {
                            val finalSize = finalFile?.length() ?: result.bytesReceived
                            val duration = (System.currentTimeMillis() - sessionStartTime).coerceAtLeast(1L)
                            val avgSpeed = (completedBytesOffset * 1000L) / duration
                            database.transferDao().insertTransfer(
                                TransferRecordEntity(
                                    id = UUID.randomUUID().toString(),
                                    sessionId = proposal.sessionId,
                                    fileName = finalFile?.name ?: sanitizedName,
                                    filePath = finalFile?.absolutePath,
                                    fileSize = finalSize,
                                    mimeType = currentItem.mimeType,
                                    remoteDeviceName = proposal.senderDevice.nickname,
                                    remoteDeviceId = proposal.senderDevice.id,
                                    direction = TransferDirection.RECEIVE,
                                    status = itemStatus,
                                    sha256Checksum = result.sha256,
                                    speedAvgBps = avgSpeed,
                                    durationMs = duration
                                )
                            )
                        }
                    }

                    val finalSessionStatus = when {
                        isCancelled -> TransferStatus.CANCELLED
                        anyFailed -> TransferStatus.FAILED
                        else -> TransferStatus.COMPLETED
                    }
                    _activeSession.update { cur ->
                        cur?.copy(
                            status = finalSessionStatus,
                            isPaused = false,
                            speedBps = 0L,
                            etaSeconds = 0L,
                            deviceProgress = mapOf(
                                proposal.senderDevice.id to DeviceTransferProgress(
                                    deviceId = proposal.senderDevice.id,
                                    deviceName = proposal.senderDevice.nickname,
                                    avatarEmoji = proposal.senderDevice.avatarEmoji,
                                    status = finalSessionStatus,
                                    bytesTransferred = completedBytesOffset,
                                    totalBytes = proposal.totalBytes
                                )
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _activeSession.update { cur ->
                        cur?.copy(
                            status = if (isCancelled) TransferStatus.CANCELLED else TransferStatus.FAILED,
                            errorMessage = e.localizedMessage ?: "Transfer interrupted"
                        )
                    }
                } finally {
                    try { socket.close() } catch (_: Exception) {}
                }
            }
        }
    }

    fun rejectIncomingTransfer(proposal: SocketTransferProposal) {
        engineScope.launch {
            _incomingProposal.value = null
            try {
                val rejectResponse = JSONObject().apply {
                    put("action", "TRANSFER_ACCEPT")
                    put("accepted", false)
                    put("reason", "rejected_by_user")
                }
                proposal.output.writeUTF(rejectResponse.toString())
                proposal.output.flush()
                proposal.clientSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ---------------------------------------------------------
    // TCP Client (Sender - Single & Bounded-Concurrent Group Broadcast)
    // ---------------------------------------------------------

    fun sendFiles(
        targetDevices: List<NearbyDevice>,
        items: List<TransferItem>,
        localNickname: String,
        avatarId: Int,
        avatarEmoji: String,
        onSessionCreated: (TransferSession) -> Unit
    ) {
        if (targetDevices.isEmpty() || items.isEmpty()) return
        lastSendRequest = LastSendRequest(targetDevices, items, localNickname, avatarId, avatarEmoji)
        isCancelled = false
        isPaused = false

        val sessionId = UUID.randomUUID().toString().take(8)
        val singlePayloadBytes = items.sumOf { it.fileSize }
        val totalAggregateBytes = singlePayloadBytes * targetDevices.size
        val initialDeviceMap = targetDevices.associate { dev ->
            dev.id to DeviceTransferProgress(
                deviceId = dev.id,
                deviceName = dev.nickname,
                avatarEmoji = dev.avatarEmoji,
                status = TransferStatus.CONNECTING,
                bytesTransferred = 0L,
                totalBytes = singlePayloadBytes
            )
        }

        val session = TransferSession(
            sessionId = sessionId,
            targetDevices = targetDevices,
            items = items,
            direction = TransferDirection.SEND,
            totalBytes = totalAggregateBytes,
            status = TransferStatus.CONNECTING,
            isGroupTransfer = targetDevices.size > 1,
            deviceProgress = initialDeviceMap
        )
        _activeSession.value = session
        onSessionCreated(session)

        val concurrency = if (targetDevices.size > 1) {
            maxConcurrentGroupTransfers.coerceIn(1, targetDevices.size.coerceAtMost(3))
        } else {
            1
        }
        val semaphore = Semaphore(concurrency)

        currentTransferJob = engineScope.launch {
            coroutineScope {
                targetDevices.map { device ->
                    async {
                        semaphore.withPermit {
                            if (!isCancelled) {
                                sendFilesToDevice(
                                    device = device,
                                    sessionId = sessionId,
                                    sourceItems = items,
                                    singlePayloadBytes = singlePayloadBytes,
                                    concurrentStreams = concurrency,
                                    localNickname = localNickname,
                                    avatarId = avatarId,
                                    avatarEmoji = avatarEmoji
                                )
                            }
                        }
                    }
                }.awaitAll()
            }

            // Finalize aggregate session state once all recipients complete or fail
            stateMutex.withLock {
                val current = _activeSession.value ?: return@withLock
                val statuses = current.deviceProgress.values.map { it.status }
                val finalStatus = when {
                    isCancelled -> TransferStatus.CANCELLED
                    statuses.all { it == TransferStatus.COMPLETED } -> TransferStatus.COMPLETED
                    statuses.all { it == TransferStatus.REJECTED } -> TransferStatus.REJECTED
                    statuses.any { it == TransferStatus.COMPLETED } -> TransferStatus.COMPLETED
                    else -> TransferStatus.FAILED
                }
                val partialWarning = if (finalStatus == TransferStatus.COMPLETED && statuses.any { it == TransferStatus.FAILED || it == TransferStatus.REJECTED }) {
                    "Completed for ${statuses.count { it == TransferStatus.COMPLETED }}/${statuses.size} devices"
                } else {
                    current.errorMessage
                }
                _activeSession.value = current.copy(
                    status = finalStatus,
                    isPaused = false,
                    speedBps = 0L,
                    etaSeconds = 0L,
                    errorMessage = partialWarning
                )
            }
        }
    }

    fun retryLastTransfer() {
        val last = lastSendRequest ?: return
        val currentSession = _activeSession.value
        val devicesToRetry = if (currentSession != null && currentSession.deviceProgress.isNotEmpty()) {
            last.targetDevices.filter { dev ->
                val st = currentSession.deviceProgress[dev.id]?.status
                st != TransferStatus.COMPLETED
            }.ifEmpty { last.targetDevices }
        } else {
            last.targetDevices
        }
        sendFiles(
            targetDevices = devicesToRetry,
            items = last.items.map {
                it.copy(bytesTransferred = 0L, status = TransferStatus.PENDING, speedBytesPerSec = 0L, etaSeconds = 0L)
            },
            localNickname = last.localNickname,
            avatarId = last.avatarId,
            avatarEmoji = last.avatarEmoji,
            onSessionCreated = {}
        )
    }

    private suspend fun sendFilesToDevice(
        device: NearbyDevice,
        sessionId: String,
        sourceItems: List<TransferItem>,
        singlePayloadBytes: Long,
        concurrentStreams: Int,
        localNickname: String,
        avatarId: Int,
        avatarEmoji: String
    ) = withContext(Dispatchers.IO) {
        val hostIp = device.ipAddress ?: "127.0.0.1"
        val port = device.port
        var socket: Socket? = null

        val sessionBufferSize = TransferStreamTuning.selectAdaptiveBufferSize(
            fileSize = sourceItems.maxOfOrNull { it.fileSize } ?: 0L,
            totalSessionBytes = singlePayloadBytes,
            concurrentStreams = concurrentStreams
        )

        try {
            socket = Socket()
            socket.connect(InetSocketAddress(hostIp, port), CONNECT_TIMEOUT_MS)
            TransferStreamTuning.configureSocket(socket, sessionBufferSize)

            val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream(), sessionBufferSize))
            val input = DataInputStream(BufferedInputStream(socket.getInputStream(), TransferStreamTuning.MIN_BUFFER_BYTES))

            // 1. Send TRANSFER_REQUEST immediately without pre-reading every file for SHA-256!
            val filesArray = JSONArray()
            val preparedItems = sourceItems.toMutableList()
            for (item in preparedItems) {
                filesArray.put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.fileName)
                    put("size", item.fileSize)
                    put("mime", item.mimeType)
                })
            }

            val requestJson = JSONObject().apply {
                put("action", "TRANSFER_REQUEST")
                put("protocolVersion", TransferStreamTuning.PROTOCOL_VERSION)
                put("sessionId", sessionId)
                put("senderName", localNickname)
                put("senderAvatar", avatarId)
                put("senderEmoji", avatarEmoji)
                put("files", filesArray)
            }
            output.writeUTF(requestJson.toString())
            output.flush()

            // 2. Wait for recipient approval & resume offsets
            val responseString = input.readUTF()
            val responseJson = JSONObject(responseString)
            val accepted = responseJson.optBoolean("accepted", false)
            val receiverProtocol = responseJson.optInt("protocolVersion", 1)
            val resumeOffsetsJson = responseJson.optJSONObject("resumeOffsets")

            if (!accepted) {
                val reason = responseJson.optString("reason", "rejected_by_user")
                val rejectMsg = if (reason == "insufficient_storage") "Recipient lacks storage space" else "Declined by recipient"
                val failStatus = if (reason == "insufficient_storage") TransferStatus.FAILED else TransferStatus.REJECTED
                updateDeviceProgress(
                    device = device,
                    status = failStatus,
                    bytesTransferred = 0L,
                    totalBytes = singlePayloadBytes,
                    speedBps = 0L,
                    errorMessage = rejectMsg,
                    updatedItems = if (concurrentStreams == 1) preparedItems.map { it.copy(status = failStatus) } else null
                )
                return@withContext
            }

            // Allocate a single reusable buffer for this device connection
            val sharedBuffer = ByteArray(sessionBufferSize)
            val sessionStartTime = System.currentTimeMillis()
            var deviceCompletedBytes = 0L

            updateDeviceProgress(
                device = device,
                status = TransferStatus.TRANSFERRING,
                bytesTransferred = 0L,
                totalBytes = singlePayloadBytes,
                speedBps = 0L,
                updatedItems = if (concurrentStreams == 1) preparedItems else null
            )

            for (i in preparedItems.indices) {
                if (isCancelled) break
                val currentItem = preparedItems[i]
                val uri = currentItem.uri ?: continue
                val resumeOffset = if (receiverProtocol >= 2) {
                    (resumeOffsetsJson?.optLong(currentItem.id, 0L) ?: 0L).coerceIn(0L, currentItem.fileSize)
                } else {
                    0L
                }

                preparedItems[i] = currentItem.copy(
                    status = TransferStatus.TRANSFERRING,
                    bytesTransferred = resumeOffset
                )

                val sha256Hex = context.contentResolver.openInputStream(uri)?.use { rawStream ->
                    TransferStreamTuning.sendStreamSinglePassSha256(
                        rawInput = rawStream,
                        output = output,
                        fileSize = currentItem.fileSize,
                        resumeOffset = resumeOffset,
                        buffer = sharedBuffer,
                        isPaused = { isPaused },
                        isCancelled = { isCancelled },
                        onProgress = { bytesSent, speedBps, etaSec ->
                            val devTransferred = deviceCompletedBytes + bytesSent
                            preparedItems[i] = preparedItems[i].copy(
                                bytesTransferred = bytesSent,
                                speedBytesPerSec = speedBps,
                                etaSeconds = etaSec,
                                status = if (isPaused) TransferStatus.PAUSED else TransferStatus.TRANSFERRING
                            )
                            engineScope.launch {
                                updateDeviceProgress(
                                    device = device,
                                    status = if (isPaused) TransferStatus.PAUSED else TransferStatus.TRANSFERRING,
                                    bytesTransferred = devTransferred,
                                    totalBytes = singlePayloadBytes,
                                    speedBps = speedBps,
                                    updatedItems = if (concurrentStreams == 1) preparedItems.toList() else null
                                )
                            }
                        }
                    )
                }

                if (isCancelled) {
                    preparedItems[i] = preparedItems[i].copy(status = TransferStatus.CANCELLED)
                    break
                }

                deviceCompletedBytes += currentItem.fileSize
                preparedItems[i] = preparedItems[i].copy(
                    status = TransferStatus.COMPLETED,
                    bytesTransferred = currentItem.fileSize,
                    sha256Checksum = sha256Hex,
                    verifiedChecksum = sha256Hex != null
                )

                updateDeviceProgress(
                    device = device,
                    status = TransferStatus.TRANSFERRING,
                    bytesTransferred = deviceCompletedBytes,
                    totalBytes = singlePayloadBytes,
                    speedBps = preparedItems[i].speedBytesPerSec,
                    updatedItems = if (concurrentStreams == 1) preparedItems.toList() else null
                )

                if (historyRecordingEnabled) {
                    val duration = (System.currentTimeMillis() - sessionStartTime).coerceAtLeast(1L)
                    val avgSpeed = (deviceCompletedBytes * 1000L) / duration
                    database.transferDao().insertTransfer(
                        TransferRecordEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            fileName = currentItem.fileName,
                            filePath = currentItem.uri.toString(),
                            fileSize = currentItem.fileSize,
                            mimeType = currentItem.mimeType,
                            remoteDeviceName = device.nickname,
                            remoteDeviceId = device.id,
                            direction = TransferDirection.SEND,
                            status = TransferStatus.COMPLETED,
                            sha256Checksum = sha256Hex,
                            speedAvgBps = avgSpeed,
                            durationMs = duration
                        )
                    )
                }
            }

            val deviceFinalStatus = if (isCancelled) TransferStatus.CANCELLED else TransferStatus.COMPLETED
            updateDeviceProgress(
                device = device,
                status = deviceFinalStatus,
                bytesTransferred = deviceCompletedBytes,
                totalBytes = singlePayloadBytes,
                speedBps = 0L,
                updatedItems = if (concurrentStreams == 1) preparedItems.toList() else null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            updateDeviceProgress(
                device = device,
                status = if (isCancelled) TransferStatus.CANCELLED else TransferStatus.FAILED,
                bytesTransferred = _activeSession.value?.deviceProgress?.get(device.id)?.bytesTransferred ?: 0L,
                totalBytes = singlePayloadBytes,
                speedBps = 0L,
                errorMessage = e.localizedMessage ?: "Connection lost"
            )
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private suspend fun updateDeviceProgress(
        device: NearbyDevice,
        status: TransferStatus,
        bytesTransferred: Long,
        totalBytes: Long,
        speedBps: Long,
        errorMessage: String? = null,
        updatedItems: List<TransferItem>? = null
    ) {
        stateMutex.withLock {
            val current = _activeSession.value ?: return@withLock
            val updatedMap = current.deviceProgress.toMutableMap()
            updatedMap[device.id] = DeviceTransferProgress(
                deviceId = device.id,
                deviceName = device.nickname,
                avatarEmoji = device.avatarEmoji,
                status = status,
                bytesTransferred = bytesTransferred,
                totalBytes = totalBytes,
                speedBps = speedBps,
                errorMessage = errorMessage
            )

            val aggTransferred = updatedMap.values.sumOf { it.bytesTransferred }
            val aggSpeed = updatedMap.values
                .filter { it.status == TransferStatus.TRANSFERRING }
                .sumOf { it.speedBps }
            val remaining = (current.totalBytes - aggTransferred).coerceAtLeast(0L)
            val aggEta = if (aggSpeed > 0L) remaining / aggSpeed else 0L

            val anyActive = updatedMap.values.any { it.status == TransferStatus.TRANSFERRING }
            val anyConnecting = updatedMap.values.any { it.status == TransferStatus.CONNECTING }
            val sessionStatus = when {
                isCancelled -> TransferStatus.CANCELLED
                isPaused -> TransferStatus.PAUSED
                anyActive -> TransferStatus.TRANSFERRING
                anyConnecting -> TransferStatus.CONNECTING
                else -> current.status
            }

            // For multi-device group transfers, reflect average item progress cleanly
            val nextItems = if (updatedItems != null) {
                updatedItems
            } else if (current.targetDevices.size > 1 && current.totalBytes > 0L) {
                val ratio = (aggTransferred.toFloat() / current.totalBytes.toFloat()).coerceIn(0f, 1f)
                current.items.map { item ->
                    val approxBytes = (item.fileSize * ratio).toLong()
                    val itemSt = when {
                        isPaused -> TransferStatus.PAUSED
                        ratio >= 1f -> TransferStatus.COMPLETED
                        anyActive -> TransferStatus.TRANSFERRING
                        else -> item.status
                    }
                    item.copy(bytesTransferred = approxBytes, status = itemSt)
                }
            } else {
                current.items
            }

            _activeSession.value = current.copy(
                items = nextItems,
                transferredBytes = aggTransferred,
                speedBps = aggSpeed,
                etaSeconds = aggEta,
                status = sessionStatus,
                isPaused = isPaused,
                deviceProgress = updatedMap
            )
        }
    }

    fun pauseActiveTransfer() {
        isPaused = true
        _activeSession.update { cur ->
            cur?.copy(isPaused = true, status = TransferStatus.PAUSED, speedBps = 0L)
        }
    }

    fun resumeActiveTransfer() {
        isPaused = false
        _activeSession.update { cur ->
            cur?.copy(isPaused = false, status = TransferStatus.TRANSFERRING)
        }
    }

    fun cancelActiveTransfer() {
        isCancelled = true
        isPaused = false
        currentTransferJob?.cancel()
        currentTransferJob = null
        _activeSession.update { cur ->
            cur?.copy(status = TransferStatus.CANCELLED, isPaused = false, speedBps = 0L)
        }
    }

    fun resetSession() {
        _activeSession.value = null
        _incomingProposal.value = null
    }
}
