package com.mad.gateway.routes

import com.mad.gateway.services.LoggingServiceClient
import com.mad.gateway.services.ProfileServiceClient
import com.mad.gateway.services.UserProfile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** Profile routes */
fun Route.profileRoutes() {
    val profileService by inject<ProfileServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/profiles") {
        // Get authenticated user's profile
        authenticate("auth-jwt") {
            get("/me") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    if (userId != null) {
                        val profile = profileService.getProfile(userId)
                        call.respond(profile)
                    } else {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                    }
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to get user profile",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to get profile")
                    )
                }
            }
        }

        // Get profile by ID
        get("/{id}") {
            try {
                val id =
                        call.parameters["id"]
                                ?: return@get call.respond(
                                        HttpStatusCode.BadRequest,
                                        mapOf("error" to "Missing ID parameter")
                                )

                val profile = profileService.getProfile(id)
                call.respond(profile)
            } catch (e: Exception) {
                loggingService.logError(
                        "Failed to get profile by ID",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error"))
                )
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Profile not found"))
            }
        }

        // List profiles with pagination
        get {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                val profiles = profileService.listProfiles(page, pageSize)
                call.respond(profiles)
            } catch (e: Exception) {
                loggingService.logError(
                        "Failed to list profiles",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error"))
                )
                call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to list profiles")
                )
            }
        }

        // Create profile
        authenticate("auth-jwt") {
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject
                    val username = principal?.payload?.getClaim("preferred_username")?.asString()
                    val email = principal?.payload?.getClaim("email")?.asString()

                    if (userId != null && username != null && email != null) {
                        val profileRequest = call.receive<UserProfile>()

                        // Ensure the profile ID matches the authenticated user
                        val profile =
                                profileRequest.copy(id = userId, name = username, email = email)

                        val createdProfile = profileService.createProfile(profile)
                        call.respond(HttpStatusCode.Created, createdProfile)

                        loggingService.logInfo(
                                "Profile created for user: $username",
                                mapOf("userId" to userId, "username" to username)
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User information not found in token")
                        )
                    }
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to create profile",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to create profile")
                    )
                }
            }
        }

        // Update profile
        authenticate("auth-jwt") {
            put("/{id}") {
                try {
                    val id =
                            call.parameters["id"]
                                    ?: return@put call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    // Ensure the user can only update their own profile
                    if (userId != id) {
                        call.respond(
                                HttpStatusCode.Forbidden,
                                mapOf("error" to "You can only update your own profile")
                        )
                        return@put
                    }

                    val profileRequest = call.receive<UserProfile>()

                    // Ensure the profile ID matches the authenticated user
                    val profile = profileRequest.copy(id = userId)

                    val updatedProfile = profileService.updateProfile(profile)
                    call.respond(updatedProfile)

                    loggingService.logInfo(
                            "Profile updated for user ID: $userId",
                            mapOf("userId" to userId)
                    )
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to update profile",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to update profile")
                    )
                }
            }
        }

        // Delete profile
        authenticate("auth-jwt") {
            delete("/{id}") {
                try {
                    val id =
                            call.parameters["id"]
                                    ?: return@delete call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    // Ensure the user can only delete their own profile
                    if (userId != id) {
                        call.respond(
                                HttpStatusCode.Forbidden,
                                mapOf("error" to "You can only delete your own profile")
                        )
                        return@delete
                    }

                    profileService.deleteProfile(id)
                    call.respond(HttpStatusCode.NoContent)

                    loggingService.logInfo(
                            "Profile deleted for user ID: $userId",
                            mapOf("userId" to userId)
                    )
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to delete profile",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to delete profile")
                    )
                }
            }
        }

        // Follow routes
        authenticate("auth-jwt") {
            post("/{id}/follow") {
                try {
                    val followeeId =
                            call.parameters["id"]
                                    ?: return@post call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val principal = call.principal<JWTPrincipal>()
                    val followerId = principal?.payload?.subject

                    if (followerId != null) {
                        profileService.follow(followerId, followeeId)
                        call.respond(
                                HttpStatusCode.OK,
                                mapOf("message" to "Successfully followed user")
                        )

                        loggingService.logInfo(
                                "User $followerId followed user $followeeId",
                                mapOf("followerId" to followerId, "followeeId" to followeeId)
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                    }
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to follow user",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to follow user")
                    )
                }
            }

            post("/{id}/unfollow") {
                try {
                    val followeeId =
                            call.parameters["id"]
                                    ?: return@post call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val principal = call.principal<JWTPrincipal>()
                    val followerId = principal?.payload?.subject

                    if (followerId != null) {
                        profileService.unfollow(followerId, followeeId)
                        call.respond(
                                HttpStatusCode.OK,
                                mapOf("message" to "Successfully unfollowed user")
                        )

                        loggingService.logInfo(
                                "User $followerId unfollowed user $followeeId",
                                mapOf("followerId" to followerId, "followeeId" to followeeId)
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                    }
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to unfollow user",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to unfollow user")
                    )
                }
            }

            get("/{id}/followers") {
                try {
                    val userId =
                            call.parameters["id"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                    val followers = profileService.listFollowers(userId, page, pageSize)
                    call.respond(followers)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to list followers",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to list followers")
                    )
                }
            }

            get("/{id}/following") {
                try {
                    val userId =
                            call.parameters["id"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                    val following = profileService.listFollowing(userId, page, pageSize)
                    call.respond(following)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to list following",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to list following")
                    )
                }
            }
        }
    }
}
