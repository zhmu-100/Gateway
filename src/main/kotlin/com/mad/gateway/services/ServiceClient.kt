package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** Base service client implementation */
abstract class ServiceClient(protected val client: HttpClient) {
    protected abstract val baseUrl: String

    // Standard HTTP methods with single type parameter
    protected suspend inline fun <reified T> get(
            endpoint: String,
            headers: Map<String, String> = emptyMap()
    ): T {
        return client
                .get("$baseUrl$endpoint") { headers.forEach { (key, value) -> header(key, value) } }
                .body()
    }

    protected suspend inline fun <reified T> post(
            endpoint: String,
            body: Any? = null,
            headers: Map<String, String> = emptyMap()
    ): T {
        return client
                .post("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                .body()
    }

    protected suspend inline fun <reified T> put(
            endpoint: String,
            body: Any? = null,
            headers: Map<String, String> = emptyMap()
    ): T {
        return client
                .put("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                .body()
    }

    protected suspend inline fun <reified T> delete(
            endpoint: String,
            headers: Map<String, String> = emptyMap()
    ): T {
        return client
                .delete("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                }
                .body()
    }

    protected suspend inline fun <reified T> patch(
            endpoint: String,
            body: Any? = null,
            headers: Map<String, String> = emptyMap()
    ): T {
        return client
                .patch("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                .body()
    }
}

/** Exception thrown when a service request fails */
class ServiceException(val statusCode: Int, val errorBody: String) :
        RuntimeException("Service request failed with status $statusCode: $errorBody")
