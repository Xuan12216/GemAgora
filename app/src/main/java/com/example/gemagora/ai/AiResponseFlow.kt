package com.example.gemagora.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

internal fun Flow<String>.requireAiResponse(): Flow<String> = flow {
    var hasContent = false
    try {
        collect { token ->
            val visible = token.replace(Regex("<\\|[^>]*\\|>|</?think>"), "")
            if (visible.isNotBlank()) hasContent = true
            emit(token)
        }
    } catch (e: CancellationException) {
        throw e
    }
    if (currentCoroutineContext().isActive) {
        check(hasContent) { "模型沒有產生回覆。請重試，或到模型設定增加上下文容量。" }
    }
}

