package com.example.cpen321application.network

import com.example.cpen321application.model.DogImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val RandomDogImageUrl = "https://dog.ceo/api/breeds/image/random"

suspend fun fetchRandomDogImage(): DogImage = withContext(Dispatchers.IO) {
    val response = fetchJson(RandomDogImageUrl)
    val status = response.optString("status")
    val imageUrl = response.optString("message")

    if (status != "success" || imageUrl.isBlank()) {
        throw IllegalStateException("Dog image API returned an unexpected response.")
    }

    DogImage(imageUrl = imageUrl)
}
