package com.servify.resqmesh

            import android.content.Context
            import android.util.Log
            import com.google.android.gms.nearby.Nearby
            import com.google.android.gms.nearby.connection.*
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.asStateFlow
            import org.json.JSONObject
            import java.util.*

            class NearbyManager(private val context: Context, private val userName: String) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.servify.resqmesh.SERVICE_ID"
    private val STRATEGY = Strategy.P2P_CLUSTER

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _connectedPeers = MutableStateFlow<Map<String, String>>(emptyMap())
    val connectedPeers = _connectedPeers.asStateFlow()

    private val pendingConnections = mutableMapOf<String, String>()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val seenMessageIds = mutableSetOf<String>()

    fun startMesh() {
        _isScanning.value = true
        startAdvertising()
        startDiscovery()
    }

    fun stopMesh() {
        _isScanning.value = false
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _connectedPeers.value = emptyMap()
        pendingConnections.clear()
        android.widget.Toast.makeText(context, "Mesh Stopped", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(userName, SERVICE_ID, lifecycleCallback, options)
            .addOnSuccessListener { 
                Log.d("Nearby", "Advertising started")
                android.widget.Toast.makeText(context, "Advertising...", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e -> 
                Log.e("Nearby", "Advertising failed", e)
                _isScanning.value = false
                android.widget.Toast.makeText(context, "Advertising Failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnSuccessListener { 
                Log.d("Nearby", "Discovery started") 
                android.widget.Toast.makeText(context, "Scanning for peers...", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e -> 
                Log.e("Nearby", "Discovery failed", e) 
                val errorMsg = when((e as? com.google.android.gms.common.api.ApiException)?.statusCode) {
                    8001 -> "Location off or no permissions"
                    8002 -> "Bluetooth off"
                    else -> e.message
                }
                android.widget.Toast.makeText(context, "Discovery Error: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
            }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.endpointName == userName) return
            
            // TIE-BREAKER: Only the device with the "smaller" name initiates the request.
            // This prevents "Double Initiation" which causes "Unknown" connection failures.
            if (userName < info.endpointName) {
                Log.d("Nearby", "Initiating connection to ${info.endpointName}")
                android.widget.Toast.makeText(context, "Found Peer: ${info.endpointName}, Requesting...", android.widget.Toast.LENGTH_SHORT).show()
                connectionsClient.requestConnection(userName, endpointId, lifecycleCallback)
            } else {
                Log.d("Nearby", "Waiting for ${info.endpointName} to initiate")
                android.widget.Toast.makeText(context, "Found Peer: ${info.endpointName}, Waiting...", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d("Nearby", "Connection initiated with $endpointId (${info.endpointName})")
            android.widget.Toast.makeText(context, "Connecting to ${info.endpointName}...", android.widget.Toast.LENGTH_SHORT).show()
            
            pendingConnections[endpointId] = info.endpointName
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.d("Nearby", "Connected to $endpointId")
                val name = pendingConnections.remove(endpointId) ?: "Peer"
                android.widget.Toast.makeText(context, "Successfully Connected to $name!", android.widget.Toast.LENGTH_SHORT).show()
                _connectedPeers.value = _connectedPeers.value + (endpointId to name)
            } else {
                Log.e("Nearby", "Connection failed: ${result.status}")
                val reason = when(result.status.statusCode) {
                    8003 -> "Already connected"
                    8004 -> "Connection refused"
                    else -> "Reason: ${result.status.statusMessage ?: "Error ${result.status.statusCode}"}"
                }
                android.widget.Toast.makeText(context, "Connection Failed: $reason", android.widget.Toast.LENGTH_LONG).show()
                pendingConnections.remove(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("Nearby", "Disconnected from $endpointId")
            _connectedPeers.value = _connectedPeers.value - endpointId
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes()?.let { String(it) } ?: return
                handleReceivedData(data, endpointId)
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun handleReceivedData(data: String, fromEndpointId: String) {
        try {
            val json = JSONObject(data)
            val msgId = json.getString("id")
            if (seenMessageIds.contains(msgId)) return
            seenMessageIds.add(msgId)

            val sender = json.getString("sender")
            val content = json.getString("content")
            val type = json.getString("type")
            val destination = json.optString("destination", "BROADCAST")

            val chatMsg = ChatMessage(msgId, sender, content, type == "EMERGENCY")
            _messages.value = _messages.value + chatMsg

            // Relay logic
            if (destination == "BROADCAST" || destination != userName) {
                relayMessage(data, fromEndpointId)
            }

        } catch (e: Exception) {
            Log.e("Nearby", "Error parsing payload", e)
        }
    }

    private fun relayMessage(data: String, sourceEndpointId: String) {
        val payload = Payload.fromBytes(data.toByteArray())
        _connectedPeers.value.keys.filter { it != sourceEndpointId }.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    fun sendMessage(content: String, isEmergency: Boolean = false) {
        val msgId = UUID.randomUUID().toString()
        seenMessageIds.add(msgId)
        
        val json = JSONObject().apply {
            put("id", msgId)
            put("sender", userName)
            put("content", content)
            put("type", if (isEmergency) "EMERGENCY" else "TEXT")
            put("destination", "BROADCAST")
        }
        
        val data = json.toString()
        val chatMsg = ChatMessage(msgId, userName, content, isEmergency)
        _messages.value = _messages.value + chatMsg
        
        val payload = Payload.fromBytes(data.toByteArray())
        _connectedPeers.value.keys.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }
}

data class ChatMessage(val id: String, val sender: String, val content: String, val isEmergency: Boolean)
