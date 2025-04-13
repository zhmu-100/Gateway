package com.mad.gateway.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** Configure security for the application */
fun Application.configureSecurity() {
    // Configure CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        // Allow requests from mobile apps
        anyHost()

        logger.info { "CORS configured to allow requests from any host" }
    }

    // Get JWT configuration from environment variables
    val jwtSecret =
            System.getenv("JWT_SECRET")
                    ?: throw IllegalStateException("JWT_SECRET environment variable is not set")
    val jwtIssuer =
            System.getenv("JWT_ISSUER")
                    ?: throw IllegalStateException("JWT_ISSUER environment variable is not set")
    val jwtAudience =
            System.getenv("JWT_AUDIENCE")
                    ?: throw IllegalStateException("JWT_AUDIENCE environment variable is not set")

    logger.info { "JWT configuration loaded from environment variables" }

    // Configure JWT Authentication
    authentication {
        jwt("auth-jwt") {
            realm = "MAD Gateway"

            verifier(
                    JWT.require(Algorithm.HMAC256(jwtSecret))
                            .withAudience(jwtAudience)
                            .withIssuer(jwtIssuer)
                            .build()
            )

            validate { credential ->
                // Validate JWT token
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Token is not valid or has expired")
                )
            }

            logger.info {
                "JWT authentication configured with issuer: $jwtIssuer, audience: $jwtAudience"
            }
        }
    }
}
