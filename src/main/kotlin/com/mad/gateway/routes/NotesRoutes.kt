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

/** Notes routes */
fun Route.notesRoutes() {
    val notesService by inject<NotesServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/notebook") {
        // Notes routes
        route("/notes") {
            // Get note by ID
            authenticate("auth-jwt") {
                get("/{id}") {
                    try {
                        val id =
                                call.parameters["id"]
                                        ?: return@get call.respond(
                                                HttpStatusCode.BadRequest,
                                                mapOf("error" to "Missing ID parameter")
                                        )

                        val note = notesService.getNote(id)

                        // Check if the note belongs to the authenticated user
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null && note.userId == userId) {
                            call.respond(note)
                        } else {
                            call.respond(
                                    HttpStatusCode.Forbidden,
                                    mapOf("error" to "Access denied")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to get note",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error").toString())
                        )
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Note not found"))
                    }
                }

                // List notes for authenticated user
                get {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize =
                                    call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                            val notes = notesService.listNotes(userId, page, pageSize)
                            call.respond(notes)
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to list notes",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to "Failed to list notes")
                        )
                    }
                }

                // Create a new note
                post {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val noteRequest = call.receive<Note>()

                            // Ensure the note's user ID matches the authenticated user
                            val note = noteRequest.copy(userId = userId)

                            val createdNote = notesService.createNote(note)
                            call.respond(HttpStatusCode.Created, createdNote)

                            loggingService.logInfo(
                                    "Note created: ${createdNote.title}",
                                    mapOf("userId" to userId, "noteId" to createdNote.id!!)
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to create note",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to create note")
                        )
                    }
                }

                // Update a note
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

                        if (userId != null) {
                            val noteRequest = call.receive<Note>()

                            // Ensure the note ID and user ID match the authenticated user
                            val note = noteRequest.copy(id = id, userId = userId)

                            val updatedNote = notesService.updateNote(note)
                            call.respond(updatedNote)

                            loggingService.logInfo(
                                    "Note updated: ${updatedNote.title}",
                                    mapOf("userId" to userId, "noteId" to id)
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to update note",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to update note")
                        )
                    }
                }

                // Delete a note
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

                        if (userId != null) {
                            notesService.deleteNote(id, userId)
                            call.respond(HttpStatusCode.NoContent)

                            loggingService.logInfo(
                                    "Note deleted",
                                    mapOf("userId" to userId, "noteId" to id)
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to delete note",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to "Failed to delete note")
                        )
                    }
                }
            }
        }

        // Notifications routes
        route("/notifications") {
            authenticate("auth-jwt") {
                // Get notification by ID
                get("/{id}") {
                    try {
                        val id =
                                call.parameters["id"]
                                        ?: return@get call.respond(
                                                HttpStatusCode.BadRequest,
                                                mapOf("error" to "Missing ID parameter")
                                        )

                        val notification = notesService.getNotification(id)

                        // Check if the notification belongs to the authenticated user
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null && notification.userId == userId) {
                            call.respond(notification)
                        } else {
                            call.respond(
                                    HttpStatusCode.Forbidden,
                                    mapOf("error" to "Access denied")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to get notification",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "Notification not found")
                        )
                    }
                }

                // List notifications for authenticated user
                get {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                            val pageSize =
                                    call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                            val notifications =
                                    notesService.listNotifications(userId, page, pageSize)
                            call.respond(notifications)
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to list notifications",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to "Failed to list notifications")
                        )
                    }
                }

                // Create a new notification
                post {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val notificationRequest = call.receive<Notification>()

                            // Ensure the notification's user ID matches the authenticated user
                            val notification = notificationRequest.copy(userId = userId)

                            val createdNotification = notesService.createNotification(notification)
                            call.respond(HttpStatusCode.Created, createdNotification)

                            loggingService.logInfo(
                                    "Notification created: ${createdNotification.title}",
                                    mapOf(
                                            "userId" to userId,
                                            "notificationId" to createdNotification.id!!
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
                                "Failed to create notification",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to create notification")
                        )
                    }
                }

                // Perform action on a notification
                post("/{id}/actions") {
                    try {
                        val id =
                                call.parameters["id"]
                                        ?: return@post call.respond(
                                                HttpStatusCode.BadRequest,
                                                mapOf("error" to "Missing ID parameter")
                                        )

                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val actionRequest = call.receive<NotificationActionRequest>()

                            val updatedNotification =
                                    notesService.performNotificationAction(
                                            id,
                                            userId,
                                            actionRequest.action,
                                            actionRequest.snoozeDuration
                                    )

                            call.respond(updatedNotification)

                            loggingService.logInfo(
                                    "Notification action performed: ${actionRequest.action}",
                                    mapOf(
                                            "userId" to userId,
                                            "notificationId" to id,
                                            "action" to actionRequest.action
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
                                "Failed to perform notification action",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to perform notification action")
                        )
                    }
                }
            }
        }
    }
}

// Data class for notification action request
data class NotificationActionRequest(
        val action: NotificationAction,
        val snoozeDuration: NotificationSnooze? = null
)
