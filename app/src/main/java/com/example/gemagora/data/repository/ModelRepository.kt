package com.example.gemagora.data.repository

import com.example.gemagora.data.local.ModelDao
import com.example.gemagora.data.model.LocalModelInfo
import kotlinx.coroutines.flow.Flow

class ModelRepository(private val modelDao: ModelDao) {
    val allModels: Flow<List<LocalModelInfo>> = modelDao.getAllModels()

    suspend fun getModelById(id: String): LocalModelInfo? = modelDao.getModelById(id)

    suspend fun insertModel(model: LocalModelInfo) = modelDao.insertModel(model)

    suspend fun updateModel(model: LocalModelInfo) = modelDao.updateModel(model)

    suspend fun deleteModelById(id: String) = modelDao.deleteModelById(id)
}
