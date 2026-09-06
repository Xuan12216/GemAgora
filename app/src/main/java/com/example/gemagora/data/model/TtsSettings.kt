package com.example.gemagora.data.model

enum class VoiceGender {
    MALE,
    FEMALE,
    NEUTRAL
}

data class TtsPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val genderHint: VoiceGender,
    val pitch: Float,
    val speed: Float
)

data class VoiceInfo(
    val name: String,
    val displayName: String,
    val locale: String,
    val isNetworkRequired: Boolean,
    val genderHint: VoiceGender
)

data class TtsSettings(
    val enabled: Boolean = true,
    val autoSpeak: Boolean = false,
    val voiceName: String? = null,
    val pitch: Float = 0.82f,
    val speed: Float = 0.95f,
    val presetId: String = "socrates"
) {
    companion object {
        val PRESETS = listOf(
            TtsPreset(
                id = "socrates",
                name = "蘇格拉底",
                subtitle = "沉穩深思 · 男聲",
                description = "渾厚沉穩的低頻音調與深思節奏，適合步步引導的詰問產婆術。",
                genderHint = VoiceGender.MALE,
                pitch = 0.82f,
                speed = 0.95f
            ),
            TtsPreset(
                id = "aspasia",
                name = "阿斯帕齊婭",
                subtitle = "睿智溫和 · 女聲",
                description = "清澈優雅的女性音色與流暢節奏，體現雅典學堂的靈動智慧與修辭之美。",
                genderHint = VoiceGender.FEMALE,
                pitch = 1.15f,
                speed = 1.00f
            ),
            TtsPreset(
                id = "stoic",
                name = "斯多葛哲人",
                subtitle = "淡然平靜 · 男聲",
                description = "平緩淡然的低語速與低沉音調，如同沉思錄中的夜間心靈對話。",
                genderHint = VoiceGender.MALE,
                pitch = 0.76f,
                speed = 0.88f
            ),
            TtsPreset(
                id = "youth",
                name = "雅典青年",
                subtitle = "熱忱朝氣 · 青年",
                description = "略微偏高的音調與明快的節奏，充滿探索真理與哲學對辯的朝氣活力。",
                genderHint = VoiceGender.NEUTRAL,
                pitch = 1.05f,
                speed = 1.10f
            ),
            TtsPreset(
                id = "system",
                name = "系統標準",
                subtitle = "原生基線",
                description = "使用 Android 系統當前預設發音人與 1.0x 基準音調及語速。",
                genderHint = VoiceGender.NEUTRAL,
                pitch = 1.00f,
                speed = 1.00f
            ),
            TtsPreset(
                id = "custom",
                name = "自訂聲線",
                subtitle = "自由調諧",
                description = "由您自定義選擇系統發聲庫、音調 (Pitch) 與語速 (Speed)。",
                genderHint = VoiceGender.NEUTRAL,
                pitch = 1.00f,
                speed = 1.00f
            )
        )

        fun findPreset(id: String): TtsPreset = PRESETS.firstOrNull { it.id == id } ?: PRESETS.first()
    }
}
