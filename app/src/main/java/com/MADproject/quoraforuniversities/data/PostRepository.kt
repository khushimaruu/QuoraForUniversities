package com.MADproject.quoraforuniversities.data

import com.MADproject.quoraforuniversities.ReplyUiModel
import com.MADproject.quoraforuniversities.components.QuestionUiModel

class PostRepository {

    private val questionsList = mutableListOf(
        QuestionUiModel(
            id = "1",
            title = "How do I add a backlog course to my timetable?",
            body = "I failed one course last semester and need to retake it alongside my current courses. Not sure how registration handles this.",
            authorName = "Anonymous",
            isAnonymous = true,
            tags = listOf("CSE", "Year2", "Registration"),
            voteCount = 12,
            answerCount = 3
        ),
        QuestionUiModel(
            id = "2",
            title = "Best cafes near the north campus for group study?",
            body = "Looking for a place with decent wifi and enough seating for 4-5 people, preferably open till late.",
            authorName = "Riya Sharma",
            isAnonymous = false,
            tags = listOf("CampusLife"),
            voteCount = 27,
            answerCount = 9
        )
    )

    private val repliesMap = mutableMapOf(
        "1" to mutableListOf(
            ReplyUiModel(
                id = "r1",
                authorName = "Riya Sharma",
                body = "You should be able to add the backlog course during the registration period. Check with your department coordinator if it doesn't appear.",
                voteCount = 8
            ),
            ReplyUiModel(
                id = "r2",
                authorName = "Anonymous",
                body = "I had the same issue last semester. You need to select the backlog course separately before submitting your timetable.",
                voteCount = 5
            ),
            ReplyUiModel(
                id = "r3",
                authorName = "Arjun Patel",
                body = "Also make sure that the course doesn't clash with your current timetable.",
                voteCount = 3
            )
        )
    )

    suspend fun getPosts(): Result<List<QuestionUiModel>> {
        return Result.success(questionsList.toList())
    }

    suspend fun searchPosts(query: String): Result<List<QuestionUiModel>> {
        if (query.isBlank()) return Result.success(questionsList.toList())
        val filtered = questionsList.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.body.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        return Result.success(filtered)
    }

    suspend fun createPost(
        title: String,
        content: String,
        theme: String,
        isAnonymous: Boolean
    ): Result<Unit> {
        val tagsList = if (theme.isNotBlank()) {
            theme.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        val newQuestion = QuestionUiModel(
            id = System.currentTimeMillis().toString(),
            title = title,
            body = content,
            authorName = if (isAnonymous) "Anonymous" else "Student",
            isAnonymous = isAnonymous,
            tags = tagsList,
            voteCount = 0,
            answerCount = 0
        )
        questionsList.add(0, newQuestion)
        return Result.success(Unit)
    }

    suspend fun votePost(postId: String): Result<Unit> {
        val index = questionsList.indexOfFirst { it.id == postId }
        if (index != -1) {
            val q = questionsList[index]
            questionsList[index] = q.copy(voteCount = q.voteCount + 1)
        }
        return Result.success(Unit)
    }

    suspend fun getReplies(postId: String): Result<List<ReplyUiModel>> {
        val list = repliesMap[postId] ?: emptyList()
        return Result.success(list.toList())
    }

    suspend fun createReply(postId: String, content: String): Result<Unit> {
        val list = repliesMap.getOrPut(postId) { mutableListOf() }
        val newReply = ReplyUiModel(
            id = System.currentTimeMillis().toString(),
            authorName = "Student",
            body = content,
            voteCount = 0
        )
        list.add(newReply)

        val index = questionsList.indexOfFirst { it.id == postId }
        if (index != -1) {
            val q = questionsList[index]
            questionsList[index] = q.copy(answerCount = list.size)
        }
        return Result.success(Unit)
    }
}
