package com.example.cpen321application.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cpen321application.auth.GoogleAuthClient
import com.example.cpen321application.auth.googleAuthErrorMessage
import com.example.cpen321application.model.ServerInfo
import com.example.cpen321application.network.verifyGoogleIdToken
import com.example.cpen321application.network.fetchServerInfo
import kotlinx.coroutines.launch

@Composable
fun LoginServerScreen(
    serverAddress: String,
    useHttps: Boolean,
    googleClientId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var info by remember { mutableStateOf<ServerInfo?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val googleAuthClient = remember { GoogleAuthClient(context) }

    fun signOut() {
        coroutineScope.launch {
            isLoading = true
            errorText = null
            try {
                googleAuthClient.signOut()
                info = null
            } catch (e: Exception) {
                errorText = "Google sign-out failed: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                isLoading = false
            }
        }
    }

    fun signInAndLoadServerInfo() {
        coroutineScope.launch {
            isLoading = true
            errorText = null
            try {
                val signInResult = googleAuthClient.signIn(
                    activityContext = context,
                    webClientId = googleClientId
                )
                val verifiedUser = verifyGoogleIdToken(
                    serverAddress = serverAddress,
                    useHttps = useHttps,
                    idToken = signInResult.idToken,
                    nonce = signInResult.nonce
                )
                info = fetchServerInfo(
                    serverAddress = serverAddress,
                    useHttps = useHttps,
                    googleName = verifiedUser.name
                        .ifBlank { signInResult.displayName ?: signInResult.email ?: "Google user" }
                )
            } catch (e: Exception) {
                errorText = googleAuthErrorMessage(e)
            } finally {
                isLoading = false
            }
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
                text = "Login + Server Info",
                style = MaterialTheme.typography.titleLarge
            )
            if (info == null) {
                Button(
                    enabled = !isLoading,
                    onClick = { signInAndLoadServerInfo() }
                ) {
                    Text(if (isLoading) "Loading..." else "Sign in with Google")
                }
            } else {
                OutlinedButton(
                    enabled = !isLoading,
                    onClick = { signOut() }
                ) {
                    Text(if (isLoading) "Signing out..." else "Sign out")
                }
            }
            errorText?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            info?.let { loadedInfo ->
                Spacer(Modifier.height(4.dp))
                InfoRow("Google name", loadedInfo.googleName)
                InfoRow("Server public IP", loadedInfo.serverIp)
                InfoRow("Server local time", loadedInfo.serverTime)
                InfoRow("Owner name", loadedInfo.ownerName)
                InfoRow("Client IP", loadedInfo.clientIp)
                InfoRow("Client local time", loadedInfo.clientTime)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
