package com.example.cpen321application.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom
import java.util.Base64

data class GoogleCredentialResult(
    val idToken: String,
    val nonce: String,
    val displayName: String?,
    val email: String?
)

class GoogleAuthClient(context: Context) {
    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    suspend fun signIn(
        activityContext: Context,
        webClientId: String
    ): GoogleCredentialResult {
        if (webClientId.isBlank()) {
            throw IllegalStateException("Missing GOOGLE_CLIENT_ID in frontend/local.properties")
        }

        val nonce = generateNonce()
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val result = credentialManager.getCredential(
            context = activityContext.findActivity() ?: activityContext,
            request = request
        )
        val credential = result.credential

        return try {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleCredentialResult(
                idToken = googleCredential.idToken,
                nonce = nonce,
                displayName = googleCredential.displayName,
                email = googleCredential.id
            )
        } catch (e: GoogleIdTokenParsingException) {
            throw IllegalStateException("Google returned an invalid ID token.", e)
        }
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}

fun googleAuthErrorMessage(error: Throwable): String =
    when (error) {
        is GetCredentialCancellationException ->
            "Google sign-in was cancelled before an account was selected."
        is NoCredentialException ->
            "No Google credential is available on this device. Add a Google account, then try again."
        is GetCredentialException ->
            "Google sign-in failed: ${error.message ?: error.javaClass.simpleName}"
        else ->
            "Google sign-in failed: ${error.message ?: error.javaClass.simpleName}"
    }

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

private fun generateNonce(byteLength: Int = 32): String {
    val randomBytes = ByteArray(byteLength)
    SecureRandom().nextBytes(randomBytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
}
