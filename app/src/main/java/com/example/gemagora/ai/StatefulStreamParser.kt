package com.example.gemagora.ai

/**
 * Stateful stream parser to handle Gemma 4 thinking tokens and control tokens.
 * Handles split chunk arrivals (e.g., `<|channel>thought`, `<channel|>`, `<think>`, `</think>`).
 */
class StatefulStreamParser {
    private val buffer = StringBuilder()
    private val thinkingBuffer = StringBuilder()
    private val contentBuffer = StringBuilder()

    var isThinking: Boolean = false
        private set

    fun appendToken(token: String): StreamChunk {
        buffer.append(token)
        val full = buffer.toString()

        // Check for start of thought block
        if (!isThinking) {
            val thoughtStart = when {
                full.contains("<|channel>thought") -> full.indexOf("<|channel>thought") + "<|channel>thought".length
                full.contains("<think>") -> full.indexOf("<think>") + "<think>".length
                else -> -1
            }

            if (thoughtStart != -1) {
                isThinking = true
                // Any text before thought block belongs to content
                val prefix = full.substring(0, thoughtStart - (if (full.contains("<|channel>thought")) "<|channel>thought".length else "<think>".length))
                if (prefix.isNotBlank()) {
                    contentBuffer.append(prefix)
                }
                buffer.clear()
                buffer.append(full.substring(thoughtStart))
            } else if (!isPotentialTagStart(full)) {
                contentBuffer.append(full)
                buffer.clear()
            }
        } else {
            // Currently thinking, check for end of thought block
            val thoughtEnd = when {
                full.contains("<channel|>") -> full.indexOf("<channel|>")
                full.contains("</think>") -> full.indexOf("</think>")
                else -> -1
            }

            if (thoughtEnd != -1) {
                val tagLength = if (full.contains("<channel|>")) "<channel|>".length else "</think>".length
                thinkingBuffer.append(full.substring(0, thoughtEnd))
                isThinking = false
                val remaining = full.substring(thoughtEnd + tagLength)
                buffer.clear()
                if (remaining.isNotEmpty()) {
                    contentBuffer.append(remaining)
                }
            } else if (!isPotentialTagStart(full)) {
                thinkingBuffer.append(full)
                buffer.clear()
            }
        }

        return StreamChunk(
            thinkingText = thinkingBuffer.toString().trim(),
            contentText = contentBuffer.toString(),
            isCurrentlyThinking = isThinking
        )
    }

    fun finish(): StreamChunk {
        // Flush any remaining buffer
        if (buffer.isNotEmpty()) {
            if (isThinking) {
                thinkingBuffer.append(buffer.toString())
            } else {
                contentBuffer.append(buffer.toString())
            }
            buffer.clear()
        }
        return StreamChunk(
            thinkingText = thinkingBuffer.toString().trim(),
            contentText = contentBuffer.toString().trim(),
            isCurrentlyThinking = false
        )
    }

    private fun isPotentialTagStart(text: String): Boolean {
        return CONTROL_TAGS.any { tag ->
            val maxLen = minOf(text.length, tag.length - 1)
            (1..maxLen).any { len -> text.endsWith(tag.substring(0, len)) }
        }
    }

    companion object {
        private val CONTROL_TAGS = listOf(
            "<|channel>thought",
            "<channel|>",
            "<think>",
            "</think>"
        )
    }

    data class StreamChunk(
        val thinkingText: String,
        val contentText: String,
        val isCurrentlyThinking: Boolean
    )
}
