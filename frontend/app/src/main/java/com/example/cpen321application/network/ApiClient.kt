package com.example.cpen321application.network

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

fun fetchJson(url: String): JSONObject {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 5_000
        readTimeout = 5_000
    }

    return connection.use {
        val responseBody = if (responseCode in 200..299) {
            inputStream.bufferedReader().use { reader -> reader.readText() }
        } else {
            val errorBody = errorStream?.bufferedReader()?.use { reader -> reader.readText() }
            throw IllegalStateException("HTTP $responseCode from $url${errorBody?.let { ": $it" } ?: ""}")
        }

        JSONObject(responseBody)
    }
}

fun postJson(url: String, body: JSONObject): JSONObject {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 5_000
        readTimeout = 5_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
    }

    return connection.use {
        outputStream.bufferedWriter().use { writer ->
            writer.write(body.toString())
        }

        val responseBody = if (responseCode in 200..299) {
            inputStream.bufferedReader().use { reader -> reader.readText() }
        } else {
            val errorBody = errorStream?.bufferedReader()?.use { reader -> reader.readText() }
            throw IllegalStateException("HTTP $responseCode from $url${errorBody?.let { ": $it" } ?: ""}")
        }

        JSONObject(responseBody)
    }
}

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}
