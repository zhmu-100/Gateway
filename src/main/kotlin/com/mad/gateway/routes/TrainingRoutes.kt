package com.mad.gateway.routes

import com.mad.gateway.services.LoggingServiceClient
import com.mad.gateway.services.TrainingServiceClient
import com.mad.gateway.services.Workout
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** Training routes */
fun Route.trainingRoutes() {
    val trainingService by inject<TrainingServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/training") {
        // Get workout by ID
        get("/workouts/{id}") {
            try {
                val id =
                        call.parameters["id"]
                                ?: return@get call.respond(
                                        HttpStatusCode.BadRequest,
                                        mapOf("error" to "Missing ID parameter")
                                )

                val workout = trainingService.getWorkout(id)
                call.respond(workout)
            } catch (e: Exception) {
                loggingService.logError(
                        "Failed to get workout",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error").toString())
                )
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Workout not found"))
            }
        }

        // List all workouts
        get("/workouts") {
            try {
                val workouts = trainingService.listWorkouts()
                call.respond(workouts)
            } catch (e: Exception) {
                loggingService.logError(
                        "Failed to list workouts",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error"))
                )
                call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to list workouts")
                )
            }
        }

        // Get exercises for a workout
        get("/workouts/{id}/exercises") {
            try {
                val id =
                        call.parameters["id"]
                                ?: return@get call.respond(
                                        HttpStatusCode.BadRequest,
                                        mapOf("error" to "Missing ID parameter")
                                )

                val exercises = trainingService.getWorkoutExercises(id)
                call.respond(exercises)
            } catch (e: Exception) {
                loggingService.logError(
                        "Failed to get workout exercises",
                        e,
                        mapOf("error" to (e.message ?: "Unknown error"))
                )
                call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Workout exercises not found")
                )
            }
        }

        // Protected routes that require authentication
        authenticate("auth-jwt") {
            // Create a new workout
            post("/workouts") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    if (userId != null) {
                        val workout = call.receive<Workout>()
                        val createdWorkout = trainingService.createWorkout(workout)
                        call.respond(HttpStatusCode.Created, createdWorkout)

                        loggingService.logInfo(
                                "Workout created: ${createdWorkout.name}",
                                mapOf("userId" to userId, "workoutId" to createdWorkout.id!!)
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                    }
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to create workout",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to create workout")
                    )
                }
            }

            // Update a workout
            put("/workouts/{id}") {
                try {
                    val id =
                            call.parameters["id"]
                                    ?: return@put call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    if (userId != null) {
                        val workoutRequest = call.receive<Workout>()

                        // Ensure the workout ID matches the path parameter
                        val workout = workoutRequest.copy(id = id)

                        val updatedWorkout = trainingService.updateWorkout(workout)
                        call.respond(updatedWorkout)

                        loggingService.logInfo(
                                "Workout updated: ${updatedWorkout.name}",
                                mapOf("userId" to userId, "workoutId" to id)
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                    }
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to update workout",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to update workout")
                    )
                }
            }

            // Delete a workout
            delete("/workouts/{id}") {
                try {
                    val id =
                            call.parameters["id"]
                                    ?: return@delete call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing ID parameter")
                                    )

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    if (userId != null) {
                        trainingService.deleteWorkout(id)
                        call.respond(HttpStatusCode.NoContent)

                        loggingService.logInfo(
                                "Workout deleted",
                                mapOf("userId" to userId, "workoutId" to id)
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                    }
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to delete workout",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to delete workout")
                    )
                }
            }

            // Create a custom workout
            post("/workouts/custom") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    if (userId != null) {
                        val workout = call.receive<Workout>()
                        val response = trainingService.createCustomWorkout(workout)
                        call.respond(HttpStatusCode.Created, response)

                        loggingService.logInfo(
                                "Custom workout created: ${workout.name}",
                                mapOf("userId" to userId, "workoutId" to response.id)
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.Unauthorized,
                                mapOf("error" to "User ID not found in token")
                        )
                    }
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to create custom workout",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to create custom workout")
                    )
                }
            }
        }
    }
}
