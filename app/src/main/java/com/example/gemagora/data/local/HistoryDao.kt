package com.example.gemagora.data.local

import androidx.room.*
import com.example.gemagora.data.model.HistoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("""
        SELECT r.* FROM history_records r
        INNER JOIN (
            SELECT MAX(id) as maxId
            FROM history_records
            WHERE featureType = :featureType
            GROUP BY sessionId
        ) latest ON r.id = latest.maxId
        ORDER BY r.timestamp DESC
    """)
    fun getSessionsByFeature(featureType: String): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getRecordsBySession(sessionId: String): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getRecordsBySessionSync(sessionId: String): List<HistoryRecord>

    @Query("SELECT * FROM history_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): HistoryRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HistoryRecord): Long

    @Query("DELETE FROM history_records WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM history_records WHERE featureType = :featureType")
    suspend fun clearFeatureHistory(featureType: String)

    @Query("DELETE FROM history_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)
}
