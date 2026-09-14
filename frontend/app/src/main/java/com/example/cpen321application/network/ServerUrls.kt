package com.example.cpen321application.network

private const val PixelRelayPath = "/ws/pixels"

fun restUrl(serverAddress: String, useHttps: Boolean, path: String): String =
    "${if (useHttps) "https" else "http"}://${serverAddress.normalizedServerAddress()}${path.normalizedPath()}"

fun pixelWebSocketUrl(serverAddress: String, useHttps: Boolean): String =
    "${if (useHttps) "wss" else "ws"}://${serverAddress.normalizedServerAddress()}$PixelRelayPath"

private fun String.normalizedServerAddress(): String =
    trim()
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("wss://")
        .removePrefix("ws://")
        .trimEnd('/')

private fun String.normalizedPath(): String =
    if (startsWith("/")) this else "/$this"
