package com.mad.gateway.routes

import com.mad.gateway.services.DietServiceClient
import com.mad.gateway.services.Food
import com.mad.gateway.services.LoggingServiceClient
import com.mad.gateway.services.Meal
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
                    val id =
                            call.parameters["id"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val food = dietService.getFood(id)
                    call.respond(food)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to get food",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error").toString())
                    )
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Food not found"))
                }
            }

            // List foods with optional name filter
            get {
                try {
                    val nameFilter = call.request.queryParameters["nameFilter"]
                    val foods = dietService.listFoods(nameFilter)
                    call.respond(foods)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to list foods",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to list foods")
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
                    val id =
                            call.parameters["id"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val meal = dietService.getMeal(id)
                    call.respond(meal)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to get meal",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Meal not found"))
                }
            }

            // List meals within a date range
            get {
                try {
                    val startDate =
                            call.request.queryParameters["startDate"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing startDate parameter")
                                    )

                    val endDate =
                            call.request.queryParameters["endDate"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing endDate parameter")
                                    )

                    val meals = dietService.listMeals(startDate, endDate)
                    call.respond(meals)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to list meals",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to list meals")
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
