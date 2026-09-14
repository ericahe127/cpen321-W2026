package com.example.cpen321application.network

import com.example.cpen321application.model.ServerInfo
import com.example.cpen321application.util.formatTimeWithGmtOffset
import com.example.cpen321application.util.normalizeIpAddress
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun fetchServerInfo(
    serverAddress: String,
    useHttps: Boolean,
    googleName: String
): ServerInfo = withContext(Dispatchers.IO) {
    val name = fetchJson(restUrl(serverAddress, useHttps, "/api/name"))
    val serverIp = fetchJson(restUrl(serverAddress, useHttps, "/api/server-ip"))
    val serverTime = fetchJson(restUrl(serverAddress, useHttps, "/api/server-time"))
    val clientIp = fetchJson(restUrl(serverAddress, useHttps, "/api/client-ip"))
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
