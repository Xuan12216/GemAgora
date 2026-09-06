package com.example.gemagora.ai

data class FallacyItem(
    val name: String,
    val type: String,
    val quote: String,
    val explanation: String
)

data class FallacyAnalysisResult(
    val premises: List<String> = emptyList(),
    val conclusion: String? = null,
    val fallacies: List<FallacyItem> = emptyList(),
    val evaluation: String? = null,
    val counter: String? = null,
    val isStructured: Boolean = false,
    val rawText: String = ""
)

data class SchoolSpeech(
    val schoolName: String,
    val masterName: String,
    val content: String
)

data class CrossExaminationItem(
    val challenger: String,
    val target: String? = null,
    val content: String
)

data class RoundTableResult(
    val speeches: List<SchoolSpeech> = emptyList(),
    val crossExamination: String? = null,
    val crossExamItems: List<CrossExaminationItem> = emptyList(),
    val synthesis: String? = null,
    val isStructured: Boolean = false,
    val rawText: String = ""
)

data class ExperimentPerspective(
    val id: String,
    val title: String,
    val fullTitle: String,
    val iconEmoji: String,
    val content: String
)

data class ThoughtExperimentResult(
    val intro: String? = null,
    val perspectives: List<ExperimentPerspective> = emptyList(),
    val isStructured: Boolean = false,
    val rawText: String = ""
)

object PhilosophicalParser {

    private val premiseRegex = Regex("<premise>(.*?)(?:</premise>|(?=<premise>)|(?=<conclusion>)|(?=</structure>)|$)", RegexOption.DOT_MATCHES_ALL)
    private val conclusionRegex = Regex("<conclusion>(.*?)(?:</conclusion>|(?=</structure>)|$)", RegexOption.DOT_MATCHES_ALL)
    private val itemTagRegex = Regex("<item\\s+([^>]*)>(.*?)(?:</item>|(?=<item)|(?=</fallacy>)|$)", RegexOption.DOT_MATCHES_ALL)
    private val evaluationRegex = Regex("<evaluation(?:\\s+[^>]*)?>(.*?)(?:</evaluation>|(?=<counter>)|$)", RegexOption.DOT_MATCHES_ALL)
    private val counterRegex = Regex("<counter>(.*?)(?:</counter>|$)", RegexOption.DOT_MATCHES_ALL)

    private val schoolTagRegex = Regex(
        """<school(?:\s*=\s*|\s+)([^>]*)>(.*?)(?:</school>|(?=<school)|(?=<cross[_-]?examination)|(?=<synthesis)|(?=\n\s*#{1,4}\s*第[二三23]輪)|(?=\n\s*(?:\*{3,}|---))|$)""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val crossStartRegex = Regex(
        """(?:<cross[_-]?examination[^>]*>|(?:\*{3,}|---)\s*\n+\s*#{0,4}\s*第[二2]輪|#{1,4}\s*第[二2]輪|#{1,4}\s*(?:邏輯(?:的)?碰撞|針鋒交鋒|各大?派(?:互相)?質[疑詢]|互相質[疑詢]|觀點碰撞|思想交鋒))""",
        RegexOption.IGNORE_CASE
    )
    private val synthesisStartRegex = Regex(
        """(?:<synthesis[^>]*>|(?:\*{3,}|---)\s*\n+\s*#{0,4}\s*第[三3]輪|#{1,4}\s*第[三3]輪|#{1,4}\s*(?:綜合與辯證|辯證合一|正反合辯證|辯證總結|圓桌總結|思想綜合))""",
        RegexOption.IGNORE_CASE
    )
    private val dualDialogRegex = Regex(
        """(?:^|\n)\s*【?(?:\*\*)?([^\n【】*：:]{2,25}?)\s*(?:質疑|VS|對|針對)\s*([^\n【】*：:]{2,25}?)(?:\*\*)?】?\s*[：:]\s*(.*?)(?=(?:\n\s*【?(?:\*\*)?[^\n【】*：:]{2,25}?\s*(?:質疑|VS|對|針對)\s*[^\n【】*：:]{2,25}?(?:\*\*)?】?\s*[：:])|$|\n\s*(?:\*{3,}|---))""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val singleHeaderRegex = Regex(
        """(?:^|\n)\s*【(?:\*\*)?([^\n【】*：:]{2,25}?)(?:\*\*)?】\s*[：:]\s*(.*?)(?=(?:\n\s*【(?:\*\*)?[^\n【】*：:]{2,25}?(?:\*\*)?】\s*[：:])|$|\n\s*(?:\*{3,}|---))""",
        RegexOption.DOT_MATCHES_ALL
    )

    private fun extractAttr(attrs: String, attrName: String): String {
        if (attrName == "name") {
            val malformed = Regex("""^["'](.*?)["']""").find(attrs.trim())?.groupValues?.get(1)?.trim()
            if (!malformed.isNullOrBlank()) return malformed
        }
        return Regex("""$attrName\s*=\s*["'](.*?)["']""").find(attrs)?.groupValues?.get(1)?.trim()
            ?: Regex("""$attrName\s*=\s*([^\s>]+)""").find(attrs)?.groupValues?.get(1)?.trim()
            ?: ""
    }

    fun parseFallacy(rawText: String): FallacyAnalysisResult {
        // Strip thinking tokens before parsing structured sections
        val cleanText = rawText.replace(Regex("<\\|?channel\\|?>[a-zA-Z0-9_]*.*?(?:<channel\\|>|$)", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<think>.*?(?:</think>|$)", RegexOption.DOT_MATCHES_ALL), "")

        val premises = premiseRegex.findAll(cleanText)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .toList()

        val conclusion = conclusionRegex.find(cleanText)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }

        val fallacies = itemTagRegex.findAll(cleanText).mapNotNull { match ->
            val attrs = match.groupValues[1]
            val content = match.groupValues[2].trim()
            val name = extractAttr(attrs, "name")
            val type = extractAttr(attrs, "type")
            val quote = extractAttr(attrs, "quote")

            if (name.isNotBlank() || content.isNotBlank()) {
                FallacyItem(
                    name = name.ifBlank { "邏輯檢驗項" },
                    type = type.ifBlank { "形式/非形式分析" },
                    quote = quote,
                    explanation = content
                )
            } else null
        }.toList()

        val evaluation = evaluationRegex.find(cleanText)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }
        val counter = counterRegex.find(cleanText)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }

        val hasStructure = premises.isNotEmpty() || conclusion != null || fallacies.isNotEmpty() || evaluation != null

        return FallacyAnalysisResult(
            premises = premises,
            conclusion = conclusion,
            fallacies = fallacies,
            evaluation = evaluation,
            counter = counter,
            isStructured = hasStructure,
            rawText = rawText
        )
    }

    fun parseRoundTable(rawText: String): RoundTableResult {
        val cleanText = rawText.replace(Regex("<\\|?channel\\|?>[a-zA-Z0-9_]*.*?(?:<channel\\|>|$)", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<think>.*?(?:</think>|$)", RegexOption.DOT_MATCHES_ALL), "")

        val crossStartMatch = crossStartRegex.find(cleanText)
        val synthesisStartMatch = synthesisStartRegex.find(cleanText)

        val crossStartPos = crossStartMatch?.range?.first
        val synthesisStartPos = synthesisStartMatch?.range?.first

        // 1. Speeches Section
        val speechesText = when {
            crossStartPos != null -> cleanText.substring(0, crossStartPos)
            synthesisStartPos != null -> cleanText.substring(0, synthesisStartPos)
            else -> cleanText
        }

        val speeches = schoolTagRegex.findAll(speechesText).mapNotNull { match ->
            val attrs = match.groupValues[1]
            val content = match.groupValues[2].replace(Regex("<.*?>"), "").trim()
            val name = extractAttr(attrs, "name")
            val master = extractAttr(attrs, "master")

            if (name.isNotBlank() || content.isNotBlank()) {
                SchoolSpeech(
                    schoolName = name.ifBlank { "哲學學派" },
                    masterName = master.ifBlank { "哲學大師" },
                    content = content
                )
            } else null
        }.toList()

        // 2. Cross Examination Section
        val rawCrossExam = if (crossStartMatch != null) {
            val startIdx = crossStartMatch.range.last + 1
            val endIdx = if (synthesisStartPos != null && synthesisStartPos > startIdx) {
                synthesisStartPos
            } else {
                cleanText.length
            }
            cleanText.substring(startIdx, endIdx)
                .replace(Regex("</?cross[_-]?examination[^>]*>", RegexOption.IGNORE_CASE), "")
                .trim()
        } else {
            null
        }

        var crossExamItems = emptyList<CrossExaminationItem>()
        var cleanCrossExam: String? = null

        if (!rawCrossExam.isNullOrBlank()) {
            // Clean leading leftover subtitles (e.g. "：邏輯的碰撞與質疑") and parenthetical hints
            val sanitizedCrossExam = rawCrossExam
                .replace(Regex("""^[:：\s]*(?:[^\n]*?質疑|[^\n]*?辯證|[^\n]*?碰撞|[^\n]*?交鋒|[^\n]*?綜合)?\n+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^\s*[（\(][^）\)]*?[）\)]\s*"""), "")
                .trim()

            // Strategy A: Check for <school ...> sub-tags within cross-examination
            val subSchoolMatches = Regex("<school(?:\\s*=\\s*|\\s+)([^>]*)>(.*?)(?:</school>|(?=<school)|$)", RegexOption.DOT_MATCHES_ALL)
                .findAll(sanitizedCrossExam).toList()

            if (subSchoolMatches.isNotEmpty()) {
                crossExamItems = subSchoolMatches.mapNotNull { m ->
                    val attrs = m.groupValues[1]
                    val rawContent = m.groupValues[2].trim()
                    val challenger = extractAttr(attrs, "name").ifBlank {
                        val master = extractAttr(attrs, "master")
                        if (master.isNotBlank()) master else "哲學代表"
                    }
                    val cleanContent = rawContent.replace(Regex("<.*?>"), "").trim()
                    if (cleanContent.isNotBlank()) {
                        val targetMatch = Regex("""^(?:對|致)?\s*([^\s，,：:「」]{2,15}?)(?:先生|大師|學派|者)?\s*[，,：:「]""").find(cleanContent)
                        val target = targetMatch?.groupValues?.get(1)?.trim()
                        CrossExaminationItem(
                            challenger = challenger,
                            target = target,
                            content = cleanContent
                        )
                    } else null
                }
            } else {
                // Strategy B: Dual-school duels: 【學派 A 質疑 學派 B】：... OR 學派 A 質疑 學派 B：...
                val dialogMatches = dualDialogRegex.findAll(sanitizedCrossExam).toList()

                if (dialogMatches.isNotEmpty()) {
                    crossExamItems = dialogMatches.mapNotNull { m ->
                        val challenger = m.groupValues[1].removePrefix("質疑").removePrefix("發言").trim()
                        val target = m.groupValues[2].removePrefix("被質疑").removePrefix("質疑").trim()
                        val content = m.groupValues[3].replace(Regex("<.*?>"), "").trim()
                        if (challenger.isNotBlank() && content.isNotBlank()) {
                            CrossExaminationItem(
                                challenger = challenger,
                                target = target.ifBlank { null },
                                content = content
                            )
                        } else null
                    }
                } else {
                    // Strategy C: Single header format: 【學派 A】：...
                    val singleHeaderMatches = singleHeaderRegex.findAll(sanitizedCrossExam).toList()

                    if (singleHeaderMatches.isNotEmpty()) {
                        crossExamItems = singleHeaderMatches.mapNotNull { m ->
                            val challenger = m.groupValues[1].trim()
                            val content = m.groupValues[2].replace(Regex("<.*?>"), "").trim()
                            if (challenger.isNotBlank() && content.isNotBlank()) {
                                CrossExaminationItem(
                                    challenger = challenger,
                                    target = null,
                                    content = content
                                )
                            } else null
                        }
                    }
                }
            }

            // Clean fallback crossExam string without ANY stray XML tags
            cleanCrossExam = sanitizedCrossExam.replace(Regex("<.*?>"), "").trim()
                .takeIf { it.isNotBlank() }
        }

        // 3. Synthesis Section
        val synthesis = if (synthesisStartMatch != null) {
            val startIdx = synthesisStartMatch.range.last + 1
            cleanText.substring(startIdx)
                .replace(Regex("</?synthesis[^>]*>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^[:：\s]*(?:[^\n]*?質疑|[^\n]*?辯證|[^\n]*?碰撞|[^\n]*?交鋒|[^\n]*?綜合)?\n+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""^\s*[（\(][^）\)]*?[）\)]\s*"""), "")
                .replace(Regex("<.*?>"), "")
                .trim()
                .takeIf { it.isNotBlank() }
        } else {
            null
        }

        val hasStructure = speeches.isNotEmpty() || cleanCrossExam != null || synthesis != null

        return RoundTableResult(
            speeches = speeches,
            crossExamination = cleanCrossExam,
            crossExamItems = crossExamItems,
            synthesis = synthesis,
            isStructured = hasStructure,
            rawText = rawText
        )
    }

    private val experimentSectionHeaderRegex = Regex(
        """(?:\n|^)\s*(?:#{1,4}\s+|(?:\*{2}|)(?:[一二三四五12345]、|[12345]\.|\(\d\)|（[一二三四五12345]）)\s*|【)\s*([^\n*#]+?(?:效益主義|功利主義|義務論|德行論|德性論|存在主義|思辨結語|思辨結論|結語|總結)[^\n*#]*?)(?:\*{2}|】|）)?\s*(?:\n|$)"""
    )

    fun parseThoughtExperiment(rawText: String): ThoughtExperimentResult {
        val cleanText = rawText
            .replace(Regex("<\\|?channel\\|?>[a-zA-Z0-9_]*.*?(?:<channel\\|>|$)", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<think>.*?(?:</think>|$)", RegexOption.DOT_MATCHES_ALL), "")
            .trim()

        val matches = experimentSectionHeaderRegex.findAll(cleanText).toList()
        if (matches.size < 2) {
            return ThoughtExperimentResult(
                intro = null,
                perspectives = emptyList(),
                isStructured = false,
                rawText = rawText
            )
        }

        val intro = cleanText.substring(0, matches.first().range.first)
            .trim()
            .trim('-', '*', '\n', ' ')
            .takeIf { it.isNotBlank() }

        val perspectives = mutableListOf<ExperimentPerspective>()
        val seenIds = mutableSetOf<String>()

        for (i in matches.indices) {
            val match = matches[i]
            val fullHeader = match.groupValues[1].trim()
            if (fullHeader.length > 50) continue

            val (id, title, icon) = when {
                fullHeader.contains("效益") || fullHeader.contains("功利") || fullHeader.contains("Utilitarian", ignoreCase = true) ->
                    Triple("utilitarianism", "效益主義", "⚖️")
                fullHeader.contains("義務") || fullHeader.contains("康德") || fullHeader.contains("Deontology", ignoreCase = true) ->
                    Triple("deontology", "康德義務論", "📜")
                fullHeader.contains("德行") || fullHeader.contains("德性") || fullHeader.contains("亞里斯多德") || fullHeader.contains("Virtue", ignoreCase = true) ->
                    Triple("virtue_ethics", "德行論", "🏛️")
                fullHeader.contains("存在") || fullHeader.contains("Existential", ignoreCase = true) ->
                    Triple("existentialism", "存在主義", "🌌")
                fullHeader.contains("結語") || fullHeader.contains("結論") || fullHeader.contains("總結") || fullHeader.contains("Synthesis", ignoreCase = true) ->
                    Triple("synthesis", "思辨結語", "💡")
                else ->
                    Triple("perspective_$i", fullHeader.take(8), "🔍")
            }

            if (seenIds.contains(id)) continue
            seenIds.add(id)

            val startIdx = match.range.last + 1
            val endIdx = if (i + 1 < matches.size) matches[i + 1].range.first else cleanText.length
            val content = cleanText.substring(startIdx, endIdx)
                .trim()
                .trim('-', '*', '\n', ' ')
                .trim()

            perspectives.add(
                ExperimentPerspective(
                    id = id,
                    title = title,
                    fullTitle = fullHeader,
                    iconEmoji = icon,
                    content = content
                )
            )
        }

        return ThoughtExperimentResult(
            intro = intro,
            perspectives = perspectives,
            isStructured = perspectives.isNotEmpty(),
            rawText = rawText
        )
    }
}
