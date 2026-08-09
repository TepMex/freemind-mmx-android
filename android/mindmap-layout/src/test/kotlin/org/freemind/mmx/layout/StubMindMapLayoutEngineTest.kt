package org.freemind.mmx.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StubMindMapLayoutEngineTest {
    @Test
    fun layoutsRootCentered() {
        val engine = StubMindMapLayoutEngine()
        val result = engine.layout(
            LayoutInput(
                rootId = "r",
                nodes = listOf(
                    LayoutNode(
                        id = "r",
                        parentId = null,
                        text = "Root",
                        folded = false,
                        side = null,
                        measuredWidth = 100f,
                        measuredHeight = 40f,
                    ),
                ),
            ),
        )
        assertEquals(1, result.nodes.size)
        assertEquals(0f, result.nodes[0].bounds.centerX)
        assertEquals(0f, result.nodes[0].bounds.centerY)
        assertTrue(result.connectors.isEmpty())
    }
}
