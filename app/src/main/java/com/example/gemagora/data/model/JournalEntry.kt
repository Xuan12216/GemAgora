package com.example.gemagora.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["timestamp"])
    ]
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val entryType: String, // "morning", "evening", "free"
    val dateString: String,
    val aiGuidance: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
