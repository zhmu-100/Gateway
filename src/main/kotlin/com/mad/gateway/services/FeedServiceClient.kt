package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/**
 * Client for the Feed service.
 *
 * This client handles communication with the Feed microservice, which manages social feed
 * functionality including posts, comments, and reactions.
 *
 * @property client The HTTP client used for making requests
 */
class FeedServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()

    /**
     * The base URL for the Feed service.
     *
     * Retrieved from the application configuration.
     */
    override val baseUrl: String =
            application.environment.config.property("services.feed.url").getString()

    /**
     * Creates a new post in the feed.
     *
     * @param post The post object to create
     * @return The created post with server-generated fields populated
     */
    suspend fun createPost(post: Post): Post {
        logger.info { "Creating post for user: ${post.userId}" }
        val request = CreatePostRequest(post)
        return post("/posts", request)
    }

    /**
     * Retrieves a post by its ID.
     *
     * @param id The unique identifier of the post
     * @return The post object if found
     */
    suspend fun getPost(id: String): Post {
        logger.info { "Getting post with ID: $id" }
        return get("/posts/$id")
    }

    /**
     * Lists posts for a specific user.
     *
     * @param userId The ID of the user whose posts to retrieve
     * @param viewerId The ID of the user viewing the posts (for permission checking)
     * @param page The page number for pagination (1-based)
     * @param pageSize The number of posts per page
     * @return A paginated response containing posts and pagination metadata
     */
    suspend fun listUserPosts(
            userId: String,
            viewerId: String,
            page: Int = 1,
            pageSize: Int = 20
    ): ListPostsResponse {
        logger.info { "Listing posts for user ID: $userId, viewed by: $viewerId" }
        return get("/users/$userId/posts?viewerId=$viewerId&page=$page&pageSize=$pageSize")
    }

    /**
     * Lists posts for the feed (from followed users).
     *
     * @param viewerId The ID of the user viewing the feed
     * @param page The page number for pagination (1-based)
     * @param pageSize The number of posts per page
     * @return A paginated response containing posts and pagination metadata
     */
    suspend fun listPosts(viewerId: String, page: Int = 1, pageSize: Int = 20): ListPostsResponse {
        logger.info { "Listing feed posts for viewer ID: $viewerId" }
        return get("/posts?viewerId=$viewerId&page=$page&pageSize=$pageSize")
    }

    /**
     * Creates a comment on a post.
     *
     * @param postId The ID of the post to comment on
     * @param comment The comment object to create
     * @return The created comment with server-generated fields populated
     */
    suspend fun createComment(postId: String, comment: PostComment): PostComment {
        logger.info { "Creating comment on post ID: $postId by user: ${comment.userId}" }
        val request = CreateCommentRequest(postId, comment)
        return post("/posts/$postId/comments", request)
    }

    /**
     * Lists comments for a post.
     *
     * @param postId The ID of the post to get comments for
     * @param page The page number for pagination (1-based)
     * @param pageSize The number of comments per page
     * @return A paginated response containing comments and pagination metadata
     */
    suspend fun listComments(
            postId: String,
            page: Int = 1,
            pageSize: Int = 20
    ): ListCommentsResponse {
        logger.info { "Listing comments for post ID: $postId" }
        return get("/posts/$postId/comments?page=$page&pageSize=$pageSize")
    }

    /**
     * Adds a reaction to a post.
     *
     * @param postId The ID of the post to react to
     * @param userId The ID of the user adding the reaction
     * @param reaction The type of reaction to add
     * @return The created reaction object
     */
    suspend fun addReaction(postId: String, userId: String, reaction: Reaction): PostReaction {
        logger.info { "Adding reaction to post ID: $postId by user: $userId" }
        val request = AddReactionRequest(postId, userId, reaction)
        return post("/posts/$postId/reactions", request)
    }

    /**
     * Removes a reaction from a post.
     *
     * @param postId The ID of the post to remove the reaction from
     * @param userId The ID of the user whose reaction to remove
     */
    suspend fun removeReaction(postId: String, userId: String) {
        logger.info { "Removing reaction from post ID: $postId by user: $userId" }
        delete<Unit>("/posts/$postId/reactions?userId=$userId")
    }
}

// Data classes based on the proto definitions

/** Types of reactions that can be added to posts and comments. */
enum class Reaction {
    /** Default unspecified reaction */
    UNSPECIFIED,

    /** Like reaction */
    LIKE,

    /** Love reaction */
    LOVE,

    /** Laughing reaction */
    HAHA,

    /** Wow reaction */
    WOW,

    /** Sad reaction */
    SAD,

    /** Angry reaction */
    ANGRY
}

/** Types of attachments that can be added to posts. */
enum class AttachmentType {
    /** Default unspecified attachment type */
    UNSPECIFIED,

    /** Image attachment */
    IMAGE,

    /** Video attachment */
    VIDEO
}

/**
 * Represents a reaction to a post.
 *
 * @property postId The ID of the post that was reacted to
 * @property userId The ID of the user who reacted
 * @property reaction The type of reaction
 */
data class PostReaction(val postId: String, val userId: String, val reaction: Reaction)

/**
 * Represents a comment on a post.
 *
 * @property id The unique identifier of the comment (null for new comments)
 * @property userId The ID of the user who created the comment
 * @property content The text content of the comment
 * @property date The creation date as an ISO-8601 timestamp (null for new comments)
 * @property reactions List of reactions to this comment
 */
data class PostComment(
        val id: String? = null,
        val userId: String,
        val content: String,
        val date: String? = null, // ISO-8601 timestamp
        val reactions: List<PostReaction> = emptyList()
)

/**
 * Represents an attachment to a post.
 *
 * @property id The unique identifier of the attachment (null for new attachments)
 * @property postId The ID of the post this attachment belongs to (null for new attachments)
 * @property type The type of attachment (image, video)
 * @property position The position of this attachment in the post (0-based)
 * @property url The URL where the attachment content can be accessed
 */
data class PostAttachment(
        val id: String? = null,
        val postId: String? = null,
        val type: AttachmentType,
        val position: Int,
        val url: String
)

/**
 * Represents a post in the feed.
 *
 * @property id The unique identifier of the post (null for new posts)
 * @property userId The ID of the user who created the post
 * @property content The text content of the post (optional)
 * @property attachments List of attachments (images, videos) for this post
 * @property date The creation date as an ISO-8601 timestamp (null for new posts)
 * @property reactions List of reactions to this post
 * @property comments List of comments on this post
 */
data class Post(
        val id: String? = null,
        val userId: String,
        val content: String? = null,
        val attachments: List<PostAttachment> = emptyList(),
        val date: String? = null, // ISO-8601 timestamp
        val reactions: List<PostReaction> = emptyList(),
        val comments: List<PostComment> = emptyList()
)

/**
 * Request object for creating a new post.
 *
 * @property post The post object to create
 */
data class CreatePostRequest(val post: Post)

/**
 * Response object for listing posts with pagination.
 *
 * @property posts The list of posts for the current page
 * @property total The total number of posts available
 * @property page The current page number
 * @property pageSize The number of posts per page
 */
data class ListPostsResponse(
        val posts: List<Post>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

/**
 * Request object for creating a new comment.
 *
 * @property postId The ID of the post to comment on
 * @property comment The comment object to create
 */
data class CreateCommentRequest(val postId: String, val comment: PostComment)

/**
 * Response object for listing comments with pagination.
 *
 * @property comments The list of comments for the current page
 * @property total The total number of comments available
 * @property page The current page number
 * @property pageSize The number of comments per page
 */
data class ListCommentsResponse(
        val comments: List<PostComment>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

/**
 * Request object for adding a reaction to a post.
 *
 * @property postId The ID of the post to react to
 * @property userId The ID of the user adding the reaction
 * @property reaction The type of reaction to add
 */
data class AddReactionRequest(val postId: String, val userId: String, val reaction: Reaction)
