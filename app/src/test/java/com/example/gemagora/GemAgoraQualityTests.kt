package com.example.gemagora

import com.example.gemagora.ai.PhilosophicalParser
import com.example.gemagora.data.model.ContextTokenLimits
import com.example.gemagora.theme.ColorUtils
import com.example.gemagora.theme.customColorScheme
import org.junit.Assert.*
import org.junit.Test

class GemAgoraQualityTests {

    @Test
    fun testContextTokenLimits() {
        assertEquals(4096, ContextTokenLimits.DEFAULT)
        assertEquals(8192, ContextTokenLimits.SAFE_MAX)
        assertTrue(ContextTokenLimits.range.contains(4096))
        assertTrue(ContextTokenLimits.presets.contains(4096))
        assertTrue(ContextTokenLimits.presets.contains(2048))
        assertTrue(ContextTokenLimits.presets.contains(8192))
    }

    @Test
    fun testColorUtilsValidation() {
        assertTrue(ColorUtils.isValidHex("#C59B27"))
        assertTrue(ColorUtils.isValidHex("C59B27"))
        assertTrue(ColorUtils.isValidHex("#000000"))
        assertTrue(ColorUtils.isValidHex("FFFFFF"))
        assertFalse(ColorUtils.isValidHex("#12345")) // too short
        assertFalse(ColorUtils.isValidHex("#ZZZZZZ")) // invalid hex chars
    }

    @Test
    fun testColorUtilsHslConversion() {
        val result = ColorUtils.hexToHsl("#C59B27")
        assertNotNull(result)
        result?.let { (hue, sat, light) ->
            assertTrue("Hue should be in 40..50, got $hue", hue in 40..50)
            assertTrue("Saturation should be in 0.5..0.8, got $sat", sat in 0.5f..0.8f)
            assertTrue("Lightness should be in 0.4..0.6, got $light", light in 0.4f..0.6f)
        }
    }

    @Test
    fun testPhilosophicalParserRoundTable() {
        val raw = """
            <think>This is internal chain of thought</think>
            <roundtable>
            <school name="斯多葛學派" master="馬可·奧理略">專注於自身能控制之事，接受無常。</school>
            <school name="存在主義" master="薩特">存在先於本質，人是被判定為自由的。</school>
            <cross_examination>斯多葛的克制是否會陷入消極順從？</cross_examination>
            <synthesis>融合自主責任與內心定力，在行動中創造自我。</synthesis>
            </roundtable>
        """.trimIndent()

        val parsed = PhilosophicalParser.parseRoundTable(raw)
        assertTrue(parsed.isStructured)
        assertEquals(2, parsed.speeches.size)
        assertEquals("斯多葛學派", parsed.speeches[0].schoolName)
        assertEquals("馬可·奧理略", parsed.speeches[0].masterName)
        assertTrue(parsed.speeches[0].content.contains("專注於自身能控制之事"))
        assertEquals("存在主義", parsed.speeches[1].schoolName)
        assertEquals("薩特", parsed.speeches[1].masterName)
        assertNotNull(parsed.crossExamination)
        assertTrue(parsed.crossExamination?.contains("消極順從") == true)
        assertNotNull(parsed.synthesis)
        assertTrue(parsed.synthesis?.contains("自主責任") == true)
    }

    @Test
    fun testPhilosophicalParserRoundTableWithSubSchoolTagsAndMalformedTags() {
        val raw = """
            <roundtable>
            <school name="斯多葛學派" master="馬可·奧理略">接受無常，安頓內心。</school>
            <school name="存在主義" master="薩特">自由選擇，承擔責任。</school>
            <cross_examination>
            <school name="斯多葛學派" master="馬可·奧理略">
            尼采先生，您將「意志」置於至高地位，是否會忽視了命運？
            </school>
            <school="存在主義 (薩特)" master="薩特">
            斯多葛先生，您的「理性接受」是否等同於一種順從？
            </school>
            </cross_examination>
            <synthesis master="論壇主持人">超越對立，融合主體自由與內在定力。</synthesis>
            </roundtable>
        """.trimIndent()

        val parsed = PhilosophicalParser.parseRoundTable(raw)
        assertTrue(parsed.isStructured)
        assertEquals(2, parsed.speeches.size)
        assertEquals(2, parsed.crossExamItems.size)

        // Item 1
        assertEquals("斯多葛學派", parsed.crossExamItems[0].challenger)
        assertEquals("尼采", parsed.crossExamItems[0].target)
        assertTrue(parsed.crossExamItems[0].content.contains("尼采先生"))
        assertFalse(parsed.crossExamItems[0].content.contains("<school"))

        // Item 2 (Malformed attribute: school="存在主義 (薩特)")
        assertEquals("存在主義 (薩特)", parsed.crossExamItems[1].challenger)
        assertEquals("斯多葛", parsed.crossExamItems[1].target)
        assertTrue(parsed.crossExamItems[1].content.contains("斯多葛先生"))
        assertFalse(parsed.crossExamItems[1].content.contains("<school"))

        // Fallback string must be sanitized as well
        assertNotNull(parsed.crossExamination)
        assertFalse(parsed.crossExamination!!.contains("<school"))
        assertFalse(parsed.crossExamination!!.contains("</school>"))
    }

    @Test
    fun testPhilosophicalParserRoundTableWithDialogFormat() {
        val raw = """
            <school name="東方道家" master="老子">道法自然。</school>
            <school name="效益主義" master="邊沁">最大化快樂。</school>
            <cross_examination>
            【東方道家 質疑 效益主義】：若一切皆能量化，生命之妙何在？
            【效益主義 質疑 東方道家】：若一切皆順應自然，如何救濟疾苦？
            </cross_examination>
            <synthesis>既有利他實效，亦抱自然超然。</synthesis>
        """.trimIndent()

        val parsed = PhilosophicalParser.parseRoundTable(raw)
        assertTrue(parsed.isStructured)
        assertEquals(2, parsed.speeches.size)
        assertEquals(2, parsed.crossExamItems.size)
        assertEquals("東方道家", parsed.crossExamItems[0].challenger)
        assertEquals("效益主義", parsed.crossExamItems[0].target)
        assertEquals("若一切皆能量化，生命之妙何在？", parsed.crossExamItems[0].content)
        assertEquals("效益主義", parsed.crossExamItems[1].challenger)
        assertEquals("東方道家", parsed.crossExamItems[1].target)
        assertEquals("若一切皆順應自然，如何救濟疾苦？", parsed.crossExamItems[1].content)
    }

    @Test
    fun testPhilosophicalParserRoundTableMissingCrossExamTagAndMarkdownHeaders() {
        val raw = """
            <school name="斯多葛學派 (馬可·奧理略 / 愛比克泰德)" master="馬可·奧理略">
            愛，應是基於理性對善惡的判斷與自然秩序的順應。
            </school>
            <school name="存在主義 (薩特 / 卡繆)" master="薩特 / 卡繆">
            愛是個體在無意義的荒謬世界中，對「他者」的徹底責任。
            </school>
            <school name="東方道家思想 (老子 / 莊子)" master="老子 / 莊子">
            愛（或「道」的體現）的本質，是「無為」與「自然」。
            </school>
            <school name="虛無主義與超人意志 (尼采)" master="弗里德里希·尼采">
            愛，是生命超越虛無的意志的彰顯。
            </school>
            <school name="效益主義 (邊沁 / 密爾)" master="傑瑞米·邊沁">
            愛的價值，應從其對社會整體幸福感的增進來衡量。
            </school>
            <school name="佛家緣起性空 (釋迦牟尼)" master="釋迦牟尼">
            愛（慈悲心）是一種深刻的「緣起」關係的體現，是消除無明所產生的痛苦的解脫之路。

            ***

            ### 第二輪：邏輯的碰撞與質疑

            （此刻，我們將從對立的立場中，挑出對手，尋找其論證的邊界。）

            斯多葛學派 質疑 存在主義：你所強調的「為他者創造意義」的行動，是否只是一種在虛無面前的「逃避」？

            存在主義 質疑 斯多葛學派：你將愛限定為「理性與秩序的調和」，這是否將情感的深度徹底壓制？

            東方道家 質疑 效益主義：你將愛必須「最大化社會快樂」的衡量標準，是否將生命降低為單純的效用計算？

            虛無主義與超人意志 質疑 佛家緣起性空：你將愛視為「無執著的接納」，這是否等同於對自身潛能的消極化？

            佛家 質疑 斯多葛派：你將愛定義為「德性」，這是否將其從真正的「空性」抽離？

            效益主義 質疑 存在主義：你對「賭注」的描述，聽起來極度不理智，它缺乏可量化的預測性。

            ***

            ### 第三輪：綜合與辯證

            （現在，我們必須從這場激烈的碰撞中，提煉出超越二元對立的結構。）

            各位，我們觀察到，愛並非是單一的屬性，它更像一條河流。
        """.trimIndent()

        val parsed = PhilosophicalParser.parseRoundTable(raw)
        assertTrue(parsed.isStructured)
        assertEquals(6, parsed.speeches.size)
        assertEquals("佛家緣起性空 (釋迦牟尼)", parsed.speeches[5].schoolName)
        assertEquals("釋迦牟尼", parsed.speeches[5].masterName)
        // Ensure speech content is NOT polluted by Round 2 or 3
        assertFalse(parsed.speeches[5].content.contains("第二輪"))
        assertFalse(parsed.speeches[5].content.contains("質疑 存在主義"))
        assertTrue(parsed.speeches[5].content.contains("消除無明所產生的痛苦的解脫之路"))

        // Cross examination items
        assertEquals(6, parsed.crossExamItems.size)
        assertEquals("斯多葛學派", parsed.crossExamItems[0].challenger)
        assertEquals("存在主義", parsed.crossExamItems[0].target)
        assertTrue(parsed.crossExamItems[0].content.contains("是否只是一種在虛無面前的「逃避」"))

        assertEquals("存在主義", parsed.crossExamItems[1].challenger)
        assertEquals("斯多葛學派", parsed.crossExamItems[1].target)

        assertEquals("東方道家", parsed.crossExamItems[2].challenger)
        assertEquals("效益主義", parsed.crossExamItems[2].target)

        assertEquals("虛無主義與超人意志", parsed.crossExamItems[3].challenger)
        assertEquals("佛家緣起性空", parsed.crossExamItems[3].target)

        assertEquals("佛家", parsed.crossExamItems[4].challenger)
        assertEquals("斯多葛派", parsed.crossExamItems[4].target)

        assertEquals("效益主義", parsed.crossExamItems[5].challenger)
        assertEquals("存在主義", parsed.crossExamItems[5].target)

        // Synthesis
        assertNotNull(parsed.synthesis)
        assertFalse(parsed.synthesis!!.contains("第三輪"))
        assertFalse(parsed.synthesis!!.contains("提煉出超越二元對立"))
        assertTrue(parsed.synthesis!!.startsWith("各位，我們觀察到，愛並非是單一的屬性"))
    }

    @Test
    fun testCustomColorSchemeContract() {
        val hues = listOf(0, 45, 120, 215, 280, 359)
        for (h in hues) {
            val light = customColorScheme(hue = h, dark = false)
            assertNotNull(light)
            assertNotNull(light.surface)
            assertNotNull(light.surfaceContainer)
            assertNotNull(light.primaryContainer)
            assertNotNull(light.onPrimaryContainer)

            val dark = customColorScheme(hue = h, dark = true)
            assertNotNull(dark)
            assertNotNull(dark.surface)
            assertNotNull(dark.surfaceContainer)
        }
    }

    @Test
    fun testPhilosophicalParserThoughtExperiment() {
        val sampleOutput = """
            <think>Thinking about trolley problem</think>
            吾友，你帶來了一個極為引人深思的思想實驗——「電車難題」。
            這是一個關於道德本質的深刻鏡子。

            ---

            ### 一、效益主義 / 功利主義 (Utilitarianism) 的推演
            **核心關懷：** 追求整體最大化幸福或最小化痛苦。
            **推演分析：** 從純粹的計算角度來看，應當追求最小損失。

            ---

            ### 二、康德義務論 (Deontology) 的推演
            **核心關懷：** 行為本身的道德性，而非其結果。
            **推演分析：** 人不可僅被當作達成目的的手段。

            ---

            ### 三、亞里斯多德德行論 (Virtue Ethics) 的推演
            **核心關懷：** 探討行為者本身的品格與實踐智慧。

            ---

            ### 四、存在主義與自我反思 (Existentialist Reflection) 的推演
            **核心關懷：** 存在先於本質，直面自由與沉重責任。

            ---

            ### 五、思辨結語
            在極端情境下，真正的道德勇氣在於自覺地承擔行動的重量。
        """.trimIndent()

        val result = PhilosophicalParser.parseThoughtExperiment(sampleOutput)
        assertTrue(result.isStructured)
        assertNotNull(result.intro)
        assertTrue(result.intro!!.contains("極為引人深思的思想實驗"))
        assertEquals(5, result.perspectives.size)

        assertEquals("utilitarianism", result.perspectives[0].id)
        assertEquals("效益主義", result.perspectives[0].title)
        assertEquals("⚖️", result.perspectives[0].iconEmoji)
        assertTrue(result.perspectives[0].content.contains("追求整體最大化幸福"))

        assertEquals("deontology", result.perspectives[1].id)
        assertEquals("康德義務論", result.perspectives[1].title)
        assertEquals("📜", result.perspectives[1].iconEmoji)

        assertEquals("virtue_ethics", result.perspectives[2].id)
        assertEquals("德行論", result.perspectives[2].title)
        assertEquals("🏛️", result.perspectives[2].iconEmoji)

        assertEquals("existentialism", result.perspectives[3].id)
        assertEquals("存在主義", result.perspectives[3].title)
        assertEquals("🌌", result.perspectives[3].iconEmoji)

        assertEquals("synthesis", result.perspectives[4].id)
        assertEquals("思辨結語", result.perspectives[4].title)
        assertEquals("💡", result.perspectives[4].iconEmoji)
    }

    @Test
    fun testPhilosophicalParserThoughtExperimentWithRealModelOutput() {
        val sample = """
            吾友，你提出了「電車難題」，一個極其精妙且充滿張力的思想實驗。
            你要求我從效益主義、康德義務論、亞里斯多德德行論，乃至於存在主義等多元的倫理學視角，對此情境進行跨倫理學派的剖析。
            
            現在，我們依你所列的五個維度，開始進行我們的思辨之旅。
            
            ---
            
            ### 一、【效益主義 / 功利主義 (Utilitarianism)】的推演
            效益主義的核心，是追求「最大化整體幸福」。
            
            ### 二、【康德義務論 (Deontology)】的推演
            康德義務論主張不可將人視為手段。
            
            ### 三、【亞里斯多德德行論 (Virtue Ethics)】的推演
            德行論強調實踐智慧。
            
            ### 四、【存在主義 (Existentialism)】的推演
            存在主義強調直面選擇。
            
            ### 五、【思辨結語與關鍵追問】
            **我的追問**：當功利主義的計算邏輯要求你做出決定時？
        """.trimIndent()

        val result = PhilosophicalParser.parseThoughtExperiment(sample)
        assertTrue(result.isStructured)
        assertEquals(5, result.perspectives.size)
        assertEquals("utilitarianism", result.perspectives[0].id)
        assertEquals("deontology", result.perspectives[1].id)
        assertEquals("virtue_ethics", result.perspectives[2].id)
        assertEquals("existentialism", result.perspectives[3].id)
        assertEquals("synthesis", result.perspectives[4].id)
        assertTrue(result.intro!!.contains("你要求我從效益主義"))
    }

    @Test
    fun testPromptBuilderThoughtExperiment() {
        val prompt = com.example.gemagora.ai.PromptBuilder.buildThoughtExperimentPrompt(
            experimentTitle = "電車難題 (Trolley Problem)",
            premise = "失控列車逼近分岔路口",
            variablesSummary = "- 主軌道人數: 5\n- 備用軌道人數: 1"
        )
        assertTrue(prompt.contains("電車難題"))
        assertTrue(prompt.contains("主軌道人數: 5"))
        assertTrue(prompt.contains("效益主義"))
        assertTrue(prompt.contains("康德義務論"))
        assertTrue(prompt.contains("亞里斯多德德行論"))
        assertTrue(prompt.contains("存在主義"))
    }

    @Test
    fun testPromptBuilderRoundTable() {
        val schools = listOf("斯多葛學派", "存在主義", "東方道家思想")
        val prompt = com.example.gemagora.ai.PromptBuilder.buildRoundTablePrompt(
            topic = "如何面對人生焦慮？",
            selectedSchools = schools
        )
        assertTrue(prompt.contains("如何面對人生焦慮？"))
        assertTrue(prompt.contains("斯多葛學派"))
        assertTrue(prompt.contains("存在主義"))
        assertTrue(prompt.contains("東方道家思想"))
        assertTrue(prompt.contains("<school"))
        assertTrue(prompt.contains("<cross_examination>"))
        assertTrue(prompt.contains("<synthesis"))
    }

    @Test
    fun testFontSizeScaleAndAppearanceSettings() {
        assertEquals(com.example.gemagora.data.model.FontSizeScale.NORMAL, com.example.gemagora.data.model.FontSizeScale.DEFAULT)
        assertEquals(com.example.gemagora.data.model.FontSizeScale.SMALL, com.example.gemagora.data.model.FontSizeScale.fromScale(0.85f))
        assertEquals(com.example.gemagora.data.model.FontSizeScale.NORMAL, com.example.gemagora.data.model.FontSizeScale.fromScale(1.0f))
        assertEquals(com.example.gemagora.data.model.FontSizeScale.LARGE, com.example.gemagora.data.model.FontSizeScale.fromScale(1.15f))
        assertEquals(com.example.gemagora.data.model.FontSizeScale.EXTRA_LARGE, com.example.gemagora.data.model.FontSizeScale.fromScale(1.30f))
        assertEquals(com.example.gemagora.data.model.FontSizeScale.NORMAL, com.example.gemagora.data.model.FontSizeScale.fromScale(null))

        // Closest match
        assertEquals(com.example.gemagora.data.model.FontSizeScale.SMALL, com.example.gemagora.data.model.FontSizeScale.fromScale(0.88f))
        assertEquals(com.example.gemagora.data.model.FontSizeScale.LARGE, com.example.gemagora.data.model.FontSizeScale.fromScale(1.18f))

        val defaultSettings = com.example.gemagora.data.model.AppearanceSettings()
        assertEquals(1.0f, defaultSettings.fontScale, 0.001f)

        val updated = defaultSettings.copy(fontScale = 1.15f)
        assertEquals(1.15f, updated.fontScale, 0.001f)
    }
}
