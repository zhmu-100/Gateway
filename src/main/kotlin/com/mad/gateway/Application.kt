package com.mad.gateway

import com.mad.gateway.config.*
import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Main entry point for the MAD Gateway application.
 *
 * This function initializes and starts the Ktor server with the Netty engine. It loads
 * configuration from application.conf and starts the server on the configured port (or 8080 by
 * default).
 */
fun main() {
    // Check for config file path from system property first
    val configFilePath = System.getProperty("config.file") ?: "application.conf"

    logger.info { "Loading configuration from $configFilePath" }

    // Create config with explicit environment variable handling
    val config =
            if (File(configFilePath).exists()) {
                logger.info { "Using configuration file at path: $configFilePath" }
                HoconApplicationConfig(
                        ConfigFactory.parseFile(File(configFilePath))
                                .withFallback(ConfigFactory.systemProperties())
                                .withFallback(ConfigFactory.systemEnvironment())
                                .resolve()
                )
            } else {
                logger.info {
                    "Configuration file not found at $configFilePath, using classpath resource"
                }
                HoconApplicationConfig(
                        ConfigFactory.load("application.conf")
                                .withFallback(ConfigFactory.systemProperties())
                                .withFallback(ConfigFactory.systemEnvironment())
                                .resolve()
                )
            }

    // Ensure critical environment variables are available
    checkEnvironmentVariables()

    val port = config.propertyOrNull("ktor.deployment.port")?.getString()?.toInt() ?: 8080

    logger.info { "Starting MAD Gateway on port $port" }

    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
}

/** Check that critical environment variables are present or log warnings */
fun checkEnvironmentVariables() {
    // List of critical environment variables
    val criticalVars = listOf("JWT_SECRET", "JWT_ISSUER", "JWT_AUDIENCE")

    criticalVars.forEach { varName ->
        val value = System.getenv(varName)
        if (value.isNullOrBlank()) {
            logger.warn {
                "Environment variable $varName is not set or is empty. This may cause configuration issues."
            }
        } else {
            logger.info { "Environment variable $varName is properly set" }
        }
    }
}

/**
 * Application module configuration function.
 *
 * This function configures all aspects of the Ktor application:
 * - Serialization for request/response handling
 * - Monitoring for logging and metrics
 * - Security for authentication and authorization
 * - Dependency injection with Koin
 * - HTTP client for communicating with backend services
 * - Routing for defining API endpoints
 */
fun Application.module() {
    // Log application startup
    logger.info { "Initializing MAD Gateway" }

    // Configure serialization
    configureSerialization()
    logger.debug { "Serialization configured" }

    // Configure monitoring (logging, metrics)
    configureMonitoring()
    logger.debug { "Monitoring configured" }

    // Configure security (CORS, authentication)
    try {
        configureSecurity()
        logger.debug { "Security configured" }
    } catch (e: Exception) {
        logger.error(e) { "Failed to configure security. Check JWT configuration variables." }
        throw e
    }

    // Configure dependency injection with Koin
    configureKoin()
    logger.debug { "Dependency injection configured" }

    // Configure HTTP client
    configureHttpClient()
    logger.debug { "HTTP client configured" }

    // Configure routing
    configureRouting()
    logger.debug { "Routing configured" }

    // Log application ready
    logger.info { "MAD Gateway initialized and ready to handle requests" }
}
