package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
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
 * @property baseUrl The base URL for the service (to be set by each implementing service client)
 * @constructor Creates a new instance of the service client with the specified HTTP client and base
 * URL
 */
abstract class ServiceClient(protected val client: HttpClient, protected val baseUrl: String) {
    /**
     * Performs an HTTP request to the specified endpoint and handles response processing.
     * This is a non-inline helper method to avoid logger access issues in inline functions.
     */
    protected suspend fun <T> handleHttpRequest(
        methodName: String,
        endpoint: String,
        call: suspend () -> HttpResponse,
        transform: suspend (HttpResponse) -> T
    ): T {
        try {
            logger.debug { "Making $methodName request to $baseUrl$endpoint" }
            val response = call()
            
            logger.debug { "Response status: ${response.status}, Content-Type: ${response.contentType()}" }
            
            if (!response.status.isSuccess()) {
                logger.warn { "Non-success status code: ${response.status} from $baseUrl$endpoint" }
                throw ServiceException(response.status.value, "Service returned error status: ${response.status}")
            }
            
            return transform(response)
        } catch (e: ServiceException) {
            // Already formatted, just rethrow
            throw e
        } catch (e: NoTransformationFoundException) {
            // This specifically handles the case in the error logs
            logger.error(e) { "Error deserializing response from $baseUrl$endpoint: ${e.message}" }
            throw ServiceException(
                HttpStatusCode.InternalServerError.value,
                "Failed to deserialize response: ${e.message}"
            )
        } catch (e: Exception) {
            logger.error(e) { "Error in $methodName request to $baseUrl$endpoint: ${e.message}" }
            throw ServiceException(
                HttpStatusCode.InternalServerError.value,
                "Failed to process response: ${e.message}"
            )
        }
    }

    /**
     * Performs an HTTP GET request to the specified endpoint.
     *
     * @param endpoint The API endpoint to call (will be appended to the base URL)
     * @param headers Optional HTTP headers to include in the request
     * @return The deserialized response body of type T
     * @throws ServiceException if the request fails
     */
    protected suspend inline fun <reified T> get(
        endpoint: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        return handleHttpRequest(
            "GET",
            endpoint,
            {
                client.get("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                }
            },
            { response -> response.body() }
        )
    }

    /**
     * Performs an HTTP POST request to the specified endpoint.
     *
     * @param endpoint The API endpoint to call (will be appended to the base URL)
     * @param body Optional request body to send (will be serialized to JSON)
     * @param headers Optional HTTP headers to include in the request
     * @return The deserialized response body of type T
     * @throws ServiceException if the request fails
     */
    protected suspend inline fun <reified T> post(
        endpoint: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): T {
        return handleHttpRequest(
            "POST",
            endpoint,
            {
                client.post("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            },
            { response -> response.body() }
        )
    }

    /**
     * Performs an HTTP PUT request to the specified endpoint.
     *
     * @param endpoint The API endpoint to call (will be appended to the base URL)
     * @param body Optional request body to send (will be serialized to JSON)
     * @param headers Optional HTTP headers to include in the request
     * @return The deserialized response body of type T
     * @throws ServiceException if the request fails
     */
    protected suspend inline fun <reified T> put(
        endpoint: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): T {
        return handleHttpRequest(
            "PUT",
            endpoint,
            {
                client.put("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            },
            { response -> response.body() }
        )
    }

    /**
     * Performs an HTTP DELETE request to the specified endpoint.
     *
     * @param endpoint The API endpoint to call (will be appended to the base URL)
     * @param headers Optional HTTP headers to include in the request
     * @return The deserialized response body of type T
     * @throws ServiceException if the request fails
     */
    protected suspend inline fun <reified T> delete(
        endpoint: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        return handleHttpRequest(
            "DELETE",
            endpoint,
            {
                client.delete("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                }
            },
            { response -> response.body() }
        )
    }

    /**
     * Performs an HTTP PATCH request to the specified endpoint.
     *
     * @param endpoint The API endpoint to call (will be appended to the base URL)
     * @param body Optional request body to send (will be serialized to JSON)
     * @param headers Optional HTTP headers to include in the request
     * @return The deserialized response body of type T
     * @throws ServiceException if the request fails
     */
    protected suspend inline fun <reified T> patch(
        endpoint: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): T {
        return handleHttpRequest(
            "PATCH",
            endpoint,
            {
                client.patch("$baseUrl$endpoint") {
                    headers.forEach { (key, value) -> header(key, value) }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            },
            { response -> response.body() }
        )
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
