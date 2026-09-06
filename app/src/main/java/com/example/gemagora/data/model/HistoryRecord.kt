package com.example.gemagora.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "history_records",
    indices = [
        Index(value = ["featureType", "timestamp"]),
        Index(value = ["sessionId", "timestamp"])
    ]
)
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = UUID.randomUUID().toString(),
    val featureType: String, // "ROUNDTABLE", "EXPERIMENT", "FALLACY", "SOCRATIC"
    val title: String, // 議題主題 / 思想實驗名稱 / 謬誤論點摘要 / 蘇格拉底對話標題
    val role: String, // "user", "assistant"
    val content: String,
    val metadataJson: String = "", // 額外參數（如出席學派清單、滑桿變數值、反詰強度等）
    val timestamp: Long = System.currentTimeMillis()
)
