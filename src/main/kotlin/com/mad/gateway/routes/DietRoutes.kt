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

/** Diet routes */
fun Route.dietRoutes() {
    val dietService by inject<DietServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/diet") {
        // Food routes
        route("/foods") {
            // Get food by ID
            get("/{id}") {
                try {
                    val id = call.parameters["id"]
                    if (id == null) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Missing ID parameter")
                        )
                        return@get
                    }

                    val food = dietService.getFood(id)
                    call.respond(food)
                } catch (e: Exception) {
                    // Local logging first to ensure we always have a record
                    application.log.error("Failed to get food with ID: ${call.parameters["id"]}", e)

                    try {
                        loggingService.logError(
                                "Failed to get food",
                                e,
                                mapOf(
                                        "error" to (e.message ?: "Unknown error").toString(),
                                        "path" to call.request.path(),
                                        "foodId" to (call.parameters["id"] ?: "unknown")
                                )
                        )
                    } catch (loggingError: Exception) {
                        application.log.error("Additionally, logging service failed", loggingError)
                    }

                    // Determine appropriate status code
                    val statusCode =
                            when (e) {
                                is io.ktor.client.plugins.ClientRequestException -> {
                                    if (e.response.status == HttpStatusCode.NotFound)
                                            HttpStatusCode.NotFound
                                    else HttpStatusCode.BadRequest
                                }
                                is io.ktor.client.plugins.ServerResponseException ->
                                        HttpStatusCode.BadGateway
                                else -> HttpStatusCode.InternalServerError
                            }

                    call.respond(
                            statusCode,
                            mapOf("error" to "Failed to get food: ${e.message ?: "Unknown error"}")
                    )
                }
            }

            // List foods with optional name filter
            get {
                try {
                    val nameFilter = call.request.queryParameters["nameFilter"]
                    val foods = dietService.listFoods(nameFilter)
                    call.respond(foods)
                } catch (e: Exception) {
                    // Local logging first to ensure we always have a record
                    application.log.error(
                            "Failed to list foods with filter: ${call.request.queryParameters["nameFilter"]}",
                            e
                    )

                    try {
                        loggingService.logError(
                                "Failed to list foods",
                                e,
                                mapOf(
                                        "error" to (e.message ?: "Unknown error").toString(),
                                        "path" to call.request.path(),
                                        "nameFilter" to
                                                (call.request.queryParameters["nameFilter"]
                                                        ?: "none")
                                )
                        )
                    } catch (loggingError: Exception) {
                        application.log.error("Additionally, logging service failed", loggingError)
                    }

                    // Determine appropriate status code
                    val statusCode =
                            when (e) {
                                is ServiceException -> HttpStatusCode.fromValue(e.statusCode)
                                is io.ktor.client.plugins.ClientRequestException -> {
                                    if (e.response.status == HttpStatusCode.NotFound)
                                            HttpStatusCode.NotFound
                                    else HttpStatusCode.BadRequest
                                }
                                is io.ktor.client.plugins.ServerResponseException ->
                                        HttpStatusCode.BadGateway
                                else -> HttpStatusCode.InternalServerError
                            }

                    call.respond(
                            statusCode,
                            mapOf(
                                    "error" to
                                            "Failed to list foods: ${e.message ?: "Unknown error"}"
                            )
                    )
                }
            }

            // Create a new food (authenticated)
            authenticate("auth-jwt") {
                post {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val food = call.receive<Food>()
                            val createdFood = dietService.createFood(food)
                            call.respond(HttpStatusCode.Created, createdFood)

                            loggingService.logInfo(
                                    "Food created: ${createdFood.name}",
                                    mapOf("userId" to userId, "foodId" to createdFood.id!!)
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to create food",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error").toString())
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to create food")
                        )
                    }
                }
            }
        }

        // Meal routes
        route("/meals") {
            // Get meal by ID
            get("/{id}") {
                try {
                    val id = call.parameters["id"]
                    if (id == null) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Missing ID parameter")
                        )
                        return@get
                    }

                    val meal = dietService.getMeal(id)
                    call.respond(meal)
                } catch (e: Exception) {
                    // Local logging first to ensure we always have a record
                    application.log.error("Failed to get meal with ID: ${call.parameters["id"]}", e)

                    try {
                        loggingService.logError(
                                "Failed to get meal",
                                e,
                                mapOf(
                                        "error" to (e.message ?: "Unknown error").toString(),
                                        "path" to call.request.path(),
                                        "mealId" to (call.parameters["id"] ?: "unknown")
                                )
                        )
                    } catch (loggingError: Exception) {
                        application.log.error("Additionally, logging service failed", loggingError)
                    }

                    // Determine appropriate status code
                    val statusCode =
                            when (e) {
                                is ServiceException -> HttpStatusCode.fromValue(e.statusCode)
                                is io.ktor.client.plugins.ClientRequestException -> {
                                    if (e.response.status == HttpStatusCode.NotFound)
                                            HttpStatusCode.NotFound
                                    else HttpStatusCode.BadRequest
                                }
                                is io.ktor.client.plugins.ServerResponseException ->
                                        HttpStatusCode.BadGateway
                                else -> HttpStatusCode.InternalServerError
                            }

                    call.respond(
                            statusCode,
                            mapOf("error" to "Failed to get meal: ${e.message ?: "Unknown error"}")
                    )
                }
            }

            // List meals within a date range
            get {
                try {
                    val startDate = call.request.queryParameters["startDate"]
                    if (startDate == null) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Missing startDate parameter")
                        )
                        return@get
                    }

                    val endDate = call.request.queryParameters["endDate"]
                    if (endDate == null) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Missing endDate parameter")
                        )
                        return@get
                    }

                    val meals = dietService.listMeals(startDate, endDate)
                    call.respond(meals)
                } catch (e: Exception) {
                    // Local logging first to ensure we always have a record
                    application.log.error("Failed to list meals with date range", e)

                    try {
                        loggingService.logError(
                                "Failed to list meals",
                                e,
                                mapOf(
                                        "error" to (e.message ?: "Unknown error").toString(),
                                        "path" to call.request.path(),
                                        "startDate" to
                                                (call.request.queryParameters["startDate"]
                                                        ?: "unknown"),
                                        "endDate" to
                                                (call.request.queryParameters["endDate"]
                                                        ?: "unknown")
                                )
                        )
                    } catch (loggingError: Exception) {
                        application.log.error("Additionally, logging service failed", loggingError)
                    }

                    // Determine appropriate status code
                    val statusCode =
                            when (e) {
                                is ServiceException -> HttpStatusCode.fromValue(e.statusCode)
                                is io.ktor.client.plugins.ClientRequestException -> {
                                    if (e.response.status == HttpStatusCode.NotFound)
                                            HttpStatusCode.NotFound
                                    else HttpStatusCode.BadRequest
                                }
                                is io.ktor.client.plugins.ServerResponseException ->
                                        HttpStatusCode.BadGateway
                                else -> HttpStatusCode.InternalServerError
                            }

                    call.respond(
                            statusCode,
                            mapOf(
                                    "error" to
                                            "Failed to list meals: ${e.message ?: "Unknown error"}"
                            )
                    )
                }
            }

            // Create a new meal (authenticated)
            authenticate("auth-jwt") {
                post {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val meal = call.receive<Meal>()
                            val createdMeal = dietService.createMeal(meal)
                            call.respond(HttpStatusCode.Created, createdMeal)

                            loggingService.logInfo(
                                    "Meal created: ${createdMeal.name}",
                                    mapOf("userId" to userId, "mealId" to createdMeal.id!!)
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to create meal",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to create meal")
                        )
                    }
                }
            }
        }
    }
}
