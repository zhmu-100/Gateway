package com.mad.gateway.routes

import com.mad.gateway.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** Feed routes */
fun Route.feedRoutes() {
    val feedService by inject<FeedServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/feed") {
        // Post routes
        route("/posts") {
            // Get post by ID
            get("/{id}") {
                try {
                    val id =
                            call.parameters["id"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val post = feedService.getPost(id)
                    call.respond(post)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to get post",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error").toString())
                    )
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Post not found"))
                }
            }

            // List posts for feed (from followed users)
            authenticate("auth-jwt") {
                get {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val viewerId = principal?.payload?.subject

                        if (viewerId != null) {
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize =
                                    call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                            val posts = feedService.listPosts(viewerId, page, pageSize)
                            call.respond(posts)
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to list feed posts",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to "Failed to list feed posts")
                        )
                    }
                }
            }

            // Create a new post (authenticated)
            authenticate("auth-jwt") {
                post {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val postRequest = call.receive<Post>()

                            // Ensure the post's user ID matches the authenticated user
                            val post = postRequest.copy(userId = userId)

                            val createdPost = feedService.createPost(post)
                            call.respond(HttpStatusCode.Created, createdPost)

                            loggingService.logInfo(
                                    "Post created",
                                    mapOf("userId" to userId!!, "postId" to createdPost.id!!)
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to create post",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error").toString())
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to create post")
                        )
                    }
                }
            }

            // Comment routes
            route("/{postId}/comments") {
                // List comments for a post
                get {
                    try {
                        val postId =
                                call.parameters["postId"]
                                        ?: return@get call.respond(
                                                HttpStatusCode.BadRequest,
                                                mapOf("error" to "Missing post ID parameter")
                                        )

                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                        val comments = feedService.listComments(postId, page, pageSize)
                        call.respond(comments)
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to list comments",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to "Failed to list comments")
                        )
                    }
                }

                // Create a comment on a post (authenticated)
                authenticate("auth-jwt") {
                    post {
                        try {
                            val postId =
                                    call.parameters["postId"]
                                            ?: return@post call.respond(
                                                    HttpStatusCode.BadRequest,
                                                    mapOf("error" to "Missing post ID parameter")
                                            )

                            val principal = call.principal<JWTPrincipal>()
                            val userId = principal?.payload?.subject

                            if (userId != null) {
                                val commentRequest = call.receive<PostComment>()

                                // Ensure the comment's user ID matches the authenticated user
                                val comment = commentRequest.copy(userId = userId)

                                val createdComment = feedService.createComment(postId, comment)
                                call.respond(HttpStatusCode.Created, createdComment)

                                loggingService.logInfo(
                                        "Comment created",
                                        mapOf(
                                                "userId" to userId!!,
                                                "postId" to postId!!,
                                                "commentId" to createdComment.id!!
                                        )
                                )
                            } else {
                                call.respond(
                                        HttpStatusCode.Unauthorized,
                                        mapOf("error" to "User ID not found in token")
                                )
                            }
                        } catch (e: Exception) {
                            loggingService.logError(
                                    "Failed to create comment",
                                    e,
                                    mapOf("error" to (e.message ?: "Unknown error"))
                            )
                            call.respond(
                                    HttpStatusCode.BadRequest,
                                    mapOf("error" to "Failed to create comment")
                            )
                        }
                    }
                }
            }

            // Reaction routes
            route("/{postId}/reactions") {
                // Add a reaction to a post (authenticated)
                authenticate("auth-jwt") {
                    post {
                        try {
                            val postId =
                                    call.parameters["postId"]
                                            ?: return@post call.respond(
                                                    HttpStatusCode.BadRequest,
                                                    mapOf("error" to "Missing post ID parameter")
                                            )

                            val principal = call.principal<JWTPrincipal>()
                            val userId = principal?.payload?.subject

                            if (userId != null) {
                                val request = call.receive<AddReactionRequest>()

                                // Ensure the reaction's user ID matches the authenticated user
                                val reaction =
                                        feedService.addReaction(postId, userId, request.reaction)
                                call.respond(reaction)

                                loggingService.logInfo(
                                        "Reaction added",
                                        mapOf(
                                                "userId" to userId,
                                                "postId" to postId,
                                                "reaction" to request.reaction
                                        )
                                )
                            } else {
                                call.respond(
                                        HttpStatusCode.Unauthorized,
                                        mapOf("error" to "User ID not found in token")
                                )
                            }
                        } catch (e: Exception) {
                            loggingService.logError(
                                    "Failed to add reaction",
                                    e,
                                    mapOf("error" to (e.message ?: "Unknown error"))
                            )
                            call.respond(
                                    HttpStatusCode.BadRequest,
                                    mapOf("error" to "Failed to add reaction")
                            )
                        }
                    }

                    // Remove a reaction from a post (authenticated)
                    delete {
                        try {
                            val postId =
                                    call.parameters["postId"]
                                            ?: return@delete call.respond(
                                                    HttpStatusCode.BadRequest,
                                                    mapOf("error" to "Missing post ID parameter")
                                            )

                            val principal = call.principal<JWTPrincipal>()
                            val userId = principal?.payload?.subject

                            if (userId != null) {
                                feedService.removeReaction(postId, userId)
                                call.respond(HttpStatusCode.NoContent)

                                loggingService.logInfo(
                                        "Reaction removed",
                                        mapOf("userId" to userId!!, "postId" to postId!!)
                                )
                            } else {
                                call.respond(
                                        HttpStatusCode.Unauthorized,
                                        mapOf("error" to "User ID not found in token")
                                )
                            }
                        } catch (e: Exception) {
                            loggingService.logError(
                                    "Failed to remove reaction",
                                    e,
                                    mapOf("error" to (e.message ?: "Unknown error"))
                            )
                            call.respond(
                                    HttpStatusCode.InternalServerError,
                                    mapOf("error" to "Failed to remove reaction")
                            )
                        }
                    }
                }
            }
        }

        // User posts routes
        route("/users/{userId}/posts") {
            // List posts for a specific user
            get {
                try {
                    val userId =
                            call.parameters["userId"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing user ID parameter")
                                    )

                    val principal = call.principal<JWTPrincipal>()
                    val viewerId = principal?.payload?.subject ?: "anonymous"

                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                    val posts = feedService.listUserPosts(userId, viewerId, page, pageSize)
                    call.respond(posts)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to list user posts",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to list user posts")
                    )
                }
            }
        }
    }
}

// Data class for reaction request
data class AddReactionRequest(val reaction: Reaction)
