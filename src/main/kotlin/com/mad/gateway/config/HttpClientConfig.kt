package com.mad.gateway.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** Creates and configures an HttpClient for making API requests to other services */
fun createHttpClient(): HttpClient {
    logger.info { "Creating HttpClient" }
    return HttpClient(CIO) {
        // Configure timeout
        install(HttpTimeout) {
            requestTimeoutMillis = 15000 // 15 seconds
            connectTimeoutMillis = 5000 // 5 seconds
        }

        // Configure JSON serialization
        install(ContentNegotiation) {
            json(
                    Json {
                        prettyPrint = false
                        isLenient = true // Accept malformed JSON
                        ignoreUnknownKeys = true // Ignore unknown keys in JSON responses
                        coerceInputValues = true // Coerce null values to defaults if possible
                    }
            )
        }

        // Log HTTP requests/responses
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        // Monitor responses
        install(ResponseObserver) {
            onResponse { response -> logger.debug { "HTTP response received: ${response.status}" } }
        }

        // Default request configuration
        defaultRequest {
            // Set headers for all requests
            headers { append("Accept", "application/json") }
        }
    }
}
