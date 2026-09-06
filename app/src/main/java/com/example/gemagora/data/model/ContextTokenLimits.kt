package com.example.gemagora.data.model

object ContextTokenLimits {
    const val DEFAULT = 4096
    const val SAFE_MAX = 8192
    val range = 256..8192
    val presets = listOf(512, 1024, 2048, 4096, 8192)
}
