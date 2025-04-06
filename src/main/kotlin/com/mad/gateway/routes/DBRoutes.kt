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
                    val response = dbService.create(request.table, request.data)
                    call.respond(HttpStatusCode.Created, response)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject
                    loggingService.logInfo(
                            "Record created in table: ${request.table}",
                            mapOf("userId" to (userId ?: "unknown"), "table" to request.table)
                    )
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to create record",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to create record")
                    )
                }
            }
        }

        // Read records
        authenticate("auth-jwt") {
            post("/read") {
                try {
                    val request = call.receive<ReadRequest>()
                    val response = dbService.read(request.query, request.params)
                    call.respond(response)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject
                    loggingService.logInfo(
                            "Query executed: ${request.query}",
                            mapOf("userId" to (userId ?: "unknown"))
                    )
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to execute query",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to execute query")
                    )
                }
            }
        }

        // Update records
        authenticate("auth-jwt") {
            post("/update") {
                try {
                    val request = call.receive<UpdateRequest>()
                    val response =
                            dbService.update(
                                    request.table,
                                    request.data,
                                    request.condition,
                                    request.conditionParams
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
                    loggingService.logError(
                            "Failed to update records",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to update records")
                    )
                }
            }
        }

        // Delete records
        authenticate("auth-jwt") {
            post("/delete") {
                try {
                    val request = call.receive<DeleteRequest>()
                    val response =
                            dbService.delete(
                                    request.table,
                                    request.condition,
                                    request.conditionParams
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
                    loggingService.logError(
                            "Failed to delete records",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to delete records")
                    )
                }
            }
        }
    }
}
