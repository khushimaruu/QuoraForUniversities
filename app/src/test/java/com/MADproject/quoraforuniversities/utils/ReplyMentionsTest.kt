package com.MADproject.quoraforuniversities.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ReplyMentionsTest {
    @Test fun insertsTrailingSpaceAndPreservesDraft() {
        assertEquals("@riya ", ReplyMentions.prepend("", "riya"))
        assertEquals("@riya Thanks!", ReplyMentions.prepend("Thanks!", "riya"))
    }

    @Test fun doesNotDuplicateSameLeadingHandle() {
        assertEquals("@riya Thanks!", ReplyMentions.prepend("@riya Thanks!", "riya"))
        assertEquals("@RIYA Thanks!", ReplyMentions.prepend("@RIYA Thanks!", "riya"))
        assertEquals("@riya ", ReplyMentions.prepend("@riya", "riya"))
        assertEquals("@riya\nThanks!", ReplyMentions.prepend("@riya\nThanks!", "riya"))
    }

    @Test fun distinguishesOtherHandlesAndDoesNotInventAUsername() {
        assertEquals("@riya @riyasharma Thanks", ReplyMentions.prepend("@riyasharma Thanks", "riya"))
        assertEquals("draft", ReplyMentions.prepend("draft", ""))
    }

    @Test fun highlightsHandlesButNotEmailAddresses() {
        val matches = ReplyMentions.pattern.findAll("@riya.sharma Thanks @sam_2! Email sam@example.com").map { it.value }.toList()
        assertEquals(listOf("@riya.sharma", "@sam_2"), matches)
    }
}
