package com.example.gemagora.service.tts

import com.example.gemagora.data.model.TtsSettings
import com.example.gemagora.data.model.VoiceGender
import com.example.gemagora.data.model.VoiceInfo

object SchoolVoiceResolver {

    /**
     * Resolves distinctive speech parameters (pitch, speed, and voice) based on the philosophical school
     * and master name to create a dramatic, recognizable auditory persona.
     */
    fun resolveVoiceSettings(
        schoolName: String,
        masterName: String,
        baseSettings: TtsSettings,
        availableVoices: List<VoiceInfo> = emptyList()
    ): TtsSettings {
        val lowerSchool = schoolName.lowercase()
        val lowerMaster = masterName.lowercase()

        val isFemale = lowerMaster.contains("波娃") || lowerMaster.contains("阿斯帕齊婭") ||
                lowerMaster.contains("鄂蘭") || lowerSchool.contains("女性") || lowerSchool.contains("阿斯帕齊婭")

        val targetGender = if (isFemale) VoiceGender.FEMALE else VoiceGender.MALE

        // Pick available voice matching target gender if system has multiple voices
        val matchedVoice = availableVoices.firstOrNull { it.genderHint == targetGender }?.name

        return when {
            // 斯多葛學派 (馬可·奧理略、愛比克泰德、塞內卡)：平靜、沉穩、自省、低沉
            lowerSchool.contains("斯多葛") || lowerMaster.contains("奧理略") ||
                    lowerMaster.contains("愛比克泰德") || lowerMaster.contains("塞內卡") -> {
                baseSettings.copy(
                    pitch = 0.76f,
                    speed = 0.88f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "stoic"
                )
            }

            // 存在主義 (沙特、卡繆)：反叛、荒謬感、青年熱忱、激情辯證
            lowerSchool.contains("存在主義") || lowerMaster.contains("薩特") ||
                    lowerMaster.contains("沙特") || lowerMaster.contains("卡繆") -> {
                baseSettings.copy(
                    pitch = 1.08f,
                    speed = 1.12f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "youth"
                )
            }

            // 虛無主義 (弗里德里希·尼采)：孤傲、銳利、冷峻、力量感
            lowerSchool.contains("虛無") || lowerMaster.contains("尼采") -> {
                baseSettings.copy(
                    pitch = 0.84f,
                    speed = 1.05f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "custom"
                )
            }

            // 東方道家 (老子、莊子)：悠遠、空靈、順應自然、極度舒緩
            lowerSchool.contains("道家") || lowerMaster.contains("老子") || lowerMaster.contains("莊子") -> {
                baseSettings.copy(
                    pitch = 0.90f,
                    speed = 0.78f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "custom"
                )
            }

            // 儒家思想 (孔子、孟子)：厚重、端莊、仁者之風、長者威儀
            lowerSchool.contains("儒家") || lowerMaster.contains("孔子") || lowerMaster.contains("孟子") -> {
                baseSettings.copy(
                    pitch = 0.80f,
                    speed = 0.95f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "custom"
                )
            }

            // 佛家思想 (釋迦牟尼)：慈悲、安寧、安定心神、祥和緩慢
            lowerSchool.contains("佛") || lowerMaster.contains("釋迦") -> {
                baseSettings.copy(
                    pitch = 0.88f,
                    speed = 0.82f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "custom"
                )
            }

            // 效益/功利主義 (邊沁、密爾)：客觀、精密、條理清晰、冷靜計算
            lowerSchool.contains("效益") || lowerSchool.contains("功利") ||
                    lowerMaster.contains("邊沁") || lowerMaster.contains("密爾") -> {
                baseSettings.copy(
                    pitch = 1.00f,
                    speed = 1.02f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "system"
                )
            }

            // 德行倫理學 / 柏拉圖 / 亞里斯多德
            lowerSchool.contains("德行") || lowerMaster.contains("亞里斯多德") || lowerMaster.contains("柏拉圖") -> {
                baseSettings.copy(
                    pitch = 0.88f,
                    speed = 0.98f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "custom"
                )
            }

            // 女性思想家
            isFemale -> {
                baseSettings.copy(
                    pitch = 1.15f,
                    speed = 1.00f,
                    voiceName = matchedVoice ?: baseSettings.voiceName,
                    presetId = "aspasia"
                )
            }

            // 預設 (蘇格拉底產婆引導音色)
            else -> {
                baseSettings.copy(
                    pitch = 0.82f,
                    speed = 0.96f,
                    presetId = "socrates"
                )
            }
        }
    }
}
