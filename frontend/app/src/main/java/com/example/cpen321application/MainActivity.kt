package com.example.cpen321application

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private enum class M1Screen {
    LoginInfo,
    Pixels,
    Timer
}

private val NavigationButtonHeight = 64.dp

private data class ButtonOneInfo(
    val googleName: String,
    val serverIp: String,
    val serverTime: String,
    val ownerName: String,
    val clientIp: String,
    val clientTime: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CPEN321ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    M1App(
                        apiBaseUrl = BuildConfig.API_BASE_URL,
                        googleClientId = BuildConfig.GOOGLE_CLIENT_ID,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun M1App(
    apiBaseUrl: String,
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
            M1Screen.LoginInfo -> ButtonOneScreen(
                apiBaseUrl = apiBaseUrl,
                googleClientId = googleClientId
            )
            M1Screen.Pixels -> PlaceholderScreen("Button 2 pixel relay is next.")
            M1Screen.Timer -> PlaceholderScreen("Button 3 timer is next.")
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

@Composable
private fun ButtonOneScreen(apiBaseUrl: String, googleClientId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var info by remember { mutableStateOf<ButtonOneInfo?>(null) }
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
        coroutineScope.launch {
            isLoading = true
            errorText = null
            try {
                withContext(Dispatchers.IO) {
                    googleClient.signOut().await()
                }
                info = null
            } catch (e: Exception) {
                errorText = "Google sign-out failed: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadButtonOneInfo(account: GoogleSignInAccount) {
        coroutineScope.launch {
            isLoading = true
            errorText = null
            try {
                info = fetchButtonOneInfo(
                    apiBaseUrl = apiBaseUrl,
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
        if (result.resultCode != Activity.RESULT_OK) {
            errorText = "Google sign-in was cancelled."
            return@rememberLauncherForActivityResult
        }

        try {
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            loadButtonOneInfo(account)
        } catch (e: ApiException) {
            errorText = "Google sign-in failed: ${e.statusCode}"
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

@Composable
private fun PlaceholderScreen(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private suspend fun fetchButtonOneInfo(
    apiBaseUrl: String,
    googleName: String
): ButtonOneInfo = withContext(Dispatchers.IO) {
    val baseUrl = apiBaseUrl.trimEnd('/')
    val name = fetchJson("$baseUrl/api/name")
    val serverIp = fetchJson("$baseUrl/api/server-ip")
    val serverTime = fetchJson("$baseUrl/api/server-time")
    val clientIp = fetchJson("$baseUrl/api/client-ip")
    val now = ZonedDateTime.now()

    ButtonOneInfo(
        googleName = googleName,
        serverIp = normalizeIpAddress(serverIp.getString("ipAddress")),
        serverTime = formatTimeWithGmtOffset(serverTime.getString("localTime")),
        ownerName = "${name.getString("firstName")} ${name.getString("lastName")}",
        clientIp = normalizeIpAddress(clientIp.getString("ipAddress")),
        clientTime = formatTimeWithGmtOffset(now)
    )
}

private fun formatTimeWithGmtOffset(value: String): String =
    formatTimeWithGmtOffset(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME))

private fun formatTimeWithGmtOffset(value: ZonedDateTime): String =
    value.format(TIME_WITH_GMT_OFFSET_FORMATTER)

private fun normalizeIpAddress(ipAddress: String): String =
    ipAddress.trim().removePrefix("::ffff:")

private val TIME_WITH_GMT_OFFSET_FORMATTER = DateTimeFormatterBuilder()
    .appendPattern("HH:mm:ss 'GMT'")
    .appendOffset("+HH:MM", "+00:00")
    .toFormatter()

private fun fetchJson(url: String): JSONObject {
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

private inline fun <T> HttpURLConnection.use(block: HttpURLConnection.() -> T): T {
    return try {
        block()
    } finally {
        disconnect()
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result) {}
        }
        addOnFailureListener { exception ->
            continuation.resumeWith(Result.failure(exception))
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
