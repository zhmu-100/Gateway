package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import java.time.Instant
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Logging service (rollbar-like) */
class LoggingServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String =
            application.environment.config.property("services.logging.url").getString()

    /** Log an info message */
    suspend fun logInfo(message: String, metadata: Map<String, Any> = emptyMap()): LogResponse {
        return log(LogLevel.INFO, message, metadata)
    }

    /** Log a warning message */
    suspend fun logWarning(message: String, metadata: Map<String, Any> = emptyMap()): LogResponse {
        return log(LogLevel.WARNING, message, metadata)
    }

    /** Log an error message */
    suspend fun logError(
            message: String,
            error: Throwable? = null,
            metadata: Map<String, Any> = emptyMap()
    ): LogResponse {
        val errorMetadata =
                if (error != null) {
                    metadata +
                            mapOf(
                                    "errorType" to error.javaClass.name,
                                    "errorMessage" to (error.message ?: ""),
                                    "stackTrace" to error.stackTraceToString()
                            )
                } else {
                    metadata
                }

        return log(LogLevel.ERROR, message, errorMetadata)
    }

    /** Log a debug message */
    suspend fun logDebug(message: String, metadata: Map<String, Any> = emptyMap()): LogResponse {
        return log(LogLevel.DEBUG, message, metadata)
    }

    /** Log a message with the specified level */
    private suspend fun log(
            level: LogLevel,
            message: String,
            metadata: Map<String, Any>
    ): LogResponse {
        logger.info { "Sending log to logging service: [$level] $message" }

        val request =
                LogRequest(
                        level = level,
                        message = message,
                        timestamp = Instant.now().toString(),
                        service = "gateway",
                        metadata = metadata
                )
        return post("/log", request)
    }

    /** Search logs with filters */
    suspend fun searchLogs(
            level: LogLevel? = null,
            service: String? = null,
            startTime: String? = null,
            endTime: String? = null,
            message: String? = null,
            page: Int = 1,
            pageSize: Int = 20
    ): LogSearchResponse {
        logger.info {
            "Searching logs with filters: level=$level, service=$service, message=$message"
        }

        val queryParams = mutableMapOf<String, String>()
        level?.let { queryParams["level"] = it.name }
        service?.let { queryParams["service"] = it }
        startTime?.let { queryParams["startTime"] = it }
        endTime?.let { queryParams["endTime"] = it }
        message?.let { queryParams["message"] = it }
        queryParams["page"] = page.toString()
        queryParams["pageSize"] = pageSize.toString()

        val queryString = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }

        return get("/search?$queryString")
    }

    /** Get log details by ID */
    suspend fun getLog(id: String): LogEntry {
        logger.info { "Getting log details for ID: $id" }
        return get("/logs/$id")
    }
}

// Data classes for requests and responses

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

data class LogRequest(
        val level: LogLevel,
        val message: String,
        val timestamp: String,
        val service: String,
        val metadata: Map<String, Any>
)

data class LogResponse(val id: String, val success: Boolean, val message: String)

data class LogEntry(
        val id: String,
        val level: LogLevel,
        val message: String,
        val timestamp: String,
        val service: String,
        val metadata: Map<String, Any>
)

data class LogSearchResponse(
        val logs: List<LogEntry>,
        val total: Int,
        val page: Int,
        val pageSize: Int,
        val totalPages: Int
)
