package com.example.cpen321application.network

import com.example.cpen321application.model.ServerInfo
import com.example.cpen321application.util.formatTimeWithGmtOffset
import com.example.cpen321application.util.normalizeIpAddress
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun fetchServerInfo(
    apiBaseUrl: String,
    googleName: String
): ServerInfo = withContext(Dispatchers.IO) {
    val baseUrl = apiBaseUrl.trimEnd('/')
    val name = fetchJson("$baseUrl/api/name")
    val serverIp = fetchJson("$baseUrl/api/server-ip")
    val serverTime = fetchJson("$baseUrl/api/server-time")
    val clientIp = fetchJson("$baseUrl/api/client-ip")
    val now = ZonedDateTime.now()

    ServerInfo(
        googleName = googleName,
        serverIp = normalizeIpAddress(serverIp.getString("ipAddress")),
        serverTime = formatTimeWithGmtOffset(serverTime.getString("localTime")),
        ownerName = "${name.getString("firstName")} ${name.getString("lastName")}",
        clientIp = normalizeIpAddress(clientIp.getString("ipAddress")),
        clientTime = formatTimeWithGmtOffset(now)
    )
}
