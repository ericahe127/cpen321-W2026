package com.example.cpen321application.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private enum class M1Screen {
    LoginInfo,
    Pixels,
    Timer
}

private val NavigationButtonHeight = 64.dp

@Composable
fun M1App(
    serverAddress: String,
    useHttps: Boolean,
    googleClientId: String,
    modifier: Modifier = Modifier
) {
    var selectedScreen by remember { mutableStateOf(M1Screen.LoginInfo) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "CPEN 321 M1",
            style = MaterialTheme.typography.headlineMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavigationButton(
                label = "Login + Server",
                selected = selectedScreen == M1Screen.LoginInfo,
                onClick = { selectedScreen = M1Screen.LoginInfo },
                modifier = Modifier.weight(1f)
            )
            NavigationButton(
                label = "Live Updates",
                selected = selectedScreen == M1Screen.Pixels,
                onClick = { selectedScreen = M1Screen.Pixels },
                modifier = Modifier.weight(1f)
            )
            NavigationButton(
                label = "Timer",
                selected = selectedScreen == M1Screen.Timer,
                onClick = { selectedScreen = M1Screen.Timer },
                modifier = Modifier.weight(1f)
            )
        }

        when (selectedScreen) {
            M1Screen.LoginInfo -> LoginServerScreen(
                serverAddress = serverAddress,
                useHttps = useHttps,
                googleClientId = googleClientId
            )
            M1Screen.Pixels -> LiveUpdatesScreen(
                serverAddress = serverAddress,
                useHttps = useHttps
            )
            M1Screen.Timer -> TimerScreen()
        }
    }
}

@Composable
private fun NavigationButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonModifier = modifier.height(NavigationButtonHeight)

    if (selected) {
        Button(onClick = onClick, modifier = buttonModifier) {
            Text(
                text = label,
                textAlign = TextAlign.Center
            )
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = buttonModifier) {
            Text(
                text = label,
                textAlign = TextAlign.Center
            )
        }
    }
}
