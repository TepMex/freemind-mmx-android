package org.freemind.mmx.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FreeMindLayoutEngineTest {
    private val engine = FreeMindLayoutEngine()

    @Test
    fun rootCenteredWithLeftAndRightBranches() {
        val result = engine.layout(
            LayoutInput(
                rootId = "r",
                nodes = listOf(
                    LayoutNode("r", null, "Root", false, null, 100f, 40f),
                    LayoutNode("l", "r", "Left", false, "left", 80f, 30f),
                    LayoutNode("r1", "r", "Right", false, "right", 80f, 30f),
                    LayoutNode("r1a", "r1", "Child", false, null, 60f, 24f),
                ),
            ),
        )
        assertEquals(4, result.nodes.size)
        assertEquals(0f, result.nodes.first { it.id == "r" }.bounds.centerX)
        assertTrue(result.nodes.first { it.id == "l" }.bounds.centerX < 0f)
        assertTrue(result.nodes.first { it.id == "r1" }.bounds.centerX > 0f)
        assertTrue(result.nodes.first { it.id == "r1a" }.bounds.centerX > result.nodes.first { it.id == "r1" }.bounds.centerX)
        assertEquals(3, result.connectors.size)
    }

    @Test
    fun foldedNodeHidesDescendants() {
        val result = engine.layout(
            LayoutInput(
                rootId = "r",
                nodes = listOf(
                    LayoutNode("r", null, "Root", false, null, 100f, 40f),
                    LayoutNode("a", "r", "Folded", true, "right", 80f, 30f),
                    LayoutNode("b", "a", "Hidden", false, null, 60f, 24f),
                ),
            ),
        )
        assertEquals(setOf("r", "a"), result.nodes.map { it.id }.toSet())
        assertTrue(result.nodes.first { it.id == "a" }.folded)
        assertTrue(result.nodes.first { it.id == "a" }.hasChildren)
    }
}
