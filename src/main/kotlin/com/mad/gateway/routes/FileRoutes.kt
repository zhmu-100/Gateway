package com.mad.gateway.routes

import com.mad.gateway.services.FileServiceClient
import com.mad.gateway.services.LoggingServiceClient
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.*
import java.nio.ByteBuffer
import org.koin.ktor.ext.inject

/** File routes */
fun Route.fileRoutes() {
    val fileService by inject<FileServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/files") {
        // Get file by ID
        get("/{id}") {
            try {
                val id =
                        call.parameters["id"]
                                ?: return@get call.respond(
                                        HttpStatusCode.BadRequest,
                                        mapOf("error" to "Missing ID parameter")
                                )

                val fileBytes = fileService.getFile(id)

                // Set appropriate content type based on file extension or default to octet-stream
                val contentType =
                        ContentType.defaultForFileExtension(id.substringAfterLast('.', "bin"))

                call.respondBytes(fileBytes, contentType)
            } catch (e: Exception) {
                loggingService.logError("Failed to get file", e, mapOf("error" to e.message!!))
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
            }
        }

        // Get file as stream
        get("/{id}/stream") {
            try {
                val id =
                        call.parameters["id"]
                                ?: return@get call.respond(
                                        HttpStatusCode.BadRequest,
                                        mapOf("error" to "Missing ID parameter")
                                )

                val fileFlow = fileService.getFileAsFlow(id)

                // Set appropriate content type based on file extension or default to octet-stream
                val contentType =
                        ContentType.defaultForFileExtension(id.substringAfterLast('.', "bin"))

                call.respondBytesWriter(contentType) {
                    fileFlow.collect { chunk ->
                        writeFully(ByteBuffer.wrap(chunk)) // Use ByteBuffer.wrap
                        flush()
                    }
                }
            } catch (e: Exception) {
                loggingService.logError("Failed to stream file", e, mapOf("error" to e.message!!))
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
            }
        }

        // Get file URL
        get("/{id}/url") {
            try {
                val id =
                        call.parameters["id"]
                                ?: return@get call.respond(
                                        HttpStatusCode.BadRequest,
                                        mapOf("error" to "Missing ID parameter")
                                )

                val urlResponse = fileService.getFileUrl(id)
                call.respond(urlResponse)
            } catch (e: Exception) {
                loggingService.logError(
                        "Failed to get file URL",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error"))
                )
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "File URL not found"))
            }
        }

        // Upload file (authenticated)
        authenticate("auth-jwt") {
            post("/upload") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    if (userId == null) {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                        return@post
                    }

                    val multipart = call.receiveMultipart()
                    var fileBytes: ByteArray? = null
                    var fileName = ""
                    var mimeType = ""
                    var isPrivate = false
                    var isTemporary = false
                    var ecosystemId: String? = null
                    var folder: String? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                fileName = part.originalFileName ?: "unnamed-file"
                                mimeType =
                                        part.contentType?.toString() ?: "application/octet-stream"
                                fileBytes = part.streamProvider().readBytes()
                            }
                            is PartData.FormItem -> {
                                when (part.name) {
                                    "private" -> isPrivate = part.value.toBoolean()
                                    "temporary" -> isTemporary = part.value.toBoolean()
                                    "ecosystemId" -> ecosystemId = part.value
                                    "folder" -> folder = part.value
                                }
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    if (fileBytes == null) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "No file content provided")
                        )
                        return@post
                    }

                    val response =
                            fileService.uploadFile(
                                    fileBytes = fileBytes!!,
                                    fileName = fileName,
                                    mimeType = mimeType,
                                    userId = userId,
                                    isPrivate = isPrivate,
                                    isTemporary = isTemporary,
                                    ecosystemId = ecosystemId,
                                    folder = folder
                            )

                    call.respond(HttpStatusCode.Created, response)

                    loggingService.logInfo(
                            "File uploaded: $fileName",
                            mapOf(
                                    "userId" to userId,
                                    "fileId" to response.id,
                                    "fileName" to fileName,
                                    "mimeType" to mimeType,
                                    "size" to fileBytes!!.size
                            )
                    )
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to upload file",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to upload file")
                    )
                }
            }

            post("/fix-upload/{id}") {
                try {
                    val id =
                            call.parameters["id"]
                                    ?: return@post call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    if (userId == null) {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                        return@post
                    }

                    val multipart = call.receiveMultipart()
                    var fileBytes: ByteArray? = null
                    var fileName = ""
                    var mimeType = ""

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                fileName = part.originalFileName ?: "unnamed-file"
                                mimeType =
                                        part.contentType?.toString() ?: "application/octet-stream"
                                fileBytes = part.streamProvider().readBytes()
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    if (fileBytes == null) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "No file content provided")
                        )
                        return@post
                    }

                    val response =
                            fileService.fixUploadFile(
                                    fileId = id,
                                    fileBytes = fileBytes!!,
                                    fileName = fileName,
                                    mimeType = mimeType
                            )

                    call.respond(HttpStatusCode.OK, response)

                    loggingService.logInfo(
                            "File replaced: $fileName",
                            mapOf(
                                    "userId" to userId,
                                    "fileId" to response.id,
                                    "fileName" to fileName,
                                    "mimeType" to mimeType,
                                    "size" to fileBytes!!.size
                            )
                    )
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to replace file",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to replace file")
                    )
                }
            }
        }
    }
}
