package com.MADproject.quoraforuniversities.utils

object ReplyMentions {
    // Match complete handles, not email addresses or a prefix of another username.
    val pattern = Regex("(?<![\\w@])@[\\p{L}\\p{N}_.-]+")

    fun prepend(text: String, username: String): String {
        val mention = "@${username.trim().removePrefix("@")}"
        if (mention == "@") return text
        val startsWithMention = text.startsWith(mention, ignoreCase = true) &&
            (text.length == mention.length || text[mention.length].isWhitespace())
        return when {
            !startsWithMention -> "$mention $text"
            text.length == mention.length -> "$text "
            else -> text
        }
    }
}
