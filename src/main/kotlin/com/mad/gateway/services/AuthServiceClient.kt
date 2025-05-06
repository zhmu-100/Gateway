package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Auth service (Keycloak) */
class AuthServiceClient(client: HttpClient, baseUrl: String) :
        ServiceClient(client, baseUrl), KoinComponent {
        private val application: Application by inject()

        // Create a coroutine scope for background tasks
        private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Admin credentials - in production these should be loaded securely
        private val adminUsername = "admin"
        private val adminPassword = "admin"

        private val adminToken = AtomicReference<String?>(null)
        private var adminTokenExpiry = 0L
        private var realmInitialized = false

        // Token expiration tracking
        private val tokenExpirationTimes = ConcurrentHashMap<String, Long>()
        private val refreshTokens = ConcurrentHashMap<String, String>()

        // Buffer time to refresh tokens before they expire (5 minutes)
        private val tokenRefreshBuffer = 300_000L

        init {
                // Check realm asynchronously to avoid blocking startup
                coroutineScope.launch {
                        try {
                                checkRealmExists()
                        } catch (e: Exception) {
                                logger.error(e) { "Failed to check realm: ${e.message}" }
                        }
                }

                // Start token refresh background job
                coroutineScope.launch { startTokenRefreshJob() }
        }

        /** Get admin access token */
        private suspend fun getAdminToken(): String {
                val currentToken = adminToken.get()
                val currentTime = System.currentTimeMillis()

                // Return current token if it's not expired (with 30 seconds buffer)
                if (currentToken != null && currentTime < adminTokenExpiry - 30000) {
                        return currentToken
                }

                logger.info { "Getting admin token from Keycloak" }
                val response =
                        client.post("$baseUrl/realms/master/protocol/openid-connect/token") {
                                contentType(ContentType.Application.FormUrlEncoded)
                                setBody(
                                        "client_id=admin-cli" +
                                                "&grant_type=password" +
                                                "&username=$adminUsername" +
                                                "&password=$adminPassword"
                                )
                        }

                if (response.status.isSuccess()) {
                        val tokenResponse = response.body<Map<String, Any>>()
                        val newToken =
                                tokenResponse["access_token"]?.toString()
                                        ?: throw RuntimeException(
                                                "Admin token not found in response"
                                        )

                        // Store token expiration time
                        val expiresIn = tokenResponse["expires_in"]?.toString()?.toIntOrNull() ?: 60
                        adminTokenExpiry = currentTime + (expiresIn * 1000L)
                        adminToken.set(newToken)

                        return newToken
                } else {
                        val errorBody = response.bodyAsText()
                        throw RuntimeException(
                                "Failed to get admin token: ${response.status} - $errorBody"
                        )
                }
        }

        /** Check if the 'mad' realm exists */
        private suspend fun checkRealmExists() {
                if (realmInitialized) return

                try {
                        val token = getAdminToken()

                        // Check if realm exists
                        val checkResponse =
                                client.get("$baseUrl/admin/realms/mad") {
                                        headers {
                                                append(HttpHeaders.Authorization, "Bearer $token")
                                        }
                                }

                        if (checkResponse.status.isSuccess()) {
                                logger.info { "Realm 'mad' exists" }
                                realmInitialized = true
                                return
                        } else if (checkResponse.status == HttpStatusCode.NotFound) {
                                logger.warn {
                                        "Realm 'mad' does not exist. Please create it manually in Keycloak."
                                }
                        } else {
                                // If error is not 404 (not found), something else is wrong
                                throw RuntimeException(
                                        "Unexpected status checking realm: ${checkResponse.status}"
                                )
                        }
                } catch (e: Exception) {
                        logger.error(e) { "Failed to check realm: ${e.message}" }
                        throw e
                }
        }

        /** Track token expiration */
        private fun trackToken(userId: String, tokenResponse: TokenResponse) {
                val currentTime = System.currentTimeMillis()

                // Store expiration time with buffer to refresh before actual expiration
                tokenResponse.accessToken?.let { token ->
                        val expiresAt = currentTime + (tokenResponse.expiresIn * 1000L)
                        tokenExpirationTimes[userId] = expiresAt

                        // Store refresh token for future use
                        tokenResponse.refreshToken?.let { refreshToken ->
                                refreshTokens[userId] = refreshToken
                        }

                        logger.debug { "Token for user $userId expires at $expiresAt" }
                }
        }

        /** Start background job to refresh tokens */
        private suspend fun startTokenRefreshJob() {
                while (true) {
                        try {
                                refreshExpiredTokens()
                                delay(60000) // Check every minute
                        } catch (e: Exception) {
                                logger.error(e) { "Error in token refresh job: ${e.message}" }
                                delay(120000) // Wait longer after error
                        }
                }
        }

        /** Check and refresh expired tokens */
        private suspend fun refreshExpiredTokens() {
                val currentTime = System.currentTimeMillis()

                tokenExpirationTimes.forEach { (userId, expiryTime) ->
                        // Refresh if token expires soon (within buffer time)
                        if (expiryTime - currentTime < tokenRefreshBuffer) {
                                val refreshToken = refreshTokens[userId] ?: return@forEach
                                try {
                                        val newTokens = refreshToken(refreshToken)
                                        // Update tracking with new tokens
                                        trackToken(userId, newTokens)
                                        logger.info {
                                                "Successfully refreshed token for user $userId"
                                        }
                                } catch (e: Exception) {
                                        logger.error(e) {
                                                "Failed to refresh token for user $userId: ${e.message}"
                                        }
                                        // Remove expired tokens
                                        tokenExpirationTimes.remove(userId)
                                        refreshTokens.remove(userId)
                                }
                        }
                }
        }

        /** Extract user ID from token */
        private fun extractUserId(token: String): String {
                // Simple extraction from JWT token without validation
                // In production, use a proper JWT parser
                try {
                        val parts = token.split(".")
                        if (parts.size > 1) {
                                val payload = java.util.Base64.getUrlDecoder().decode(parts[1])
                                val payloadText = String(payload)
                                val subMatch = "\"sub\":\"([^\"]+)\"".toRegex().find(payloadText)
                                return subMatch?.groupValues?.get(1) ?: "unknown"
                        }
                } catch (e: Exception) {
                        logger.warn { "Could not extract user ID from token: ${e.message}" }
                }
                return "unknown"
        }

        /** Login with username and password */
        suspend fun login(username: String, password: String): TokenResponse {
                logger.info { "Authenticating user: $username" }

                // Ensure realm is initialized
                if (!realmInitialized) {
                        checkRealmExists()
                }

                try {
                        val response =
                                client.post("$baseUrl/realms/mad/protocol/openid-connect/token") {
                                        contentType(ContentType.Application.FormUrlEncoded)
                                        setBody(
                                                "client_id=mad-mobile-app" +
                                                        "&grant_type=password" +
                                                        "&username=$username" +
                                                        "&password=$password"
                                        )
                                }

                        if (response.status.isSuccess()) {
                                val tokenResponse: TokenResponse = response.body()

                                // Track token expiration
                                tokenResponse.accessToken?.let { token ->
                                        val userId = extractUserId(token)
                                        trackToken(userId, tokenResponse)
                                }

                                return tokenResponse
                        } else {
                                val errorBody = response.bodyAsText()
                                logger.error { "Login failed: ${response.status} - $errorBody" }
                                throw RuntimeException(
                                        "Login failed: ${response.status} - $errorBody"
                                )
                        }
                } catch (e: Exception) {
                        logger.error(e) { "Login error: ${e.message}" }
                        throw e
                }
        }

        /** Refresh an access token using a refresh token */
        suspend fun refreshToken(refreshToken: String): TokenResponse {
                logger.info { "Refreshing token" }
                try {
                        val response =
                                client.post("$baseUrl/realms/mad/protocol/openid-connect/token") {
                                        contentType(ContentType.Application.FormUrlEncoded)
                                        setBody(
                                                "client_id=mad-mobile-app" +
                                                        "&grant_type=refresh_token" +
                                                        "&refresh_token=$refreshToken"
                                        )
                                }

                        if (response.status.isSuccess()) {
                                val tokenResponse: TokenResponse = response.body()

                                // Update token tracking
                                tokenResponse.accessToken?.let { token ->
                                        val userId = extractUserId(token)
                                        trackToken(userId, tokenResponse)
                                }

                                return tokenResponse
                        } else {
                                val errorBody = response.bodyAsText()
                                logger.error {
                                        "Token refresh failed: ${response.status} - $errorBody"
                                }
                                throw RuntimeException("Token refresh failed: ${response.status}")
                        }
                } catch (e: Exception) {
                        logger.error(e) { "Token refresh error: ${e.message}" }
                        throw e
                }
        }

        /** Validate a token */
        suspend fun validateToken(token: String): TokenInfo {
                logger.info { "Validating token" }
                try {
                        val response =
                                client.post(
                                        "$baseUrl/realms/mad/protocol/openid-connect/token/introspect"
                                ) {
                                        contentType(ContentType.Application.FormUrlEncoded)
                                        setBody("client_id=mad-mobile-app" + "&token=$token")
                                }

                        if (response.status.isSuccess()) {
                                val tokenInfo: TokenInfo = response.body()
                                return tokenInfo
                        } else {
                                val errorBody = response.bodyAsText()
                                logger.error {
                                        "Token validation failed: ${response.status} - $errorBody"
                                }
                                throw RuntimeException(
                                        "Token validation failed: ${response.status}"
                                )
                        }
                } catch (e: Exception) {
                        logger.error(e) { "Token validation error: ${e.message}" }
                        throw e
                }
        }

        /** Logout a user */
        suspend fun logout(refreshToken: String) {
                logger.info { "Logging out user" }
                try {
                        val response =
                                client.post("$baseUrl/realms/mad/protocol/openid-connect/logout") {
                                        contentType(ContentType.Application.FormUrlEncoded)
                                        setBody(
                                                "client_id=mad-mobile-app" +
                                                        "&refresh_token=$refreshToken"
                                        )
                                }

                        if (!response.status.isSuccess()) {
                                val errorBody = response.bodyAsText()
                                logger.error { "Logout failed: ${response.status} - $errorBody" }
                                throw RuntimeException("Logout failed: ${response.status}")
                        }
                } catch (e: Exception) {
                        logger.error(e) { "Logout error: ${e.message}" }
                        throw e
                }
        }

        /** Register a new user */
        suspend fun register(
                username: String,
                email: String,
                password: String
        ): RegistrationResponse {
                logger.info { "Registering new user: $username" }

                // Ensure realm is initialized
                if (!realmInitialized) {
                        checkRealmExists()
                }

                try {
                        // Get admin token first
                        val token = getAdminToken()

                        val response =
                                client.post("$baseUrl/admin/realms/mad/users") {
                                        contentType(ContentType.Application.Json)
                                        headers {
                                                append(HttpHeaders.Authorization, "Bearer $token")
                                        }
                                        setBody(
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
                                        )
                                }

                        if (response.status == HttpStatusCode.Created || response.status.isSuccess()
                        ) {
                                // Keycloak doesn't return the user in the response body,
                                // so we need to fetch the user after creation
                                val location = response.headers[HttpHeaders.Location]
                                val userId =
                                        location?.substringAfterLast('/')
                                                ?: throw RuntimeException(
                                                        "User ID not found in response"
                                                )

                                // Get user details
                                val userResponse =
                                        client.get("$baseUrl/admin/realms/mad/users/$userId") {
                                                headers {
                                                        append(
                                                                HttpHeaders.Authorization,
                                                                "Bearer $token"
                                                        )
                                                }
                                        }

                                if (userResponse.status.isSuccess()) {
                                        val userInfo: Map<String, Any> = userResponse.body()
                                        return RegistrationResponse(
                                                id = userInfo["id"]?.toString() ?: "",
                                                createdTimestamp =
                                                        userInfo["createdTimestamp"]
                                                                ?.toString()
                                                                ?.toLongOrNull()
                                                                ?: 0L,
                                                username = userInfo["username"]?.toString() ?: "",
                                                enabled =
                                                        userInfo["enabled"]?.toString()?.toBoolean()
                                                                ?: false,
                                                emailVerified =
                                                        userInfo["emailVerified"]
                                                                ?.toString()
                                                                ?.toBoolean()
                                                                ?: false,
                                                email = userInfo["email"]?.toString() ?: ""
                                        )
                                } else {
                                        val errorBody = userResponse.bodyAsText()
                                        logger.error {
                                                "Failed to get user after registration: ${userResponse.status} - $errorBody"
                                        }
                                        throw RuntimeException(
                                                "Registration completed but failed to get user details"
                                        )
                                }
                        } else {
                                val errorBody = response.bodyAsText()
                                logger.error {
                                        "Registration failed: ${response.status} - $errorBody"
                                }
                                throw RuntimeException("Registration failed: ${response.status}")
                        }
                } catch (e: Exception) {
                        logger.error(e) { "Registration error: ${e.message}" }
                        throw e
                }
        }
}

// Data classes for requests and responses

@Serializable
data class LoginRequest(
        val grantType: String,
        val clientId: String,
        val username: String,
        val password: String
)

@Serializable
data class RefreshTokenRequest(
        val grantType: String,
        val clientId: String,
        val refreshToken: String
)

@Serializable data class ValidateTokenRequest(val token: String, val clientId: String)

@Serializable data class LogoutRequest(val refreshToken: String, val clientId: String)

@Serializable data class Credential(val type: String, val value: String, val temporary: Boolean)

@Serializable
data class RegistrationRequest(
        val username: String,
        val email: String,
        val enabled: Boolean,
        val credentials: List<Credential>
)

@Serializable
data class TokenResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("expires_in") val expiresIn: Int = 0,
        @SerialName("refresh_expires_in") val refreshExpiresIn: Int = 0,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("token_type") val tokenType: String? = null,
        @SerialName("not-before-policy") val notBeforePolicy: Int = 0,
        @SerialName("session_state") val sessionState: String? = null,
        @SerialName("scope") val scope: String? = null
)

@Serializable
data class TokenInfo(
        val active: Boolean = false,
        val exp: Long = 0,
        val iat: Long = 0,
        val jti: String = "",
        val iss: String = "",
        val sub: String = "",
        val typ: String = "",
        val azp: String = "",
        val sessionState: String = "",
        val acr: String = "",
        val scope: String = "",
        val sid: String = "",
        val email: String? = null,
        val name: String? = null,
        val preferredUsername: String? = null
)

@Serializable
data class RegistrationResponse(
        val id: String = "",
        val createdTimestamp: Long = 0,
        val username: String = "",
        val enabled: Boolean = false,
        val emailVerified: Boolean = false,
        val email: String = ""
)
