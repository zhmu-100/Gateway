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

/** Statistics routes */
fun Route.statisticsRoutes() {
    val statisticsService by inject<StatisticsServiceClient>()
    val loggingService by inject<LoggingServiceClient>()

    route("/statistics") {
        // GPS data routes
        route("/gps") {
            // Get GPS data for an exercise
            get {
                try {
                    val exerciseId =
                            call.request.queryParameters["exerciseId"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing exerciseId parameter")
                                    )

                    val gpsData = statisticsService.getGPSData(exerciseId)
                    call.respond(gpsData)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to get GPS data",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to get GPS data")
                    )
                }
            }

            // Upload GPS data (authenticated)
            authenticate("auth-jwt") {
                post {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val gpsData = call.receive<GPSData>()
                            statisticsService.uploadGPSData(gpsData)
                            call.respond(
                                    HttpStatusCode.Created,
                                    mapOf("message" to "GPS data uploaded successfully")
                            )

                            loggingService.logInfo(
                                    "GPS data uploaded",
                                    mapOf(
                                            "userId" to userId,
                                            "exerciseId" to gpsData.meta.exerciseId,
                                            "positionsCount" to gpsData.positions.size
                                    )
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to upload GPS data",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to upload GPS data")
                        )
                    }
                }
            }
        }

        // Heart rate data routes
        route("/heartrate") {
            // Get heart rate data for an exercise
            get {
                try {
                    val exerciseId =
                            call.request.queryParameters["exerciseId"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing exerciseId parameter")
                                    )

                    val heartRateData = statisticsService.getHeartRateData(exerciseId)
                    call.respond(heartRateData)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to get heart rate data",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to get heart rate data")
                    )
                }
            }

            // Upload heart rate data (authenticated)
            authenticate("auth-jwt") {
                post {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val heartRateData = call.receive<HeartRateData>()
                            statisticsService.uploadHeartRateData(heartRateData)
                            call.respond(
                                    HttpStatusCode.Created,
                                    mapOf("message" to "Heart rate data uploaded successfully")
                            )

                            loggingService.logInfo(
                                    "Heart rate data uploaded",
                                    mapOf(
                                            "userId" to userId,
                                            "exerciseId" to heartRateData.meta.exerciseId,
                                            "bpm" to heartRateData.bpm
                                    )
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to upload heart rate data",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to upload heart rate data")
                        )
                    }
                }
            }
        }

        // Calories data routes
        route("/calories") {
            // Get calories data for a user
            get {
                try {
                    val userId =
                            call.request.queryParameters["userId"]
                                    ?: return@get call.respond(
                                            HttpStatusCode.BadRequest,
                                            mapOf("error" to "Missing userId parameter")
                                    )

                    // Check if the authenticated user is requesting their own data
                    val principal = call.principal<JWTPrincipal>()
                    val authenticatedUserId = principal?.payload?.subject

                    if (authenticatedUserId == null || authenticatedUserId != userId) {
                        call.respond(
                                HttpStatusCode.Forbidden,
                                mapOf("error" to "You can only access your own calories data")
                        )
                        return@get
                    }

                    val caloriesData = statisticsService.getCaloriesData(userId)
                    call.respond(caloriesData)
                } catch (e: Exception) {
                    loggingService.logError(
                            "Failed to get calories data",
                            e,
                            mapOf("error" to (e.message ?: "Unknown error"))
                    )
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to get calories data")
                    )
                }
            }

            // Upload calories data (authenticated)
            authenticate("auth-jwt") {
                post {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.subject

                        if (userId != null) {
                            val caloriesDataRequest = call.receive<CaloriesData>()

                            // Ensure the user ID matches the authenticated user
                            if (caloriesDataRequest.meta.userId != userId) {
                                call.respond(
                                        HttpStatusCode.Forbidden,
                                        mapOf(
                                                "error" to
                                                        "You can only upload your own calories data"
                                        )
                                )
                                return@post
                            }

                            statisticsService.uploadCaloriesData(caloriesDataRequest)
                            call.respond(
                                    HttpStatusCode.Created,
                                    mapOf("message" to "Calories data uploaded successfully")
                            )

                            loggingService.logInfo(
                                    "Calories data uploaded",
                                    mapOf(
                                            "userId" to userId,
                                            "calories" to caloriesDataRequest.calories
                                    )
                            )
                        } else {
                            call.respond(
                                    HttpStatusCode.Unauthorized,
                                    mapOf("error" to "User ID not found in token")
                            )
                        }
                    } catch (e: Exception) {
                        loggingService.logError(
                                "Failed to upload calories data",
                                e,
                                mapOf("error" to (e.message ?: "Unknown error"))
                        )
                        call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Failed to upload calories data")
                        )
                    }
                }
            }
        }
    }
}
