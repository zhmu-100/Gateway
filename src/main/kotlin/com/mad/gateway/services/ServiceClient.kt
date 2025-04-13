package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Base service client implementation.
 *
 * This abstract class provides a foundation for all service clients in the application. It
 * encapsulates common HTTP operations and handles the communication with backend services.
 *
 * @property client The Ktor HTTP client used for making requests
 */
abstract class ServiceClient(protected val client: HttpClient) {
        /**
         * The base URL for the service.
         *
         * This should be set by each implementing service client, typically from configuration.
         */
        protected abstract val baseUrl: String

        /**
         * Performs an HTTP GET request to the specified endpoint.
         *
         * @param endpoint The API endpoint to call (will be appended to the base URL)
         * @param headers Optional HTTP headers to include in the request
         * @return The deserialized response body of type T
         */
        protected suspend inline fun <reified T> get(
                endpoint: String,
                headers: Map<String, String> = emptyMap()
        ): T {
                return client
                        .get("$baseUrl$endpoint") {
                                headers.forEach { (key, value) -> header(key, value) }
                        }
                        .body()
        }

        /**
         * Performs an HTTP POST request to the specified endpoint.
         *
         * @param endpoint The API endpoint to call (will be appended to the base URL)
         * @param body Optional request body to send (will be serialized to JSON)
         * @param headers Optional HTTP headers to include in the request
         * @return The deserialized response body of type T
         */
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

        /**
         * Performs an HTTP PUT request to the specified endpoint.
         *
         * @param endpoint The API endpoint to call (will be appended to the base URL)
         * @param body Optional request body to send (will be serialized to JSON)
         * @param headers Optional HTTP headers to include in the request
         * @return The deserialized response body of type T
         */
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

        /**
         * Performs an HTTP DELETE request to the specified endpoint.
         *
         * @param endpoint The API endpoint to call (will be appended to the base URL)
         * @param headers Optional HTTP headers to include in the request
         * @return The deserialized response body of type T
         */
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

        /**
         * Performs an HTTP PATCH request to the specified endpoint.
         *
         * @param endpoint The API endpoint to call (will be appended to the base URL)
         * @param body Optional request body to send (will be serialized to JSON)
         * @param headers Optional HTTP headers to include in the request
         * @return The deserialized response body of type T
         */
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

/**
 * Exception thrown when a service request fails.
 *
 * This exception captures the HTTP status code and error response body to provide detailed
 * information about the failure.
 *
 * @property statusCode The HTTP status code of the failed request
 * @property errorBody The error response body as a string
 */
class ServiceException(val statusCode: Int, val errorBody: String) :
        RuntimeException("Service request failed with status $statusCode: $errorBody")
