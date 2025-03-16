package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Training service */
class TrainingServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String =
            application.environment.config.property("services.training.url").getString()

    /** Create a new workout */
    suspend fun createWorkout(workout: Workout): Workout {
        logger.info { "Creating workout: ${workout.name}" }
        return post("/workouts", workout)
    }

    /** Get a workout by ID */
    suspend fun getWorkout(id: String): Workout {
        logger.info { "Getting workout with ID: $id" }
        return get("/workouts/$id")
    }

    /** List all workouts */
    suspend fun listWorkouts(): ListWorkoutsResponse {
        logger.info { "Listing all workouts" }
        return get("/workouts")
    }

    /** Update a workout */
    suspend fun updateWorkout(workout: Workout): Workout {
        logger.info { "Updating workout with ID: ${workout.id}" }
        return put("/workouts/${workout.id}", workout)
    }

    /** Delete a workout */
    suspend fun deleteWorkout(id: String) {
        logger.info { "Deleting workout with ID: $id" }
        delete<Unit>("/workouts/$id")
    }

    /** Get exercises for a workout */
    suspend fun getWorkoutExercises(workoutId: String): GetWorkoutExercisesResponse {
        logger.info { "Getting exercises for workout with ID: $workoutId" }
        return get("/workouts/$workoutId/exercises")
    }

    /** Create a custom workout */
    suspend fun createCustomWorkout(workout: Workout): CreateCustomWorkoutResponse {
        logger.info { "Creating custom workout: ${workout.name}" }
        return post("/workouts/custom", workout)
    }
}

// Data classes based on the proto definitions

enum class ExerciseReaction {
    UNSPECIFIED,
    EXCELLENT,
    GOOD,
    OK,
    BAD,
    VERY_BAD
}

enum class ExerciseType {
    UNSPECIFIED,
    STATIC,
    DYNAMIC
}

enum class ExerciseName {
    UNSPECIFIED,
    PUSHUPS,
    PULLUPS,
    SQUATS,
    PLANK,
    RUNNING,
    CYCLING
}

data class Exercise(
        val name: ExerciseName,
        val duration: String, // ISO-8601 duration
        val exerciseType: ExerciseType,
        val sets: Int? = null,
        val reps: Int? = null,
        val distance: Int? = null,
        val steps: Int? = null,
        val bmp: Int? = null,
        val speed: Int? = null,
        val weight: Int? = null,
        val calories: Double? = null,
        val reaction: ExerciseReaction? = null,
        val note: String? = null
)

data class Workout(
        val id: String? = null,
        val name: String,
        val date: String, // ISO-8601 timestamp
        val exercises: List<Exercise> = emptyList()
)

data class ListWorkoutsResponse(
        val workouts: List<Workout>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

data class GetWorkoutExercisesResponse(val exercises: List<Exercise>)

data class CreateCustomWorkoutResponse(val id: String)
