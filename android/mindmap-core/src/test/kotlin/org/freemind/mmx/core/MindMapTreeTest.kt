package org.freemind.mmx.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MindMapTreeTest {
    @Test
    fun toggleFoldedUpdatesOnlyTarget() {
        val child = MindMapNode(id = "c", text = "Child")
        val branch = MindMapNode(id = "b", text = "Branch", children = listOf(child), folded = false)
        val map = MindMap(root = MindMapNode(id = "r", text = "Root", children = listOf(branch)))
        val folded = MindMapTree.toggleFolded(map, "b")
        assertTrue(MindMapTree.find(folded.root, "b")!!.folded)
        assertFalse(MindMapTree.find(folded.root, "c")!!.folded)
        assertEquals(3, MindMapTree.flatten(folded).size)
    }
}
