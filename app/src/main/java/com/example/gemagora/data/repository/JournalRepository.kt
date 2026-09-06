package com.example.gemagora.data.repository

import com.example.gemagora.data.local.JournalDao
import com.example.gemagora.data.model.JournalEntry
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()
    suspend fun getEntryById(id: Int): JournalEntry? = journalDao.getEntryById(id)
    suspend fun insertEntry(entry: JournalEntry): Long = journalDao.insertEntry(entry)
    suspend fun updateEntry(entry: JournalEntry) = journalDao.updateEntry(entry)
    suspend fun deleteEntry(entry: JournalEntry) = journalDao.deleteEntry(entry)
}
