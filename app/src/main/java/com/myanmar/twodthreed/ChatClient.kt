package com.myanmar.twodthreed

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class ChatMessage(val id: String, val username: String, val text: String)

internal class ChatClient(private val serverUrl: String) {
    private var socket: Socket? = null

    fun connect(
        onConnected: (Boolean) -> Unit,
        onHistory: (List<ChatMessage>) -> Unit,
        onMessage: (ChatMessage) -> Unit,
        onError: (String) -> Unit
    ) {
        if (serverUrl.isBlank()) {
            onConnected(false)
            onError("Chat server URL မသတ်မှတ်ရသေးပါ။")
            return
        }
        try {
            val current = IO.socket(serverUrl)
            socket = current
            current.on(Socket.EVENT_CONNECT) { onConnected(true) }
            current.on(Socket.EVENT_DISCONNECT) { onConnected(false) }
            current.on(Socket.EVENT_CONNECT_ERROR) { onConnected(false); onError("Chat server ကို ချိတ်ဆက်မရပါ။") }
            current.on("chat:history") { args ->
                val array = args.firstOrNull() as? JSONArray ?: return@on
                onHistory((0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toChatMessage() })
            }
            current.on("chat:message") { args ->
                val message = (args.firstOrNull() as? JSONObject)?.toChatMessage() ?: return@on
                onMessage(message)
            }
            current.on("chat:error") { args ->
                val message = args.firstOrNull() as? String
                if (message != null) onError(message)
            }
            current.connect()
        } catch (_: Exception) {
            onConnected(false)
            onError("Chat ချိတ်ဆက်မှုကို စတင်မရပါ။")
        }
    }

    fun send(username: String, text: String) {
        val safeName = username.trim().ifBlank { "ဧည့်သည်" }.take(24)
        val safeText = text.trim().take(500)
        if (safeText.isNotEmpty()) socket?.emit("chat:send", JSONObject().put("username", safeName).put("text", safeText))
    }

    fun close() {
        socket?.disconnect()
        socket?.close()
        socket = null
    }
}

private fun JSONObject.toChatMessage(): ChatMessage = ChatMessage(
    id = optString("id").ifBlank { UUID.randomUUID().toString() },
    username = optString("username", "ဧည့်သည်"),
    text = optString("text")
)
