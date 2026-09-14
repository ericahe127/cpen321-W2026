package com.example.cpen321application.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.cpen321application.model.ServerInfo
import com.example.cpen321application.network.fetchServerInfo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
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

    val signInOptions = remember(googleClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .apply {
                if (googleClientId.isNotBlank()) {
                    requestIdToken(googleClientId)
                }
            }
            .build()
    }
    val googleClient = remember(signInOptions) {
        GoogleSignIn.getClient(context, signInOptions)
    }

    fun signOut() {
        isLoading = true
        errorText = null
        googleClient.signOut()
            .addOnSuccessListener {
                info = null
            }
            .addOnFailureListener { e ->
                errorText = "Google sign-out failed: ${e.message ?: e.javaClass.simpleName}"
            }
            .addOnCompleteListener {
                isLoading = false
            }
    }

    fun loadServerInfo(account: GoogleSignInAccount) {
        coroutineScope.launch {
            isLoading = true
            errorText = null
            try {
                info = fetchServerInfo(
                    serverAddress = serverAddress,
                    useHttps = useHttps,
                    googleName = account.displayName ?: account.email ?: "Google user"
                )
            } catch (e: Exception) {
                errorText = "Could not load Button 1 info: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                isLoading = false
            }
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            loadServerInfo(account)
        } catch (e: ApiException) {
            errorText = googleSignInErrorMessage(e.statusCode)
        } catch (e: Exception) {
            errorText = if (result.resultCode == Activity.RESULT_OK) {
                "Google sign-in failed: ${e.message ?: e.javaClass.simpleName}"
            } else {
                "Google sign-in was cancelled before an account was selected."
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
                    onClick = { signInLauncher.launch(googleClient.signInIntent) }
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

private fun googleSignInErrorMessage(statusCode: Int): String =
    when (statusCode) {
        GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
            "Google sign-in was cancelled before an account was selected."
        GoogleSignInStatusCodes.SIGN_IN_FAILED ->
            "Google sign-in failed. Check the OAuth client package name and SHA-1."
        GoogleSignInStatusCodes.NETWORK_ERROR ->
            "Google sign-in failed because of a network error."
        GoogleSignInStatusCodes.DEVELOPER_ERROR ->
            "Google sign-in failed: developer error. Check the Android OAuth client package name, SHA-1, and web client ID."
        else -> "Google sign-in failed: status code $statusCode"
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
