package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Auth service (Keycloak) */
class AuthServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
        private val application: Application by inject()
        override val baseUrl: String =
                application.environment.config.property("services.auth.url").getString()

        /** Login with username and password */
        suspend fun login(username: String, password: String): TokenResponse {
                logger.info { "Authenticating user: $username" }
                val request =
                        LoginRequest(
                                grantType = "password",
                                clientId = "mad-mobile-app",
                                username = username,
                                password = password
                        )
                return post("/realms/mad/protocol/openid-connect/token", request)
        }

        /** Refresh an access token using a refresh token */
        suspend fun refreshToken(refreshToken: String): TokenResponse {
                logger.info { "Refreshing token" }
                val request =
                        RefreshTokenRequest(
                                grantType = "refresh_token",
                                clientId = "mad-mobile-app",
                                refreshToken = refreshToken
                        )
                return post("/realms/mad/protocol/openid-connect/token", request)
        }

        /** Validate a token */
        suspend fun validateToken(token: String): TokenInfo {
                logger.info { "Validating token" }
                val request = ValidateTokenRequest(token = token, clientId = "mad-mobile-app")
                return post("/realms/mad/protocol/openid-connect/token/introspect", request)
        }

        /** Logout a user */
        suspend fun logout(refreshToken: String) {
                logger.info { "Logging out user" }
                val request =
                        LogoutRequest(refreshToken = refreshToken, clientId = "mad-mobile-app")
                post<Unit>("/realms/mad/protocol/openid-connect/logout", request)
        }

        /** Register a new user */
        suspend fun register(
                username: String,
                email: String,
                password: String
        ): RegistrationResponse {
                logger.info { "Registering new user: $username" }
                val request =
                        RegistrationRequest(
                                username = username,
                                email = email,
                                enabled = true,
                                credentials =
                                        listOf(
                                                Credential(
                                                        type = "password",
                                                        value = password,
                                                        temporary = false
                                                )
                                        )
                        )
                return post("/admin/realms/mad/users", request)
        }
}

// Data classes for requests and responses

data class LoginRequest(
        val grantType: String,
        val clientId: String,
        val username: String,
        val password: String
)

data class RefreshTokenRequest(
        val grantType: String,
        val clientId: String,
        val refreshToken: String
)

data class ValidateTokenRequest(val token: String, val clientId: String)

data class LogoutRequest(val refreshToken: String, val clientId: String)

data class Credential(val type: String, val value: String, val temporary: Boolean)

data class RegistrationRequest(
        val username: String,
        val email: String,
        val enabled: Boolean,
        val credentials: List<Credential>
)

data class TokenResponse(
        val accessToken: String,
        val expiresIn: Int,
        val refreshExpiresIn: Int,
        val refreshToken: String,
        val tokenType: String,
        val notBeforePolicy: Int,
        val sessionState: String,
        val scope: String
)

data class TokenInfo(
        val active: Boolean,
        val exp: Long,
        val iat: Long,
        val jti: String,
        val iss: String,
        val sub: String,
        val typ: String,
        val azp: String,
        val sessionState: String,
        val acr: String,
        val scope: String,
        val sid: String,
        val email: String?,
        val name: String?,
        val preferredUsername: String?
)

data class RegistrationResponse(
        val id: String,
        val createdTimestamp: Long,
        val username: String,
        val enabled: Boolean,
        val emailVerified: Boolean,
        val email: String
)
