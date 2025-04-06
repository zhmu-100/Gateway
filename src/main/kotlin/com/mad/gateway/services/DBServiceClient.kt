package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Database service */
class DBServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String =
            application.environment.config.property("services.db.url").getString()

    /** Create a new record in the specified table */
    suspend fun create(table: String, data: Map<String, String>): CreateResponse {
        logger.info { "Creating record in table: $table" }
        val request = CreateRequest(table, data)
        return post("/create", request)
    }

    /** Read records using a custom SQL query */
    suspend fun read(query: String, params: List<String>): ReadResponse {
        logger.info { "Executing query: $query with params: $params" }
        val request = ReadRequest(query, params)
        return post("/read", request)
    }

    /** Update records in the specified table */
    suspend fun update(
            table: String,
            data: Map<String, String>,
            condition: String,
            conditionParams: List<String>
    ): UpdateResponse {
        logger.info { "Updating records in table: $table with condition: $condition" }
        val request = UpdateRequest(table, data, condition, conditionParams)
        return post("/update", request)
    }

    /** Delete records from the specified table */
    suspend fun delete(
            table: String,
            condition: String,
            conditionParams: List<String>
    ): DeleteResponse {
        logger.info { "Deleting records from table: $table with condition: $condition" }
        val request = DeleteRequest(table, condition, conditionParams)
        return post("/delete", request)
    }
}

// Data classes based on the gRPC definitions

data class CreateRequest(val table: String, val data: Map<String, String>)

data class CreateResponse(val success: Boolean, val message: String, val insertedId: Long)

data class ReadRequest(val query: String, val params: List<String>)

data class Row(val columns: Map<String, String>)

data class ReadResponse(val rows: List<Row>)

data class UpdateRequest(
        val table: String,
        val data: Map<String, String>,
        val condition: String,
        val conditionParams: List<String>
)

data class UpdateResponse(val success: Boolean, val message: String, val rowsAffected: Long)

data class DeleteRequest(
        val table: String,
        val condition: String,
        val conditionParams: List<String>
)

data class DeleteResponse(val success: Boolean, val message: String, val rowsAffected: Long)
