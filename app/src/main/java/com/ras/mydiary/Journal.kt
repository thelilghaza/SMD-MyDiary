package com.ras.mydiary

data class JournalEntry(
    val id: String = "",
    val userName: String = "",
    val content: String = "",
    val mood: String = "",
    val timestamp: Long = 0L
)