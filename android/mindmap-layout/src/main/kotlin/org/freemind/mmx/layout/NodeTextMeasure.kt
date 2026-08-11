package org.freemind.mmx.layout

/**
 * Measures node labels for layout and canvas drawing.
 *
 * Mirrors FreeMind's long/multiline node behavior: respect explicit newlines,
 * wrap long lines to [maxContentWidth], and grow node height with line count.
 * Uses a fixed glyph advance estimate so measurement stays JVM-testable and
 * consistent between layout and draw without depending on Android text APIs.
 */
data class MeasuredNodeText(
    val lines: List<String>,
    val contentWidth: Float,
    val contentHeight: Float,
    val nodeWidth: Float,
    val nodeHeight: Float,
)

object NodeTextMeasure {
    const val DefaultTextSize = 14f
    /** FreeMind default max_node_width is 600; mobile canvas uses a tighter cap. */
    const val MaxContentWidth = 240f
    const val MinNodeWidth = 88f
    const val MinNodeHeight = 36f
    const val HorizontalPadding = 16f
    const val VerticalPadding = 10f
    /** Approximate Latin glyph advance at [DefaultTextSize] (matches prior heuristic). */
    const val ApproxCharWidth = 7.5f
    const val LineHeightMultiplier = 1.3f

    fun measure(
        text: String,
        maxContentWidth: Float = MaxContentWidth,
        charWidth: Float = ApproxCharWidth,
        textSize: Float = DefaultTextSize,
    ): MeasuredNodeText {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val display = normalized.ifBlank { " " }
        val lineHeight = textSize * LineHeightMultiplier
        val paragraphs = display.split('\n')
        val lines = ArrayList<String>(paragraphs.size)
        var widest = 0f

        for (paragraph in paragraphs) {
            val wrapped = wrapParagraph(paragraph, maxContentWidth, charWidth)
            for (line in wrapped) {
                lines += line
                widest = maxOf(widest, lineWidth(line, charWidth))
            }
        }

        val contentWidth = widest.coerceAtMost(maxContentWidth)
        val contentHeight = lines.size * lineHeight
        val nodeWidth = (contentWidth + HorizontalPadding * 2f).coerceAtLeast(MinNodeWidth)
        val nodeHeight = (contentHeight + VerticalPadding * 2f).coerceAtLeast(MinNodeHeight)
        return MeasuredNodeText(
            lines = lines,
            contentWidth = contentWidth,
            contentHeight = contentHeight,
            nodeWidth = nodeWidth,
            nodeHeight = nodeHeight,
        )
    }

    private fun lineWidth(line: String, charWidth: Float): Float =
        if (line.isEmpty()) 0f else line.length * charWidth

    internal fun wrapParagraph(
        paragraph: String,
        maxContentWidth: Float,
        charWidth: Float,
    ): List<String> {
        if (paragraph.isEmpty()) return listOf("")
        val maxChars = maxOf(1, (maxContentWidth / charWidth).toInt())
        if (paragraph.length <= maxChars) return listOf(paragraph)

        val lines = mutableListOf<String>()
        var remaining = paragraph
        while (remaining.length > maxChars) {
            val window = remaining.take(maxChars + 1)
            val spaceBreak = window.lastIndexOf(' ')
            val breakAt = if (spaceBreak > 0) spaceBreak else maxChars
            lines += remaining.take(breakAt).trimEnd()
            remaining = remaining.drop(breakAt).trimStart()
            if (remaining.isEmpty()) break
        }
        if (remaining.isNotEmpty()) lines += remaining
        return lines.ifEmpty { listOf("") }
    }
}
