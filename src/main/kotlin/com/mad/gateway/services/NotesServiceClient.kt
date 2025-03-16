package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Notes service */
class NotesServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String =
            application.environment.config.property("services.notes.url").getString()

    // Note operations

    /** Create a new note */
    suspend fun createNote(note: Note): Note {
        logger.info { "Creating note for user: ${note.userId}" }
        val request = CreateNoteRequest(note)
        return post("/notes", request)
    }

    /** Get a note by ID */
    suspend fun getNote(id: String): Note {
        logger.info { "Getting note with ID: $id" }
        return get("/notes/$id")
    }

    /** List notes for a user */
    suspend fun listNotes(userId: String, page: Int = 1, pageSize: Int = 20): ListNotesResponse {
        logger.info { "Listing notes for user ID: $userId" }
        return get("/notes?userId=$userId&page=$page&pageSize=$pageSize")
    }

    /** Update a note */
    suspend fun updateNote(note: Note): Note {
        logger.info { "Updating note with ID: ${note.id}" }
        val request = UpdateNoteRequest(note)
        return put("/notes/${note.id}", request)
    }

    /** Delete a note */
    suspend fun deleteNote(id: String, userId: String) {
        logger.info { "Deleting note with ID: $id for user: $userId" }
        delete<Unit>("/notes/$id?userId=$userId")
    }

    // Notification operations

    /** Create a new notification */
    suspend fun createNotification(notification: Notification): Notification {
        logger.info { "Creating notification for user: ${notification.userId}" }
        val request = CreateNotificationRequest(notification)
        return post("/notifications", request)
    }

    /** Get a notification by ID */
    suspend fun getNotification(id: String): Notification {
        logger.info { "Getting notification with ID: $id" }
        return get("/notifications/$id")
    }

    /** List notifications for a user */
    suspend fun listNotifications(
            userId: String,
            page: Int = 1,
            pageSize: Int = 20
    ): ListNotificationsResponse {
        logger.info { "Listing notifications for user ID: $userId" }
        return get("/notifications?userId=$userId&page=$page&pageSize=$pageSize")
    }

    /** Perform an action on a notification */
    suspend fun performNotificationAction(
            id: String,
            userId: String,
            action: NotificationAction,
            snoozeDuration: NotificationSnooze? = null
    ): Notification {
        logger.info { "Performing action $action on notification with ID: $id" }
        val request = NotificationActionRequest(id, userId, action, snoozeDuration)
        return post("/notifications/$id/actions", request)
    }
}

// Data classes based on the proto definitions

data class Note(
        val id: String? = null,
        val userId: String,
        val title: String,
        val content: String,
        val date: String? = null // ISO-8601 timestamp
)

data class CreateNoteRequest(val note: Note)

data class UpdateNoteRequest(val note: Note)

data class ListNotesResponse(
        val notes: List<Note>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

enum class NotificationAction {
    UNSPECIFIED,
    COMPLETE,
    DISMISS,
    SNOOZE
}

enum class NotificationSnooze {
    UNSPECIFIED,
    FIVE_MINUTES,
    FIFTEEN_MINUTES,
    THIRTY_MINUTES,
    ONE_HOUR,
    FIVE_HOURS,
    ONE_DAY
}

data class Notification(
        val id: String? = null,
        val userId: String,
        val title: String,
        val description: String,
        val createDate: String? = null, // ISO-8601 timestamp
        val notificationDate: String // ISO-8601 timestamp
)

data class CreateNotificationRequest(val notification: Notification)

data class NotificationActionRequest(
        val id: String,
        val userId: String,
        val action: NotificationAction,
        val snoozeDuration: NotificationSnooze? = null
)

data class ListNotificationsResponse(
        val notifications: List<Notification>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)
