package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Database service */
class DBServiceClient(client: HttpClient, baseUrl: String) :
        ServiceClient(client, baseUrl), KoinComponent {
    private val application: Application by inject()

    /** Create a new record in the specified table */
    suspend fun create(table: String, data: Map<String, String>): CreateResponse {
        logger.info { "Creating record in table: $table" }
        val request = CreateRequest(table, data)
        return post("/create", request)
    }

    /** Read records from a table with optional columns and filters */
    suspend fun read(
            table: String,
            columns: List<String>? = null,
            filters: Map<String, String>? = null
    ): ReadResponse {
        logger.info { "Reading from table: $table, columns: $columns, filters: $filters" }
        val request = ReadRequest(table, columns, filters)
        return post("/read", request)
    }

    /**
     * Legacy read method using custom SQL query Note: This method is maintained for backward
     * compatibility
     */
    suspend fun read(query: String, params: List<String>): ReadResponse {
        logger.info { "Executing query: $query with params: $params" }
        // Convert to the new format - this is a simplified approach and may not work for all cases
        logger.warn { "Using legacy read method with raw SQL is not recommended" }
        return post("/read", mapOf("query" to query, "params" to params))
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

// Data classes based on the DB ORM service API

@Serializable
data class CreateRequest(val table: String?, val data: Map<String, String> = emptyMap())

@Serializable
data class CreateResponse(val success: Boolean, val message: String, val insertedId: Long)

@Serializable
data class ReadRequest(
        val table: String,
        val columns: List<String>? = null,
        val filters: Map<String, String>? = null
)

@Serializable data class Row(val columns: Map<String, String>)

@Serializable data class ReadResponse(val rows: List<Row>)

@Serializable
data class UpdateRequest(
        val table: String?,
        val data: Map<String, String> = emptyMap(),
        val condition: String?,
        val conditionParams: List<String>? = null
)

@Serializable
data class UpdateResponse(val success: Boolean, val message: String, val rowsAffected: Long)

@Serializable
data class DeleteRequest(
        val table: String?,
        val condition: String?,
        val conditionParams: List<String>? = null
)

@Serializable
data class DeleteResponse(val success: Boolean, val message: String, val rowsAffected: Long)
