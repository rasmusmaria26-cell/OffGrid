package com.servify.resqmesh

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.servify.resqmesh.ui.theme.OffGridMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class NearbyManager(private val context: Context, var userName: String) {

    companion object {
        fun generateNickname(): String {
            val colors = listOf("Blue", "Red", "Green", "Electric", "Dark", "Neon", "Silent", "Rapid")
            val animals = listOf("Fox", "Wolf", "Hawk", "Bear", "Lynx", "Eagle", "Panther", "Tiger")
            val color = colors.random()
            val animal = animals.random()
            val id = (1000..9999).random()
            return "$color$animal #$id"
        }
    }

    init {
        if (userName.isBlank()) {
            userName = generateNickname()
        }
    }

    // ─── Crypto Setup ──────────────────────────────────────────────────────────
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val myPublicKeyB64: String = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

    /** nickname → PublicKey of that peer */
    private val peerPublicKeys = mutableMapOf<String, PublicKey>()

    /** Expose known DM-capable peers (have exchanged keys) */
    val encryptedPeers: Set<String> get() = peerPublicKeys.keys

    // ─── Nearby ────────────────────────────────────────────────────────────────
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val SERVICE_ID = "com.servify.resqmesh.SERVICE_ID"
    private val STRATEGY = Strategy.P2P_CLUSTER

    // ─── State ─────────────────────────────────────────────────────────────────
    private val _mode = MutableStateFlow(OffGridMode.FESTIVAL)
    val mode = _mode.asStateFlow()
    fun setMode(mode: OffGridMode) { _mode.value = mode }

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _connectedPeers = MutableStateFlow<Map<String, String>>(emptyMap())
    val connectedPeers = _connectedPeers.asStateFlow()

    private val pendingConnections = mutableMapOf<String, String>()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _emergencyAlert = MutableStateFlow<ChatMessage?>(null)
    val emergencyAlert = _emergencyAlert.asStateFlow()
    fun dismissEmergency() { _emergencyAlert.value = null }

    private val seenMessageIds = mutableSetOf<String>()

    // ─── Mesh Control ──────────────────────────────────────────────────────────
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
        peerPublicKeys.clear()
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
                val errorMsg = when ((e as? com.google.android.gms.common.api.ApiException)?.statusCode) {
                    8001 -> "Location off or no permissions"
                    8002 -> "Bluetooth off"
                    else -> e.message
                }
                android.widget.Toast.makeText(context, "Discovery Error: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
            }
    }

    // ─── Callbacks ─────────────────────────────────────────────────────────────
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.endpointName == userName) return
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
                val name = pendingConnections.remove(endpointId) ?: "Peer"
                Log.d("Nearby", "Connected to $name ($endpointId)")
                android.widget.Toast.makeText(context, "Connected to $name!", android.widget.Toast.LENGTH_SHORT).show()
                _connectedPeers.value = _connectedPeers.value + (endpointId to name)
                // Send our public key immediately upon successful connection
                sendKeyExchange(endpointId)
            } else {
                Log.e("Nearby", "Connection failed: ${result.status}")
                val reason = when (result.status.statusCode) {
                    8003 -> "Already connected"
                    8004 -> "Connection refused"
                    else -> "Error ${result.status.statusCode}"
                }
                android.widget.Toast.makeText(context, "Connection Failed: $reason", android.widget.Toast.LENGTH_LONG).show()
                pendingConnections.remove(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            val name = _connectedPeers.value[endpointId]
            Log.d("Nearby", "Disconnected from $endpointId")
            _connectedPeers.value = _connectedPeers.value - endpointId
            if (name != null) peerPublicKeys.remove(name)
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

    // ─── Key Exchange ──────────────────────────────────────────────────────────
    private fun sendKeyExchange(toEndpointId: String) {
        val json = JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("type", "KEY_EXCHANGE")
            put("sender", userName)
            put("publicKey", myPublicKeyB64)
            put("destination", "DIRECT")
            put("hops", 0)
        }
        connectionsClient.sendPayload(toEndpointId, Payload.fromBytes(json.toString().toByteArray()))
    }

    // ─── Message Handling ──────────────────────────────────────────────────────
    private fun handleReceivedData(data: String, fromEndpointId: String) {
        try {
            val json = JSONObject(data)
            val msgId = json.getString("id")
            if (seenMessageIds.contains(msgId)) return
            seenMessageIds.add(msgId)

            val type = json.getString("type")
            val sender = json.getString("sender")
            val destination = json.optString("destination", "BROADCAST")
            val hops = json.optInt("hops", 0)

            // Handle key exchange — don't show in chat, don't relay
            if (type == "KEY_EXCHANGE") {
                val pubKeyB64 = json.getString("publicKey")
                val pubKeyBytes = Base64.decode(pubKeyB64, Base64.NO_WRAP)
                val publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(X509EncodedKeySpec(pubKeyBytes))
                peerPublicKeys[sender] = publicKey
                Log.d("Nearby", "Stored public key for $sender")
                return
            }

            // For DMs: only display if I am the intended recipient (or if it's from me)
            val isDirect = destination != "BROADCAST"
            if (isDirect && destination != userName && sender != userName) {
                // This DM is not for me — relay it but don't store or show
                json.put("hops", hops + 1)
                relayMessage(json.toString(), fromEndpointId)
                return
            }

            // Decrypt content if it's an encrypted DM
            val rawContent = json.getString("content")
            val displayContent = if (isDirect && json.has("encryptedAesKey")) {
                tryDecrypt(json)
            } else {
                rawContent
            }

            val chatMsg = ChatMessage(
                id = msgId,
                sender = sender,
                content = displayContent ?: "[Encrypted message]",
                isEmergency = type == "EMERGENCY",
                hops = hops,
                isDirect = isDirect,
                recipient = if (isDirect) destination else null
            )
            _messages.value = _messages.value + chatMsg

            if (type == "EMERGENCY" && sender != userName) {
                _emergencyAlert.value = chatMsg
            }

            // Relay broadcasts (not DMs, they were handled above)
            if (!isDirect) {
                json.put("hops", hops + 1)
                relayMessage(json.toString(), fromEndpointId)
            }

        } catch (e: Exception) {
            Log.e("Nearby", "Error parsing payload", e)
        }
    }

    private fun tryDecrypt(json: JSONObject): String? {
        return try {
            val encryptedAesKeyB64 = json.getString("encryptedAesKey")
            val encryptedContentB64 = json.getString("content")

            // Decrypt AES key with our RSA private key
            val encryptedAesKey = Base64.decode(encryptedAesKeyB64, Base64.NO_WRAP)
            val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            rsaCipher.init(Cipher.DECRYPT_MODE, keyPair.private)
            val aesKeyBytes = rsaCipher.doFinal(encryptedAesKey)
            val aesKey: SecretKey = SecretKeySpec(aesKeyBytes, "AES")

            // Decrypt content with AES key
            val encryptedContent = Base64.decode(encryptedContentB64, Base64.NO_WRAP)
            val aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey)
            String(aesCipher.doFinal(encryptedContent))
        } catch (e: Exception) {
            Log.e("Nearby", "Decryption failed", e)
            null
        }
    }

    private fun relayMessage(data: String, sourceEndpointId: String) {
        val payload = Payload.fromBytes(data.toByteArray())
        _connectedPeers.value.keys.filter { it != sourceEndpointId }.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    // ─── Public Send API ───────────────────────────────────────────────────────

    /** Broadcast to all peers (no encryption) */
    fun sendMessage(content: String, isEmergency: Boolean = false) {
        val msgId = UUID.randomUUID().toString()
        seenMessageIds.add(msgId)

        val json = JSONObject().apply {
            put("id", msgId)
            put("sender", userName)
            put("content", content)
            put("type", if (isEmergency) "EMERGENCY" else "TEXT")
            put("destination", "BROADCAST")
            put("hops", 0)
        }

        _messages.value = _messages.value + ChatMessage(msgId, userName, content, isEmergency, 0, isDirect = false)

        val payload = Payload.fromBytes(json.toString().toByteArray())
        _connectedPeers.value.keys.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    /** Send a targeted, E2E encrypted direct message to a specific peer nickname */
    fun sendDirectMessage(content: String, recipientNickname: String) {
        val publicKey = peerPublicKeys[recipientNickname]
        val msgId = UUID.randomUUID().toString()
        seenMessageIds.add(msgId)

        val json = JSONObject().apply {
            put("id", msgId)
            put("sender", userName)
            put("type", "TEXT")
            put("destination", recipientNickname)
            put("hops", 0)
        }

        if (publicKey != null) {
            // Hybrid encryption: generate random AES key → encrypt content → encrypt AES key with RSA
            try {
                val aesKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

                val aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                aesCipher.init(Cipher.ENCRYPT_MODE, aesKey)
                val encryptedContent = Base64.encodeToString(aesCipher.doFinal(content.toByteArray()), Base64.NO_WRAP)

                val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
                rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
                val encryptedAesKey = Base64.encodeToString(rsaCipher.doFinal(aesKey.encoded), Base64.NO_WRAP)

                json.put("content", encryptedContent)
                json.put("encryptedAesKey", encryptedAesKey)
            } catch (e: Exception) {
                Log.e("Nearby", "Encryption failed, sending plaintext", e)
                json.put("content", content)
            }
        } else {
            // No key yet — send plaintext DM (still targeted)
            json.put("content", content)
        }

        // Show in our own chat with a 🔒 label
        val displayContent = if (publicKey != null) "🔒 $content" else content
        _messages.value = _messages.value + ChatMessage(
            id = msgId,
            sender = userName,
            content = displayContent,
            isEmergency = false,
            hops = 0,
            isDirect = true,
            recipient = recipientNickname
        )

        val payload = Payload.fromBytes(json.toString().toByteArray())
        _connectedPeers.value.keys.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }
}

data class ChatMessage(
    val id: String,
    val sender: String,
    val content: String,
    val isEmergency: Boolean,
    val hops: Int = 0,
    val isDirect: Boolean = false,
    val recipient: String? = null
)
