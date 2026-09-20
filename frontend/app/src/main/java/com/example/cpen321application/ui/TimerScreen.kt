package com.example.cpen321application.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.cpen321application.network.fetchRandomDogImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val SecondsPerMinute = 60

@Composable
fun TimerScreen() {
    var minutesText by remember { mutableStateOf("") }
    var secondsText by remember { mutableStateOf("") }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var dogImageUrl by remember { mutableStateOf<String?>(null) }
    var dogImageError by remember { mutableStateOf<String?>(null) }
    var isLoadingDogImage by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (!isRunning) {
            return@LaunchedEffect
        }

        if (remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds -= 1
        } else {
            isLoadingDogImage = true
            dogImageError = null
            dogImageUrl = null

            try {
                dogImageUrl = fetchRandomDogImage().imageUrl
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dogImageError = "Time's up! Dog surprise failed to load: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                isLoadingDogImage = false
                isRunning = false
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
                text = "Timer",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimerInput(
                    label = "Minutes",
                    value = minutesText,
                    enabled = !isRunning,
                    onValueChange = { minutesText = it.onlyDigits(maxLength = 3) },
                    modifier = Modifier.weight(1f)
                )
                TimerInput(
                    label = "Seconds",
                    value = secondsText,
                    enabled = !isRunning,
                    onValueChange = { secondsText = it.onlyDigits(maxLength = 2) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = formatCountdown(remainingSeconds),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !isRunning && enteredSeconds(minutesText, secondsText) > 0,
                    onClick = {
                        remainingSeconds = enteredSeconds(minutesText, secondsText)
                        isRunning = true
                        dogImageUrl = null
                        dogImageError = null
                        isLoadingDogImage = false
                    }
                ) {
                    Text("Start")
                }
                OutlinedButton(
                    enabled = isRunning || remainingSeconds > 0 || dogImageUrl != null || dogImageError != null || isLoadingDogImage,
                    onClick = {
                        isRunning = false
                        remainingSeconds = 0
                        dogImageUrl = null
                        dogImageError = null
                        isLoadingDogImage = false
                    }
                ) {
                    Text("Reset")
                }
            }

            if (isLoadingDogImage) {
                Text(
                    text = "Time's up! Fetching a dog surprise...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            dogImageUrl?.let { imageUrl ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Time's up! Here's your furry friend!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = "Random dog surprise",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (painter.state) {
                            is coil.compose.AsyncImagePainter.State.Loading,
                            is coil.compose.AsyncImagePainter.State.Empty -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Loading image...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            is coil.compose.AsyncImagePainter.State.Error -> {
                                Text(
                                    text = "The dog image URL loaded, but the image could not be displayed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            is coil.compose.AsyncImagePainter.State.Success -> {
                                SubcomposeAsyncImageContent()
                            }
                        }
                    }
                }
            }

            dogImageError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun TimerInput(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private fun String.onlyDigits(maxLength: Int): String =
    filter { it.isDigit() }.take(maxLength)

private fun enteredSeconds(minutesText: String, secondsText: String): Int {
    val minutes = minutesText.toIntOrNull() ?: 0
    val seconds = (secondsText.toIntOrNull() ?: 0).coerceIn(0, 59)
    return minutes * SecondsPerMinute + seconds
}

private fun formatCountdown(totalSeconds: Int): String {
    val minutes = totalSeconds / SecondsPerMinute
    val seconds = totalSeconds % SecondsPerMinute
    return "%02d:%02d".format(minutes, seconds)
}
