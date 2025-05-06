package com.mad.gateway.routes

import com.mad.gateway.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

/** Database routes */
fun Route.dbRoutes() {
    val dbService by inject<DBServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/db") {
        // Create a new record
        authenticate("auth-jwt") {
            post("/create") {
                try {
                    val request = call.receive<CreateRequest>()
                    logger.info { "Received create request for table: ${request.table}" }

                    // Validate request
                    if (request.table.isNullOrBlank() || request.data.isEmpty()) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid request: table and data are required")
                        )
                        return@post
                    }

                    val response = dbService.create(request.table, request.data)
                    call.respond(HttpStatusCode.Created, response)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject
                    loggingService.logInfo(
                            "Record created in table: ${request.table}",
                            mapOf("userId" to (userId ?: "unknown"), "table" to request.table)
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Failed to create record: ${e.message}" }
                    loggingService.logError(
                            "Failed to create record",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to create record: ${e.message}")
                    )
                }
            }
        }

        // Read records
        authenticate("auth-jwt") {
            post("/read") {
                try {
                    val requestBody = call.receiveText()
                    logger.info { "Received read request with body: $requestBody" }

                    val request = call.receive<ReadRequest>()

                    // Validate request
                    if (request.query.isNullOrBlank()) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Query parameter is required")
                        )
                        return@post
                    }

                    logger.info { "Processing read request with query: ${request.query}" }
                    val response = dbService.read(request.query, request.params ?: emptyList())
                    call.respond(response)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject
                    loggingService.logInfo(
                            "Query executed: ${request.query}",
                            mapOf("userId" to (userId ?: "unknown"))
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Failed to execute query: ${e.message}" }
                    loggingService.logError(
                            "Failed to execute query",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to execute query: ${e.message}")
                    )
                }
            }
        }

        // Update records
        authenticate("auth-jwt") {
            post("/update") {
                try {
                    val request = call.receive<UpdateRequest>()
                    logger.info { "Received update request for table: ${request.table}" }

                    // Validate request
                    if (request.table.isNullOrBlank() ||
                                    request.data.isEmpty() ||
                                    request.condition.isNullOrBlank()
                    ) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                        "error" to
                                                "Invalid request: table, data, and condition are required"
                                )
                        )
                        return@post
                    }

                    val response =
                            dbService.update(
                                    request.table,
                                    request.data,
                                    request.condition,
                                    request.conditionParams ?: emptyList()
                            )
                    call.respond(response)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject
                    loggingService.logInfo(
                            "Records updated in table: ${request.table}",
                            mapOf(
                                    "userId" to (userId ?: "unknown"),
                                    "table" to request.table,
                                    "rowsAffected" to response.rowsAffected.toString()
                            )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Failed to update records: ${e.message}" }
                    loggingService.logError(
                            "Failed to update records",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to update records: ${e.message}")
                    )
                }
            }
        }

        // Delete records
        authenticate("auth-jwt") {
            post("/delete") {
                try {
                    val request = call.receive<DeleteRequest>()
                    logger.info { "Received delete request for table: ${request.table}" }

                    // Validate request
                    if (request.table.isNullOrBlank() || request.condition.isNullOrBlank()) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                        "error" to
                                                "Invalid request: table and condition are required"
                                )
                        )
                        return@post
                    }

                    val response =
                            dbService.delete(
                                    request.table,
                                    request.condition,
                                    request.conditionParams ?: emptyList()
                            )
                    call.respond(response)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject
                    loggingService.logInfo(
                            "Records deleted from table: ${request.table}",
                            mapOf(
                                    "userId" to (userId ?: "unknown"),
                                    "table" to request.table,
                                    "rowsAffected" to response.rowsAffected.toString()
                            )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Failed to delete records: ${e.message}" }
                    loggingService.logError(
                            "Failed to delete records",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to delete records: ${e.message}")
                    )
                }
            }
        }
    }
}
