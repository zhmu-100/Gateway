package com.mad.gateway.routes

import com.mad.gateway.services.AuthServiceClient
import com.mad.gateway.services.LoggingServiceClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Authentication routes for user management.
 *
 * This function configures routes for user authentication operations including:
 * - Login
 * - Registration
 * - Token refresh
 * - Logout
 * - Token validation
 */
fun Route.authRoutes() {
    val authService by inject<AuthServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/auth") {
        /**
         * Login endpoint.
         *
         * POST /api/auth/login
         *
         * Authenticates a user with username and password, returning JWT tokens on success.
         */
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                val response = authService.login(request.username, request.password)
                call.respond(response)

                loggingService.logInfo(
                        "User logged in: ${request.username}",
                        mapOf("username" to request.username)
                )
            } catch (e: Exception) {
                loggingService.logError(
                        "Login failed",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error"))
                )
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            }
        }

        /**
         * Registration endpoint.
         *
         * POST /api/auth/register
         *
         * Creates a new user account with the provided username, email, and password.
         */
        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                val response =
                        authService.register(request.username, request.email, request.password)
                call.respond(HttpStatusCode.Created, response)

                loggingService.logInfo(
                        "User registered: ${request.username}",
                        mapOf("username" to request.username, "email" to request.email)
                )
            } catch (e: Exception) {
                loggingService.logError(
                        "Registration failed",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error"))
                )
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Registration failed"))
            }
        }

        /**
         * Token refresh endpoint.
         *
         * POST /api/auth/refresh
         *
         * Issues a new access token using a valid refresh token.
         */
        post("/refresh") {
            try {
                val request = call.receive<RefreshTokenRequest>()
                val response = authService.refreshToken(request.refreshToken)
                call.respond(response)

                loggingService.logInfo("Token refreshed")
            } catch (e: Exception) {
                loggingService.logError(
                        "Token refresh failed",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error"))
                )
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid refresh token"))
            }
        }

        /**
         * Logout endpoint.
         *
         * POST /api/auth/logout
         *
         * Invalidates the user's refresh token, requiring re-authentication.
         * Requires a valid JWT token.
         */
        authenticate("auth-jwt") {
            post("/logout") {
                try {
                    val request = call.receive<LogoutRequest>()
                    authService.logout(request.refreshToken)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))

                    val principal = call.principal<JWTPrincipal>()
                    val username = principal?.payload?.getClaim("preferred_username")?.asString()
                    loggingService.logInfo(
                            "User logged out",
                            mapOf("username" to (username ?: "unknown"))
                    )
                } catch (e: Exception) {
                    loggingService.logError(
                            "Logout failed",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Logout failed"))
                }
            }
        }

        /**
         * Token validation endpoint.
         *
         * GET /api/auth/validate
         *
         * Validates the current JWT token and returns user information.
         * Requires a valid JWT token.
         */
        authenticate("auth-jwt") {
            get("/validate") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val username = principal?.payload?.getClaim("preferred_username")?.asString()
                    val userId = principal?.payload?.subject
                    val expiresAt = principal?.expiresAt?.time?.minus(System.currentTimeMillis())

                    call.respond(
                            mapOf(
                                    "userId" to userId,
                                    "username" to username,
                                    "expiresIn" to expiresAt
                            )
                    )
                } catch (e: Exception) {
                    loggingService.logError(
                            "Token validation failed",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                }
            }
        }
    }
}

// Data classes for requests

/**
 * Request data for user login.
 *
 * @property username The user's username
 * @property password The user's password
 */
data class LoginRequest(val username: String, val password: String)

/**
 * Request data for user registration.
 *
 * @property username The desired username
 * @property email The user's email address
 * @property password The desired password
 */
data class RegisterRequest(val username: String, val email: String, val password: String)

/**
 * Request data for token refresh.
 *
 * @property refreshToken The refresh token to use for obtaining a new access token
 */
data class RefreshTokenRequest(val refreshToken: String)

/**
 * Request data for logout.
 *
 * @property refreshToken The refresh token to invalidate
 */
data class LogoutRequest(val refreshToken: String)
