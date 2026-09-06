package com.example.gemagora.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["category", "timestamp"])
    ]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "assistant"
    val content: String,
    val category: String = "socratic", // "socratic", "roundtable", "fallacy", "experiment", "general"
    val audioBytes: ByteArray? = null,
    val imageBytes: ByteArray? = null,
    val timestamp: Long = System.currentTimeMillis()
)
