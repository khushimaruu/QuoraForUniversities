package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi

@Serializable
data class PostDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val title: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val theme: String = "",
    @SerialName("is_anonymous") val isAnonymous: Boolean = false
)

@Serializable
data class PostWithProfileDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val title: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val theme: String = "",
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    val profiles: ProfileDto? = null
)

@Serializable
data class CreatePostDto(
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val content: String,
    val theme: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false
)

@Serializable
data class ProfileDto(
    val id: String,
    val username: String? = null,
    val name: String? = null,
    val gender: String? = null,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ReplyDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("post_id") val postId: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("parent_reply_id") val parentReplyId: String? = null
)

@Serializable
data class ReplyWithProfileDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("post_id") val postId: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val profiles: ProfileDto? = null,
    @SerialName("parent_reply_id") val parentReplyId: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateReplyDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("post_id") val postId: String,
    val content: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("parent_reply_id") val parentReplyId: String? = null
)

@Serializable
data class VoteDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("reply_id") val replyId: String? = null,
    @SerialName("vote_type") val voteType: Int,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class VoteMinimalDto(
    @SerialName("id") val id: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("vote_type") val voteType: Int,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
data class ReplyMinimalDto(
    @SerialName("post_id") val postId: String = ""
)

@Serializable
data class CreateVoteDto(
    @SerialName("user_id") val userId: String,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("reply_id") val replyId: String? = null,
    @SerialName("vote_type") val voteType: Int
)

@Serializable
data class BookmarkDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateBookmarkDto(
    @SerialName("user_id") val userId: String,
    @SerialName("post_id") val postId: String
)

@Serializable
data class ReportDto(
    val id: String? = null,
    @SerialName("reporter_id") val reporterId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("reply_id") val replyId: String? = null,
    val reason: String = "",
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateReportDto(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("reply_id") val replyId: String? = null,
    val reason: String,
    val status: String = "pending"
)

fun PostDto.toUiModel(
    authorName: String = "Anonymous",
    voteCount: Int = 0,
    answerCount: Int = 0,
    upvoteCount: Int = 0,
    downvoteCount: Int = 0,
    currentUserVote: Int = 0,
    currentUserId: String? = null
): QuestionUiModel {
    val tagsList = if (theme.isNotBlank()) {
        theme.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    } else emptyList()

    return QuestionUiModel(
        id = id ?: "",
        authorId = if (isAnonymous) "" else userId.orEmpty(),
        isOwnContent = currentUserId != null && currentUserId == userId,
        title = title,
        body = content,
        authorName = if (isAnonymous) "Anonymous" else authorName,
        isAnonymous = isAnonymous,
        tags = tagsList,
        voteCount = voteCount,
        answerCount = answerCount,
        upvoteCount = upvoteCount,
        downvoteCount = downvoteCount,
        currentUserVote = currentUserVote,
        createdAt = createdAt
    )
}

fun PostWithProfileDto.toUiModel(
    voteCount: Int = 0,
    answerCount: Int = 0,
    upvoteCount: Int = 0,
    downvoteCount: Int = 0,
    currentUserVote: Int = 0,
    currentUserId: String? = null
): QuestionUiModel {
    val tagsList = if (theme.isNotBlank()) {
        theme.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    } else emptyList()

    val resolvedAuthorName = if (isAnonymous) {
        "Anonymous"
    } else {
        profiles?.name ?: profiles?.username ?: "Student"
    }

    return QuestionUiModel(
        id = id ?: "",
        authorId = if (isAnonymous) "" else userId.orEmpty(),
        isOwnContent = currentUserId != null && currentUserId == userId,
        title = title,
        body = content,
        authorName = resolvedAuthorName,
        isAnonymous = isAnonymous,
        tags = tagsList,
        voteCount = voteCount,
        answerCount = answerCount,
        upvoteCount = upvoteCount,
        downvoteCount = downvoteCount,
        currentUserVote = currentUserVote,
        createdAt = createdAt,
        authorAvatar = if (isAnonymous) null else profiles?.avatarUrl
    )
}

fun ReplyDto.toUiModel(
    authorName: String = "Anonymous",
    voteCount: Int = 0,
    authorUsername: String? = null,
    currentUserId: String? = null
): ReplyUiModel {
    return ReplyUiModel(
        id = id ?: "",
        authorId = userId.orEmpty(),
        isOwnContent = currentUserId != null && currentUserId == userId,
        authorName = authorName,
        body = content,
        voteCount = voteCount,
        createdAt = createdAt,
        authorUsername = authorUsername,
        parentReplyId = parentReplyId
    )
}

fun ReplyWithProfileDto.toUiModel(
    voteCount: Int = 0,
    upvoteCount: Int = 0,
    downvoteCount: Int = 0,
    currentUserVote: Int = 0,
    currentUserId: String? = null
): ReplyUiModel {
    val resolvedAuthorName = profiles?.name ?: profiles?.username ?: "Student"
    return ReplyUiModel(
        id = id ?: "",
        authorId = userId.orEmpty(),
        isOwnContent = currentUserId != null && currentUserId == userId,
        authorName = resolvedAuthorName,
        body = content,
        voteCount = voteCount,
        createdAt = createdAt,
        authorUsername = profiles?.username?.takeIf { it.isNotBlank() },
        parentReplyId = parentReplyId,
        upvoteCount = upvoteCount,
        downvoteCount = downvoteCount,
        currentUserVote = currentUserVote,
        authorAvatar = profiles?.avatarUrl
    )
}
