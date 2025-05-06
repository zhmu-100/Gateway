package com.mad.gateway.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    // Get Keycloak configuration from environment variables
    val keycloakUrl = System.getenv("KEYCLOAK_URL") ?: "http://keycloak:8081"
    val keycloakRealm = System.getenv("KEYCLOAK_REALM") ?: "mad"

    // Build the issuer URL from Keycloak config
    val jwtIssuer = "$keycloakUrl/auth/realms/$keycloakRealm"

    logger.info { "Keycloak JWT configuration: issuer=$jwtIssuer" }

    // Configure JWT Authentication for Keycloak
    authentication {
        jwt("auth-jwt") {
            realm = "MAD Gateway"

            verifier {
                // Get RSA public key from Keycloak
                val keyProvider = KeycloakRSAKeyProvider(keycloakUrl, keycloakRealm)

                JWT.require(Algorithm.RSA256(keyProvider)).withIssuer(jwtIssuer).build()
            }

            validate { credential ->
                // Keycloak tokens have multiple possible audience values
                // We'll accept the token if it's valid, regardless of audience
                JWTPrincipal(credential.payload)
            }

            challenge { _, _ ->
                call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Token is not valid or has expired")
                )
            }

            logger.info { "JWT authentication configured with Keycloak issuer: $jwtIssuer" }
        }
    }
}

/** Provides RSA public key from Keycloak for JWT verification */
class KeycloakRSAKeyProvider(private val keycloakUrl: String, private val realm: String) :
        RSAKeyProvider {
    private val publicKey: RSAPublicKey by lazy { fetchPublicKey() }

    override fun getPublicKeyById(keyId: String): RSAPublicKey = publicKey

    override fun getPrivateKey(): Nothing? = null

    override fun getPrivateKeyId(): String? = null

    private fun fetchPublicKey(): RSAPublicKey {
        val client =
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(
                                Json {
                                    ignoreUnknownKeys = true
                                    isLenient = true
                                }
                        )
                    }
                }

        return runBlocking {
            try {
                val response: JsonObject = client.get("$keycloakUrl/auth/realms/$realm").body()
                val pemKey =
                        response["public_key"]?.jsonPrimitive?.content
                                ?: throw IllegalStateException(
                                        "Public key not found in Keycloak response"
                                )

                // Convert PEM to RSAPublicKey
                val keyBytes = Base64.getDecoder().decode(pemKey)
                val spec = X509EncodedKeySpec(keyBytes)
                KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
            } catch (e: Exception) {
                logger.error(e) { "Failed to fetch Keycloak public key: ${e.message}" }
                throw RuntimeException("Could not fetch Keycloak public key", e)
            } finally {
                client.close()
            }
        }
    }
}
