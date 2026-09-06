package com.example.gemagora.data.repository

import com.example.gemagora.data.local.HistoryDao
import com.example.gemagora.data.model.HistoryRecord
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {

    fun getSessionsByFeature(featureType: String): Flow<List<HistoryRecord>> {
        return historyDao.getSessionsByFeature(featureType)
    }

    fun getRecordsBySession(sessionId: String): Flow<List<HistoryRecord>> {
        return historyDao.getRecordsBySession(sessionId)
    }

    suspend fun getRecordsBySessionSync(sessionId: String): List<HistoryRecord> {
        return historyDao.getRecordsBySessionSync(sessionId)
    }

    suspend fun insertRecord(record: HistoryRecord): Long {
        return historyDao.insertRecord(record)
    }

    suspend fun saveInitialGeneration(
        sessionId: String,
        featureType: String,
        title: String,
        promptInput: String,
        generatedOutput: String,
        metadataJson: String = ""
    ) {
        val now = System.currentTimeMillis()
        if (promptInput.isNotBlank()) {
            historyDao.insertRecord(
                HistoryRecord(
                    sessionId = sessionId,
                    featureType = featureType,
                    title = title,
                    role = "user",
                    content = promptInput,
                    metadataJson = metadataJson,
                    timestamp = now
                )
            )
        }
        historyDao.insertRecord(
            HistoryRecord(
                sessionId = sessionId,
                featureType = featureType,
                title = title,
                role = "assistant",
                content = generatedOutput,
                metadataJson = metadataJson,
                timestamp = now + 1
            )
        )
    }

    suspend fun saveFollowUpTurn(
        sessionId: String,
        featureType: String,
        title: String,
        userQuestion: String,
        assistantReply: String,
        metadataJson: String = ""
    ) {
        val now = System.currentTimeMillis()
        if (userQuestion.isNotBlank()) {
            historyDao.insertRecord(
                HistoryRecord(
                    sessionId = sessionId,
                    featureType = featureType,
                    title = title,
                    role = "user",
                    content = userQuestion,
                    metadataJson = metadataJson,
                    timestamp = now
                )
            )
        }
        if (assistantReply.isNotBlank()) {
            historyDao.insertRecord(
                HistoryRecord(
                    sessionId = sessionId,
                    featureType = featureType,
                    title = title,
                    role = "assistant",
                    content = assistantReply,
                    metadataJson = metadataJson,
                    timestamp = now + 1
                )
            )
        }
    }

    suspend fun deleteSession(sessionId: String) {
        historyDao.deleteSession(sessionId)
    }

    suspend fun clearFeatureHistory(featureType: String) {
        historyDao.clearFeatureHistory(featureType)
    }

    suspend fun deleteRecord(id: Long) {
        historyDao.deleteRecord(id)
    }
}
