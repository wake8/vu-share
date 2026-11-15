package com.vunnam.network

import android.content.Context
import android.net.Uri
import com.vunnam.model.NearbyDevice
import com.vunnam.model.TransferDirection
import com.vunnam.model.TransferItem
import com.vunnam.model.TransferStatus
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.StandardCharsets

class NearbyConnectionsManager(private val context: Context) {

    companion object {
        const val SERVICE_ID = "com.vunnam.vushare"
        val STRATEGY: Strategy = Strategy.P2P_STAR
    }

    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context)
    }

    private val _discoveredDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<NearbyDevice>> = _discoveredDevices.asStateFlow()

    private val _incomingConnectionRequest = MutableStateFlow<NearbyDevice?>(null)
    val incomingConnectionRequest: StateFlow<NearbyDevice?> = _incomingConnectionRequest.asStateFlow()

    private val _incomingTransferProposal = MutableStateFlow<TransferProposal?>(null)
    val incomingTransferProposal: StateFlow<TransferProposal?> = _incomingTransferProposal.asStateFlow()

    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints.asStateFlow()

    data class TransferProposal(
        val endpointId: String,
        val senderDevice: NearbyDevice,
        val items: List<TransferItem>,
        val totalBytes: Long
    )

    var onPayloadProgress: ((endpointId: String, payloadId: Long, bytesTransferred: Long, totalBytes: Long) -> Unit)? = null
    var onPayloadReceived: ((endpointId: String, payload: Payload) -> Unit)? = null
    var onConnectionEstablished: ((endpointId: String) -> Unit)? = null
    var onConnectionDisconnected: ((endpointId: String) -> Unit)? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = String(payload.asBytes() ?: ByteArray(0), StandardCharsets.UTF_8)
                handleControlMessage(endpointId, data)
            } else {
                onPayloadReceived?.invoke(endpointId, payload)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            onPayloadProgress?.invoke(
                endpointId,
                update.payloadId,
                update.bytesTransferred,
                update.totalBytes
            )
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val device = parseDeviceEndpoint(endpointId, connectionInfo.endpointName)
            _incomingConnectionRequest.value = device
            // Authenticated connection prompt - auto-accepting connection handshake to allow message negotiation
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                _connectedEndpoints.value = _connectedEndpoints.value + endpointId
                onConnectionEstablished?.invoke(endpointId)
            } else {
                _connectedEndpoints.value = _connectedEndpoints.value - endpointId
                onConnectionDisconnected?.invoke(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
            onConnectionDisconnected?.invoke(endpointId)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val device = parseDeviceEndpoint(endpointId, info.endpointName)
            val current = _discoveredDevices.value.filter { it.id != endpointId }
            _discoveredDevices.value = current + device
        }

        override fun onEndpointLost(endpointId: String) {
            _discoveredDevices.value = _discoveredDevices.value.filter { it.id != endpointId }
        }
    }

    private fun parseDeviceEndpoint(endpointId: String, rawName: String): NearbyDevice {
        return try {
            if (rawName.startsWith("{")) {
                val json = JSONObject(rawName)
                NearbyDevice(
                    id = endpointId,
                    nickname = json.optString("name", "VU Peer"),
                    avatarId = json.optInt("avatar", 0),
                    avatarEmoji = json.optString("emoji", "🚀"),
                    statusText = "Nearby"
                )
            } else {
                NearbyDevice(
                    id = endpointId,
                    nickname = rawName,
                    statusText = "Nearby"
                )
            }
        } catch (e: Exception) {
            NearbyDevice(
                id = endpointId,
                nickname = rawName,
                statusText = "Nearby"
            )
        }
    }

    fun startAdvertising(localNickname: String, avatarId: Int, avatarEmoji: String) {
        val endpointInfo = JSONObject().apply {
            put("name", localNickname)
            put("avatar", avatarId)
            put("emoji", avatarEmoji)
        }.toString()

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        try {
            connectionsClient.startAdvertising(
                endpointInfo,
                SERVICE_ID,
                connectionLifecycleCallback,
                advertisingOptions
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startDiscovery() {
        _discoveredDevices.value = emptyList()
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        try {
            connectionsClient.startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                discoveryOptions
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAdvertising() {
        try {
            connectionsClient.stopAdvertising()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopDiscovery() {
        try {
            connectionsClient.stopDiscovery()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun connectToDevice(endpointId: String, localNickname: String, avatarId: Int, avatarEmoji: String) {
        val endpointInfo = JSONObject().apply {
            put("name", localNickname)
            put("avatar", avatarId)
            put("emoji", avatarEmoji)
        }.toString()

        try {
            connectionsClient.requestConnection(
                endpointInfo,
                endpointId,
                connectionLifecycleCallback
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendControlMessage(endpointId: String, json: JSONObject) {
        val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val payload = Payload.fromBytes(bytes)
        try {
            connectionsClient.sendPayload(endpointId, payload)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendFileProposal(endpointId: String, items: List<TransferItem>, senderDevice: NearbyDevice) {
        val filesArray = JSONArray()
        for (item in items) {
            val fileJson = JSONObject().apply {
                put("id", item.id)
                put("name", item.fileName)
                put("size", item.fileSize)
                put("mime", item.mimeType)
                put("sha256", item.sha256Checksum ?: "")
            }
            filesArray.put(fileJson)
        }
        val proposalJson = JSONObject().apply {
            put("type", "TRANSFER_PROPOSAL")
            put("senderName", senderDevice.nickname)
            put("senderAvatar", senderDevice.avatarId)
            put("senderEmoji", senderDevice.avatarEmoji)
            put("files", filesArray)
        }
        sendControlMessage(endpointId, proposalJson)
    }

    fun acceptProposal(proposal: TransferProposal) {
        val response = JSONObject().apply {
            put("type", "PROPOSAL_RESPONSE")
            put("accepted", true)
        }
        sendControlMessage(proposal.endpointId, response)
        _incomingTransferProposal.value = null
    }

    fun rejectProposal(proposal: TransferProposal) {
        val response = JSONObject().apply {
            put("type", "PROPOSAL_RESPONSE")
            put("accepted", false)
        }
        sendControlMessage(proposal.endpointId, response)
        _incomingTransferProposal.value = null
    }

    private fun handleControlMessage(endpointId: String, message: String) {
        try {
            val json = JSONObject(message)
            when (json.optString("type")) {
                "TRANSFER_PROPOSAL" -> {
                    val senderName = json.optString("senderName", "Nearby Device")
                    val senderAvatar = json.optInt("senderAvatar", 0)
                    val senderEmoji = json.optString("senderEmoji", "🚀")
                    val filesArray = json.optJSONArray("files") ?: JSONArray()
                    val items = mutableListOf<TransferItem>()
                    for (i in 0 until filesArray.length()) {
                        val fileJson = filesArray.getJSONObject(i)
                        items.add(
                            TransferItem(
                                id = fileJson.getString("id"),
                                uri = null,
                                fileName = fileJson.getString("name"),
                                fileSize = fileJson.getLong("size"),
                                mimeType = fileJson.optString("mime", "application/octet-stream"),
                                sha256Checksum = fileJson.optString("sha256").takeIf { it.isNotBlank() },
                                direction = TransferDirection.RECEIVE
                            )
                        )
                    }
                    val senderDevice = NearbyDevice(
                        id = endpointId,
                        nickname = senderName,
                        avatarId = senderAvatar,
                        avatarEmoji = senderEmoji
                    )
                    _incomingTransferProposal.value = TransferProposal(
                        endpointId = endpointId,
                        senderDevice = senderDevice,
                        items = items,
                        totalBytes = items.sumOf { it.fileSize }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect(endpointId: String) {
        try {
            connectionsClient.disconnectFromEndpoint(endpointId)
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAll() {
        try {
            connectionsClient.stopAllEndpoints()
            _connectedEndpoints.value = emptySet()
            _discoveredDevices.value = emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
