package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Statistics service */
class StatisticsServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String =
            application.environment.config.property("services.statistics.url").getString()

    /** Get GPS data for an exercise */
    suspend fun getGPSData(exerciseId: String): GetGPSDataResponse {
        logger.info { "Getting GPS data for exercise ID: $exerciseId" }
        return get("/gps?exerciseId=$exerciseId")
    }

    /** Get heart rate data for an exercise */
    suspend fun getHeartRateData(exerciseId: String): GetHeartRateDataResponse {
        logger.info { "Getting heart rate data for exercise ID: $exerciseId" }
        return get("/heartrate?exerciseId=$exerciseId")
    }

    /** Get calories data for a user */
    suspend fun getCaloriesData(userId: String): GetCaloriesDataResponse {
        logger.info { "Getting calories data for user ID: $userId" }
        return get("/calories?userId=$userId")
    }

    /** Upload GPS data */
    suspend fun uploadGPSData(gpsData: GPSData) {
        logger.info { "Uploading GPS data for exercise ID: ${gpsData.meta.exerciseId}" }
        post<Unit>("/gps", gpsData)
    }

    /** Upload heart rate data */
    suspend fun uploadHeartRateData(heartRateData: HeartRateData) {
        logger.info {
            "Uploading heart rate data for exercise ID: ${heartRateData.meta.exerciseId}"
        }
        post<Unit>("/heartrate", heartRateData)
    }

    /** Upload calories data */
    suspend fun uploadCaloriesData(caloriesData: CaloriesData) {
        logger.info { "Uploading calories data for user ID: ${caloriesData.meta.userId}" }
        post<Unit>("/calories", caloriesData)
    }
}

// Data classes based on the proto definitions

data class ExerciseMetadata(
        val id: String? = null,
        val exerciseId: String,
        val timestamp: String // ISO-8601 timestamp
)

data class UserMetadata(
        val id: String? = null,
        val userId: String,
        val timestamp: String // ISO-8601 timestamp
)

data class GPSPosition(
        val timestamp: String, // ISO-8601 timestamp
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val speed: Double,
        val accuracy: Double
)

data class GPSData(val meta: ExerciseMetadata, val positions: List<GPSPosition> = emptyList())

data class HeartRateData(val meta: ExerciseMetadata, val bpm: Int)

data class CaloriesData(val meta: UserMetadata, val calories: Double)

data class GetGPSDataResponse(val gpsData: List<GPSData>)

data class GetHeartRateDataResponse(val heartRateData: List<HeartRateData>)

data class GetCaloriesDataResponse(val caloriesData: List<CaloriesData>)
