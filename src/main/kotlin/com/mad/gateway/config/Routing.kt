package com.mad.gateway.config

import com.mad.gateway.routes.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures the routing for the application.
 *
 * This function sets up all API routes for the gateway, organizing them into logical groups.
 * It includes:
 * - A health check endpoint for monitoring
 * - API routes grouped by service domain
 *
 * All API endpoints are prefixed with "/api" except for the health check.
 */
fun Application.configureRouting() {
    routing {
        // Health check endpoint
        get("/health") { call.respond(mapOf("status" to "UP")) }

        // API routes grouped by service
        route("/api") {
            // Auth routes
            authRoutes()

            // Profile routes
            profileRoutes()

            // Training routes
            trainingRoutes()

            // Diet routes
            dietRoutes()

            // Feed routes
            feedRoutes()

            // Notes routes
            notesRoutes()

            // Statistics routes
            statisticsRoutes()

            // File routes
            fileRoutes()
        }
    }
}
