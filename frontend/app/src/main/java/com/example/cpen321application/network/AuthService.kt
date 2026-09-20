package com.example.cpen321application.network

import com.example.cpen321application.model.GoogleUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

suspend fun verifyGoogleIdToken(
    serverAddress: String,
    useHttps: Boolean,
    idToken: String,
    nonce: String
): GoogleUser = withContext(Dispatchers.IO) {
    val response = postJson(
        url = restUrl(serverAddress, useHttps, "/api/auth/google"),
        body = JSONObject()
            .put("idToken", idToken)
            .put("nonce", nonce)
    )

    GoogleUser(
        name = response.optString("name").ifBlank { "Google user" },
        email = response.optString("email").ifBlank { null }
    )
}
