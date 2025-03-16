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
    // Access environment explicitly through this reference
    val appEnvironment = environment

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

    // Configure JWT Authentication
    authentication {
        jwt("auth-jwt") {
            // Get JWT configuration from application.conf
            val jwtSecret = appEnvironment.config.property("jwt.secret").getString()
            val jwtIssuer = appEnvironment.config.property("jwt.issuer").getString()
            val jwtAudience = appEnvironment.config.property("jwt.audience").getString()

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
