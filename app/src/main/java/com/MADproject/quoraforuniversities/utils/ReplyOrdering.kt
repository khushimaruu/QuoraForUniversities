package com.MADproject.quoraforuniversities.utils

import com.MADproject.quoraforuniversities.ReplyUiModel
import java.time.Instant
import java.time.OffsetDateTime

enum class ReplyOrder { TOP, NEWEST }

/** Sort within each thread, keeping a parent before its children. */
fun orderReplyThreads(replies: List<ReplyUiModel>, order: ReplyOrder): List<Pair<ReplyUiModel, Boolean>> {
    fun time(reply: ReplyUiModel): Instant = runCatching {
        OffsetDateTime.parse(reply.createdAt?.replace(' ', 'T')).toInstant()
    }.getOrDefault(Instant.MIN)
    val comparator = if (order == ReplyOrder.TOP)
        compareByDescending<ReplyUiModel> { it.upvoteCount }.thenByDescending { time(it) }
    else compareByDescending { reply: ReplyUiModel -> time(reply) }
    val ids = replies.map { it.id }.toSet()
    val children = replies.groupBy { it.parentReplyId }.mapValues { it.value.sortedWith(comparator) }
    val visited = mutableSetOf<String>()
    val ordered = mutableListOf<Pair<ReplyUiModel, Boolean>>()
    fun append(root: ReplyUiModel) {
        val pending = mutableListOf(root to false)
        while (pending.isNotEmpty()) {
            val row = pending.removeAt(pending.lastIndex)
            if (!visited.add(row.first.id)) continue
            ordered += row
            children[row.first.id].orEmpty().asReversed().forEach { pending += it to true }
        }
    }
    replies.filter { it.parentReplyId == null || it.parentReplyId !in ids }.sortedWith(comparator).forEach(::append)
    replies.sortedWith(comparator).filter { it.id !in visited }.forEach(::append)
    return ordered
}
