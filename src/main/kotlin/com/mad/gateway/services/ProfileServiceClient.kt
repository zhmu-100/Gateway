package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Profile service */
class ProfileServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String =
            application.environment.config.property("services.profile.url").getString()

    /** Create a new user profile */
    suspend fun createProfile(profile: UserProfile): UserProfile {
        logger.info { "Creating profile for user: ${profile.name}" }
        val request = CreateProfileRequest(profile)
        return post("/", request)
    }

    /** Get a user profile by ID */
    suspend fun getProfile(id: String): UserProfile {
        logger.info { "Getting profile for user ID: $id" }
        return get("/$id")
    }

    /** List profiles with pagination */
    suspend fun listProfiles(page: Int = 1, pageSize: Int = 20): ListProfilesResponse {
        logger.info { "Listing profiles: page=$page, pageSize=$pageSize" }
        return get("/?page=$page&pageSize=$pageSize")
    }

    /** Update a user profile */
    suspend fun updateProfile(profile: UserProfile): UserProfile {
        logger.info { "Updating profile for user ID: ${profile.id}" }
        val request = UpdateProfileRequest(profile)
        return put("/${profile.id}", request)
    }

    /** Delete a user profile */
    suspend fun deleteProfile(id: String) {
        logger.info { "Deleting profile for user ID: $id" }
        delete<Unit>("/$id")
    }

    /** Follow another user */
    suspend fun follow(followerId: String, followeeId: String) {
        logger.info { "User $followerId is following user $followeeId" }
        val request = FollowRequest(followerId, followeeId)
        post<Unit>("/follow", request)
    }

    /** Unfollow another user */
    suspend fun unfollow(followerId: String, followeeId: String) {
        logger.info { "User $followerId is unfollowing user $followeeId" }
        val request = UnfollowRequest(followerId, followeeId)
        post<Unit>("/unfollow", request)
    }

    /** List followers for a user */
    suspend fun listFollowers(
            userId: String,
            page: Int = 1,
            pageSize: Int = 20
    ): ListFollowersResponse {
        logger.info { "Listing followers for user ID: $userId" }
        return get("/$userId/followers?page=$page&pageSize=$pageSize")
    }

    /** List users that a user is following */
    suspend fun listFollowing(
            userId: String,
            page: Int = 1,
            pageSize: Int = 20
    ): ListFollowingResponse {
        logger.info { "Listing following for user ID: $userId" }
        return get("/$userId/following?page=$page&pageSize=$pageSize")
    }
}

// Data classes based on the proto definitions

data class Location(val country: String, val city: String)

data class Birthdate(val year: Int, val month: Int, val day: Int)

data class UserProfile(
        val id: String,
        val name: String,
        val email: String,
        val imageId: String? = null,
        val bio: String? = null,
        val location: Location? = null,
        val birthdate: Birthdate? = null,
        val weight: Double? = null,
        val height: Double? = null,
        val followerCount: Int = 0,
        val followingCount: Int = 0
)

data class CreateProfileRequest(val profile: UserProfile)

data class UpdateProfileRequest(val profile: UserProfile)

data class ListProfilesResponse(
        val profiles: List<UserProfile>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

data class FollowRequest(val followerId: String, val followeeId: String)

data class UnfollowRequest(val followerId: String, val followeeId: String)

data class ListFollowersResponse(
        val followerIds: List<String>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

data class ListFollowingResponse(
        val followingIds: List<String>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)
