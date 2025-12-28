package com.budmash.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

expect fun createHttpClient(): HttpClient

object MenuFetcher {
    private val client by lazy { createHttpClient() }

    suspend fun fetchMenuHtml(url: String): Result<String> {
        return try {
            val response: HttpResponse = client.get(url)
            Result.success(response.bodyAsText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
