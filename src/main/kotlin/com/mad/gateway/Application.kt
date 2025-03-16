package com.mad.gateway

import com.mad.gateway.config.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Main entry point for the MAD Gateway application.
 *
 * This function initializes and starts the Ktor server with the Netty engine.
 * It loads configuration from application.conf and starts the server on the
 * configured port (or 8080 by default).
 */
fun main() {
    // Load configuration from application.conf
    val config = ApplicationConfig("application.conf")
    val port = config.propertyOrNull("ktor.deployment.port")?.getString()?.toInt() ?: 8080

    logger.info { "Starting MAD Gateway on port $port" }

    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
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
    configureSecurity()
    logger.debug { "Security configured" }

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
