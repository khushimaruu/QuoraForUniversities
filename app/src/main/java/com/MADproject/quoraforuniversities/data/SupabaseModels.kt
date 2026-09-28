package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ReplyDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("post_id") val postId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreateReplyDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("post_id") val postId: String,
    val content: String
)

@Serializable
data class VoteDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("reply_id") val replyId: String? = null,
    @SerialName("vote_type") val voteType: String = "upvote",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateVoteDto(
    @SerialName("user_id") val userId: String,
    @SerialName("post_id") val postId: String? = null,
    @SerialName("reply_id") val replyId: String? = null,
    @SerialName("vote_type") val voteType: String = "upvote"
)

fun PostDto.toUiModel(
    authorName: String = "Anonymous",
    voteCount: Int = 0,
    answerCount: Int = 0
): QuestionUiModel {
    val tagsList = if (theme.isNotBlank()) {
        theme.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    } else emptyList()

    return QuestionUiModel(
        id = id ?: "",
        title = title,
        body = content,
        authorName = if (isAnonymous) "Anonymous" else authorName,
        isAnonymous = isAnonymous,
        tags = tagsList,
        voteCount = voteCount,
        answerCount = answerCount
    )
}

fun ReplyDto.toUiModel(
    authorName: String = "Anonymous",
    voteCount: Int = 0
): ReplyUiModel {
    return ReplyUiModel(
        id = id ?: "",
        authorName = authorName,
        body = content,
        voteCount = voteCount
    )
}
