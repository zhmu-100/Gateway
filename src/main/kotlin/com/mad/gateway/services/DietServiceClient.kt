package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Diet service */
class DietServiceClient(client: HttpClient, baseUrl: String) :
        ServiceClient(client, baseUrl), KoinComponent {
    private val application: Application by inject()

    // Meal operations

    /** Create a new meal */
    suspend fun createMeal(meal: Meal): Meal {
        logger.info { "Creating meal: ${meal.name}" }
        val request = CreateMealRequest(meal)
        return post("/meals", request)
    }

    /** Get a meal by ID */
    suspend fun getMeal(id: String): Meal {
        logger.info { "Getting meal with ID: $id" }
        return get("/meals/$id")
    }

    /** List meals within a date range */
    suspend fun listMeals(startDate: String, endDate: String): ListMealsResponse {
        logger.info { "Listing meals from $startDate to $endDate" }
        return get("/meals?startDate=$startDate&endDate=$endDate")
    }

    // Food operations

    /** Create a new food */
    suspend fun createFood(food: Food): Food {
        logger.info { "Creating food: ${food.name}" }
        val request = CreateFoodRequest(food)
        return post("/foods", request)
    }

    /**
     * Get a food by ID
     * @throws ServiceException if the food is not found (with status 404) or other service error
     */
    /**
     * Get a food by ID
     * @throws ServiceException for errors including "not found"
     */
    suspend fun getFood(id: String): Food {
        logger.info { "Getting food with ID: $id" }
        try {
            return get("/foods/$id")
        } catch (e: ServiceException) {
            // Convert 404s to a more user-friendly message
            if (e.statusCode == HttpStatusCode.NotFound.value) {
                logger.info { "Food not found with ID: $id" }
                throw ServiceException(e.statusCode, "Food not found with id: $id")
            }
            // For other errors, just add some context
            logger.error(e) { "Error getting food with ID: $id" }
            throw ServiceException(e.statusCode, "Failed to get food: ${e.errorBody}")
        }
    }

    /**
     * List foods with optional name filter
     * @throws ServiceException if there is a service error
     */
    /**
     * List foods with optional name filter with improved error handling
     * @throws ServiceException for service errors
     */
    suspend fun listFoods(nameFilter: String? = null): ListFoodsResponse {
        logger.info { "Listing foods with filter: $nameFilter" }
        val queryParam = nameFilter?.let { "?nameFilter=$it" } ?: ""
        try {
            return get("/foods$queryParam")
        } catch (e: ServiceException) {
            logger.error(e) { "Error listing foods with filter: $nameFilter" }
            throw ServiceException(e.statusCode, "Failed to list foods: ${e.errorBody}")
        }
    }
}

// Data classes based on the proto definitions

data class Vitamin(val id: String? = null, val name: String, val amount: Double, val unit: String)

data class VitaminFood(val id: String, val vitamins: List<Vitamin> = emptyList())

data class Mineral(val id: String? = null, val name: String, val amount: Double, val unit: String)

data class Food(
        val id: String? = null,
        val name: String,
        val description: String,
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val saturatedFats: Double,
        val transFats: Double,
        val fiber: Double,
        val sugar: Double,
        val vitamins: List<Vitamin> = emptyList(),
        val minerals: List<Mineral> = emptyList()
)

enum class MealType {
    UNSPECIFIED,
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}

data class Meal(
        val id: String? = null,
        val name: String,
        val mealType: MealType,
        val foods: List<Food> = emptyList(),
        val date: String // ISO-8601 timestamp
)

data class CreateMealRequest(val meal: Meal)

data class ListMealsResponse(
        val meals: List<Meal>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

data class CreateFoodRequest(val food: Food)

data class ListFoodsResponse(
        val foods: List<Food>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)
