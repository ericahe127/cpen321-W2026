package com.example.cpen321application.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cpen321application.network.PixelWebSocketClient

private const val PixelGridSize = 16
private val EmptyPixelColor = Color(0xffd0d3dc)

@Composable
fun LiveUpdatesScreen(serverAddress: String, useHttps: Boolean) {
    val pixels = remember {
        mutableStateListOf<Color>().apply {
            repeat(PixelGridSize * PixelGridSize) {
                add(EmptyPixelColor)
            }
        }
    }

    DisposableEffect(serverAddress, useHttps) {
        val client = PixelWebSocketClient(
            serverAddress = serverAddress,
            useHttps = useHttps,
            onPixelUpdate = { update ->
                pixels[update.y * PixelGridSize + update.x] = update.color
            }
        )
        client.connect()

        onDispose {
            client.disconnect()
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Live Pixel Updates",
                style = MaterialTheme.typography.titleLarge
            )
            PixelGrid(pixels)
        }
    }
}

@Composable
private fun PixelGrid(pixels: List<Color>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(1.dp, MaterialTheme.colorScheme.outline),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (y in 0 until PixelGridSize) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                for (x in 0 until PixelGridSize) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(pixels[y * PixelGridSize + x])
                    )
                }
            }
        }
    }
}
