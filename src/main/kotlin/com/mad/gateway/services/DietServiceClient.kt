package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Diet service */
class DietServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String =
            application.environment.config.property("services.diet.url").getString()

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

    /** Get a food by ID */
    suspend fun getFood(id: String): Food {
        logger.info { "Getting food with ID: $id" }
        return get("/foods/$id")
    }

    /** List foods with optional name filter */
    suspend fun listFoods(nameFilter: String? = null): ListFoodsResponse {
        logger.info { "Listing foods with filter: $nameFilter" }
        val queryParam = nameFilter?.let { "?nameFilter=$it" } ?: ""
        return get("/foods$queryParam")
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
