package com.mad.gateway.services

import java.time.Instant
import mu.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

/** Simple terminal logger */
class LoggingServiceClient {

    /** Log an info message */
    fun logInfo(message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, message, metadata)
    }

    /** Log a warning message */
    fun logWarning(message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.WARNING, message, metadata)
    }

    /** Log an error message */
    fun logError(
            message: String,
            error: Throwable? = null,
            metadata: Map<String, Any> = emptyMap()
    ) {
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

        log(LogLevel.ERROR, message, errorMetadata)
    }

    /** Log a debug message */
    fun logDebug(message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.DEBUG, message, metadata)
    }

    /** Log a message with the specified level */
    private fun log(level: LogLevel, message: String, metadata: Map<String, Any>) {
        val timestamp = Instant.now().toString()
        val metadataStr = if (metadata.isNotEmpty()) " - metadata: $metadata" else ""

        when (level) {
            LogLevel.DEBUG -> logger.debug { "[$timestamp] $message$metadataStr" }
            LogLevel.INFO -> logger.info { "[$timestamp] $message$metadataStr" }
            LogLevel.WARNING -> logger.warn { "[$timestamp] $message$metadataStr" }
            LogLevel.ERROR -> logger.error { "[$timestamp] $message$metadataStr" }
        }
    }
}

@Serializable
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}
