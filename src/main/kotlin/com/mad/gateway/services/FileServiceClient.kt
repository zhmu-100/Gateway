package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the File service (MinIO) */
class FileServiceClient(client: HttpClient, private val fileServiceUrl: String) :
        ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String = fileServiceUrl

    // Convert PartData to FormBuilder
    suspend fun uploadFile(
            userId: String,
            fileType: String,
            parts: List<PartData>
    ): Map<String, Any> {
        return client.submitFormWithBinaryData(
                        url = "$baseUrl/files/upload/$userId/$fileType",
                        formData =
                                formData {
                                    // Convert parts to form data
                                    parts.forEach { part ->
                                        when (part) {
                                            is PartData.FileItem -> {
                                                append(
                                                        "file",
                                                        part.streamProvider()
                                                                .readBytes(), // Use streamProvider
                                                        // instead of content
                                                        Headers.build {
                                                            append(
                                                                    HttpHeaders.ContentDisposition,
                                                                    "form-data; name=\"file\"; filename=\"${part.originalFileName}\""
                                                            )
                                                            append(
                                                                    HttpHeaders.ContentType,
                                                                    part.contentType.toString()
                                                            )
                                                        }
                                                )
                                            }
                                            is PartData.FormItem -> {
                                                append(part.name ?: "param", part.value)
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                )
                .body()
    }

    suspend fun uploadProfileImage(userId: String, parts: List<PartData>): Map<String, Any> {
        return uploadFile(userId, "profile", parts)
    }

    // Upload file with direct parameters
    suspend fun uploadFile(
            fileBytes: ByteArray,
            fileName: String,
            mimeType: String,
            userId: String,
            isPrivate: Boolean = false,
            isTemporary: Boolean = false,
            ecosystemId: String? = null,
            folder: String? = null,
            fileType: String = "general"
    ): UploadResponse {
        val formData =
                MultiPartFormDataContent(
                        formData {
                            append(
                                    "file",
                                    fileBytes,
                                    Headers.build {
                                        append(
                                                HttpHeaders.ContentDisposition,
                                                "form-data; name=\"file\"; filename=\"$fileName\""
                                        )
                                        append(HttpHeaders.ContentType, mimeType)
                                    }
                            )
                            append("private", isPrivate.toString())
                            append("temporary", isTemporary.toString())
                            ecosystemId?.let { append("ecosystemId", it) }
                            folder?.let { append("folder", it) }
                        }
                )

        return client.post("$baseUrl/files/upload/$userId/$fileType") { setBody(formData) }.body()
    }

    suspend fun fixUploadFile(
            fileId: String,
            fileBytes: ByteArray,
            fileName: String,
            mimeType: String
    ): UploadResponse {
        val formData =
                MultiPartFormDataContent(
                        formData {
                            append(
                                    "file",
                                    fileBytes,
                                    Headers.build {
                                        append(
                                                HttpHeaders.ContentDisposition,
                                                "form-data; name=\"file\"; filename=\"$fileName\""
                                        )
                                        append(HttpHeaders.ContentType, mimeType)
                                    }
                            )
                        }
                )

        return client.post("$baseUrl/files/fix-upload/$fileId") { setBody(formData) }.body()
    }

    suspend fun getFile(fileId: String): ByteArray {
        return client
                .get("$baseUrl/files/$fileId") { accept(ContentType.Application.OctetStream) }
                .body()
    }

    suspend fun getProfileImage(userId: String): ByteArray {
        return client.get("$baseUrl/files/profile/$userId") { accept(ContentType.Image.Any) }.body()
    }

    suspend fun deleteFile(fileId: String): Boolean {
        val response = client.delete("$baseUrl/files/$fileId")
        return response.status.value in 200..299
    }

    /** Get a file as a flow of bytes */
    fun getFileAsFlow(id: String): Flow<ByteArray> = flow {
        logger.info { "Getting file with ID: $id as flow" }

        val channel = client.get("$baseUrl/$id").body<ByteReadChannel>()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int

        while (channel.readAvailable(buffer, 0, buffer.size).also { bytesRead = it } > 0) {
            emit(buffer.copyOf(bytesRead))
        }
    }

    /** Get a file URL */
    suspend fun getFileUrl(id: String): GetFileUrlResponse {
        logger.info { "Getting URL for file with ID: $id" }
        return get("/url/$id")
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}

// Data classes based on the proto definitions

data class FileMetadata(
        val userId: String? = null,
        val private: Boolean = false,
        val mimeType: String,
        val fileName: String,
        val size: Long,
        val temporary: Boolean = false,
        val ecosystemId: String? = null,
        val folder: String? = null
)

data class FixFileMetadata(
        val fileId: String,
        val mimeType: String,
        val fileName: String,
        val size: Long
)

data class UploadResponse(val id: String)

data class GetFileUrlResponse(val url: String)
