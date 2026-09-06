package com.example.gemagora.data.local

import androidx.room.*
import com.example.gemagora.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE category = :category ORDER BY timestamp ASC")
    fun getMessagesByCategory(category: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE category = :category")
    suspend fun clearCategory(category: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
