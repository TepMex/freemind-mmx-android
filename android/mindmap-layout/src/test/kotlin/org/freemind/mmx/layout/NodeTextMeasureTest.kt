package org.freemind.mmx.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NodeTextMeasureTest {

    @Test
    fun singleShortLineFitsMinHeight() {
        val measured = NodeTextMeasure.measure("Hello")
        assertEquals(listOf("Hello"), measured.lines)
        assertTrue(measured.nodeWidth >= NodeTextMeasure.MinNodeWidth)
        assertTrue(measured.nodeHeight >= NodeTextMeasure.MinNodeHeight)
        assertTrue(measured.nodeHeight < NodeTextMeasure.DefaultTextSize * 3)
    }

    @Test
    fun explicitNewlinesProduceMultipleLinesAndTallerNode() {
        val measured = NodeTextMeasure.measure("line one\nline two\nline three")
        assertEquals(listOf("line one", "line two", "line three"), measured.lines)
        val single = NodeTextMeasure.measure("line one")
        assertTrue(measured.nodeHeight > single.nodeHeight)
        assertEquals(3, measured.lines.size)
    }

    @Test
    fun longLineWrapsToMaxContentWidth() {
        val long = "word ".repeat(40).trim()
        val measured = NodeTextMeasure.measure(long)
        assertTrue(measured.lines.size > 1)
        assertTrue(measured.contentWidth <= NodeTextMeasure.MaxContentWidth + 0.01f)
        assertTrue(
            measured.nodeWidth <=
                NodeTextMeasure.MaxContentWidth + NodeTextMeasure.HorizontalPadding * 2 + 0.01f,
        )
        measured.lines.forEach { line ->
            assertTrue(
                line.length * NodeTextMeasure.ApproxCharWidth <=
                    NodeTextMeasure.MaxContentWidth + NodeTextMeasure.ApproxCharWidth,
                "line longer than wrap budget: '$line'",
            )
        }
    }

    @Test
    fun blankTextStillProducesDrawableBox() {
        val measured = NodeTextMeasure.measure("   ")
        assertEquals(1, measured.lines.size)
        assertTrue(measured.nodeWidth >= NodeTextMeasure.MinNodeWidth)
        assertTrue(measured.nodeHeight >= NodeTextMeasure.MinNodeHeight)
    }

    @Test
    fun wrapPrefersSpaceBreaks() {
        val lines = NodeTextMeasure.wrapParagraph(
            "alpha beta gamma delta epsilon",
            maxContentWidth = 10 * NodeTextMeasure.ApproxCharWidth,
            charWidth = NodeTextMeasure.ApproxCharWidth,
        )
        assertTrue(lines.size >= 2)
        lines.forEach { line ->
            assertTrue(!line.contains("alphab") && line.isNotBlank(), "unexpected break: '$line'")
        }
    }

    @Test
    fun tallerMultilineNodesAffectSubtreeSpacing() {
        val tall = NodeTextMeasure.measure("one\ntwo\nthree\nfour")
        val result = FreeMindLayoutEngine().layout(
            LayoutInput(
                rootId = "r",
                nodes = listOf(
                    LayoutNode("r", null, "Root", false, null, 100f, 40f),
                    LayoutNode("a", "r", "Tall A", false, "right", tall.nodeWidth, tall.nodeHeight),
                    LayoutNode("b", "r", "Tall B", false, "right", tall.nodeWidth, tall.nodeHeight),
                ),
            ),
        )
        val a = result.nodes.first { it.id == "a" }
        val b = result.nodes.first { it.id == "b" }
        assertTrue(kotlin.math.abs(a.bounds.centerY - b.bounds.centerY) >= tall.nodeHeight)
    }
}
