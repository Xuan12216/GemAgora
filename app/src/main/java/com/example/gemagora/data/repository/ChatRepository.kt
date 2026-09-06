package com.example.gemagora.data.repository

import com.example.gemagora.data.local.ChatDao
import com.example.gemagora.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    fun getMessagesByCategory(category: String): Flow<List<ChatMessage>> = chatDao.getMessagesByCategory(category)
    fun getAllMessages(): Flow<List<ChatMessage>> = chatDao.getAllMessages()
    suspend fun insertMessage(message: ChatMessage): Long = chatDao.insertMessage(message)
    suspend fun clearCategory(category: String) = chatDao.clearCategory(category)
    suspend fun clearAll() = chatDao.clearAll()
}
