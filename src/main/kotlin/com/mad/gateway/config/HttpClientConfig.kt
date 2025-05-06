package com.mad.gateway.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.statement.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun Application.configureHttpClient() {
    // This function is called from Application.kt to ensure the HTTP client is configured
    // The actual client is created in the createHttpClient function and injected via Koin
}

/**
 * Creates and configures an HTTP client with improved resilience and error handling. The client is
 * configured to gracefully handle network issues, including DNS resolution failures.
 */
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
            headers.append("Accept-Charset", "UTF-8")
        }

        // Configure the client to throw exceptions for non-2xx responses
        // Our ServiceClient will handle these exceptions appropriately
        expectSuccess = true

        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, request ->
                when (exception) {
                    is NoTransformationFoundException -> {
                        // Special handling for deserialization issues
                        logger.error {
                            "Failed to deserialize response from ${request.url}: ${exception.message}"
                        }
                    }
                    is ClientRequestException -> {
                        val status = exception.response.status
                        if (status == HttpStatusCode.NotFound) {
                            logger.info { "Resource not found at ${request.url}: ${status}" }
                        } else {
                            logger.warn { "Client error for request to ${request.url}: ${status}" }
                        }
                    }
                    is ServerResponseException -> {
                        logger.error { "Server error for request to ${request.url}: ${exception.response.status}" }
                    }
                    else -> {
                        logger.error(exception) { "Request to ${request.url} failed: ${exception.message}" }
                    }
                }
                // Let ServiceClient handle these exceptions properly
            }
        }

        // Configure engine
        engine {
            requestTimeout = 30000 // 30 seconds
            maxConnectionsCount = 1000
            endpoint {
                connectTimeout = 15000 // 15 seconds
                connectAttempts = 3

                // Handle DNS resolution timeout
                keepAliveTime = 5000
                socketTimeout = 15000
            }
        }
    }
}
