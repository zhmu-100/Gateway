package com.mad.gateway.services

import io.ktor.client.*
import io.ktor.server.application.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/** Client for the Feed service */
class FeedServiceClient(client: HttpClient) : ServiceClient(client), KoinComponent {
    private val application: Application by inject()
    override val baseUrl: String =
            application.environment.config.property("services.feed.url").getString()

    /** Create a new post */
    suspend fun createPost(post: Post): Post {
        logger.info { "Creating post for user: ${post.userId}" }
        val request = CreatePostRequest(post)
        return post("/posts", request)
    }

    /** Get a post by ID */
    suspend fun getPost(id: String): Post {
        logger.info { "Getting post with ID: $id" }
        return get("/posts/$id")
    }

    /** List posts for a specific user */
    suspend fun listUserPosts(
            userId: String,
            viewerId: String,
            page: Int = 1,
            pageSize: Int = 20
    ): ListPostsResponse {
        logger.info { "Listing posts for user ID: $userId, viewed by: $viewerId" }
        return get("/users/$userId/posts?viewerId=$viewerId&page=$page&pageSize=$pageSize")
    }

    /** List posts for the feed (from followed users) */
    suspend fun listPosts(viewerId: String, page: Int = 1, pageSize: Int = 20): ListPostsResponse {
        logger.info { "Listing feed posts for viewer ID: $viewerId" }
        return get("/posts?viewerId=$viewerId&page=$page&pageSize=$pageSize")
    }

    /** Create a comment on a post */
    suspend fun createComment(postId: String, comment: PostComment): PostComment {
        logger.info { "Creating comment on post ID: $postId by user: ${comment.userId}" }
        val request = CreateCommentRequest(postId, comment)
        return post("/posts/$postId/comments", request)
    }

    /** List comments for a post */
    suspend fun listComments(
            postId: String,
            page: Int = 1,
            pageSize: Int = 20
    ): ListCommentsResponse {
        logger.info { "Listing comments for post ID: $postId" }
        return get("/posts/$postId/comments?page=$page&pageSize=$pageSize")
    }

    /** Add a reaction to a post */
    suspend fun addReaction(postId: String, userId: String, reaction: Reaction): PostReaction {
        logger.info { "Adding reaction to post ID: $postId by user: $userId" }
        val request = AddReactionRequest(postId, userId, reaction)
        return post("/posts/$postId/reactions", request)
    }

    /** Remove a reaction from a post */
    suspend fun removeReaction(postId: String, userId: String) {
        logger.info { "Removing reaction from post ID: $postId by user: $userId" }
        delete<Unit>("/posts/$postId/reactions?userId=$userId")
    }
}

// Data classes based on the proto definitions

enum class Reaction {
    UNSPECIFIED,
    LIKE,
    LOVE,
    HAHA,
    WOW,
    SAD,
    ANGRY
}

enum class AttachmentType {
    UNSPECIFIED,
    IMAGE,
    VIDEO
}

data class PostReaction(val postId: String, val userId: String, val reaction: Reaction)

data class PostComment(
        val id: String? = null,
        val userId: String,
        val content: String,
        val date: String? = null, // ISO-8601 timestamp
        val reactions: List<PostReaction> = emptyList()
)

data class PostAttachment(
        val id: String? = null,
        val postId: String? = null,
        val type: AttachmentType,
        val position: Int,
        val url: String
)

data class Post(
        val id: String? = null,
        val userId: String,
        val content: String? = null,
        val attachments: List<PostAttachment> = emptyList(),
        val date: String? = null, // ISO-8601 timestamp
        val reactions: List<PostReaction> = emptyList(),
        val comments: List<PostComment> = emptyList()
)

data class CreatePostRequest(val post: Post)

data class ListPostsResponse(
        val posts: List<Post>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

data class CreateCommentRequest(val postId: String, val comment: PostComment)

data class ListCommentsResponse(
        val comments: List<PostComment>,
        val total: Int,
        val page: Int,
        val pageSize: Int
)

data class AddReactionRequest(val postId: String, val userId: String, val reaction: Reaction)
