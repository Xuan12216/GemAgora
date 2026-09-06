package com.example.gemagora.ui.journal

import android.content.Context
import android.content.Intent
import com.example.gemagora.data.model.JournalEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JournalExporter {

    fun exportToMarkdown(entries: List<JournalEntry>): String {
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.append("# GemAgora 斯多葛哲學沉思集\n")
        sb.append("匯出日期：$currentDate\n")
        sb.append("備註：本檔案由地端離線 AI 模型 Gemma 4 輔助生成，絕無隱私外洩。\n\n")
        sb.append("---\n\n")

        entries.sortedByDescending { it.timestamp }.forEach { entry ->
            val typeTag = when (entry.entryType) {
                "morning" -> "🌅 斯多葛晨思"
                "evening" -> "🌙 斯多葛夕省"
                else -> "💭 自由沉思"
            }
            sb.append("## ${entry.title} ($typeTag)\n")
            sb.append("**日期**：${entry.dateString}\n\n")
            sb.append("### 📝 思緒記錄\n")
            sb.append("${entry.content}\n\n")

            if (!entry.aiGuidance.isNullOrBlank()) {
                val cleanGuidance = entry.aiGuidance
                    .replace(Regex("<\\|?channel\\|?>[a-zA-Z0-9_]*.*?(<channel\\|>|$)", RegexOption.DOT_MATCHES_ALL), "")
                    .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
                    .trim()
                sb.append("### 💡 本地 AI 哲思指引\n")
                sb.append("> ${cleanGuidance.replace("\n", "\n> ")}\n\n")
            }
            sb.append("---\n\n")
        }

        return sb.toString()
    }

    fun shareMarkdown(context: Context, markdownText: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, markdownText)
            type = "text/markdown"
        }
        val shareIntent = Intent.createChooser(sendIntent, "分享或保存哲學日記 Markdown").apply {
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(shareIntent)
    }
}
