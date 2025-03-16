package com.mad.gateway.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            // Configure GSON serialization
            setPrettyPrinting()
            serializeNulls()
            // Add any custom type adapters here if needed
        }
    }
}

// Create a shared Gson instance for use throughout the application
object GsonProvider {
    val gson: Gson by lazy {
        GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                // Add any custom type adapters here if needed
                .create()
    }
}

// Extension function to convert objects to JSON
inline fun <reified T> T.toJson(): String = GsonProvider.gson.toJson(this, T::class.java)

// Extension function to convert JSON to objects
inline fun <reified T> String.fromJson(): T = GsonProvider.gson.fromJson(this, T::class.java)
