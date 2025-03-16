package com.mad.gateway.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*

fun Application.configureHttpClient() {
    // This function is called from Application.kt to ensure the HTTP client is configured
    // The actual client is created in the createHttpClient function and injected via Koin
}

fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        // Configure request timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 30000 // 30 seconds
            connectTimeoutMillis = 15000 // 15 seconds
            socketTimeoutMillis = 60000 // 60 seconds
        }

        // Configure content negotiation with GSON
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
                serializeNulls()
                // Add any custom type adapters here if needed
            }
        }

        // Configure logging
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        // Configure default request
        defaultRequest {
            // Add common headers here if needed
            headers.append("Accept", "application/json")
            headers.append("Content-Type", "application/json")
        }

        // Configure engine
        engine {
            requestTimeout = 30000 // 30 seconds
            maxConnectionsCount = 1000
            endpoint {
                connectTimeout = 15000 // 15 seconds
                connectAttempts = 3
            }
        }
    }
}
