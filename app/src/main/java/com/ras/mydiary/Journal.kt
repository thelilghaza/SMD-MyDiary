package com.ras.mydiary

data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val mood: String = "",
    val timestamp: Long = 0L,
    val likes: Map<String, Boolean> = mapOf(),
    val isPublic: Boolean = true // Default to public, but can be set to private
)