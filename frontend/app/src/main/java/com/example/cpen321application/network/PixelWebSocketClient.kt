package com.example.cpen321application.network

import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import com.example.cpen321application.model.PixelUpdate
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class PixelWebSocketClient(
    serverAddress: String,
    useHttps: Boolean,
    private val onPixelUpdate: (PixelUpdate) -> Unit
) {
    private val client = OkHttpClient()
    private val request = Request.Builder()
        .url(pixelWebSocketUrl(serverAddress, useHttps))
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var webSocket: WebSocket? = null

    fun connect() {
        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(text)
                }
            }
        )
    }

    fun disconnect() {
        webSocket?.close(1000, "Leaving pixel screen")
        webSocket = null
        client.dispatcher.executorService.shutdown()
    }

    private fun handleMessage(message: String) {
        mainHandler.post {
            parsePixelUpdate(message)?.let(onPixelUpdate)
        }
    }
}

private fun parsePixelUpdate(message: String): PixelUpdate? {
    return try {
        val json = JSONObject(message)
        val x = json.optInt("x", -1)
        val y = json.optInt("y", -1)
        val color = parseHexColor(json.optString("color"))

        if (x !in 0..15 || y !in 0..15 || color == null) {
            null
        } else {
            PixelUpdate(x = x, y = y, color = color)
        }
    } catch (_: Exception) {
        null
    }
}

private fun parseHexColor(value: String): Color? {
    val normalized = value.trim()
    val colorString = if (normalized.startsWith("#")) normalized else "#$normalized"
    return try {
        Color(AndroidColor.parseColor(colorString))
    } catch (_: IllegalArgumentException) {
        null
    }
}
