package org.freemind.mmx.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditingCommandsTest {
    private fun sampleMap(): MindMap {
        val a = MindMapNode(id = "a", text = "A", side = NodeSide.RIGHT)
        val b = MindMapNode(id = "b", text = "B", side = NodeSide.LEFT)
        return MindMap(root = MindMapNode(id = "r", text = "Root", children = listOf(a, b)))
    }

    @Test
    fun setTextIsSingleUndoStep() {
        val stack = UndoRedoStack()
        var map = sampleMap()
        val node = MindMapTree.find(map, "a")!!
        map = stack.execute(
            map,
            SetNodeTextCommand(
                nodeId = "a",
                oldText = node.text,
                newText = "Alpha",
                oldRichContentHtml = node.richContentHtml,
                oldModifiedAtMillis = node.modifiedAtMillis,
            ),
        )
        assertEquals("Alpha", MindMapTree.find(map, "a")!!.text)
        map = stack.undo(map)
        assertEquals("A", MindMapTree.find(map, "a")!!.text)
        map = stack.redo(map)
        assertEquals("Alpha", MindMapTree.find(map, "a")!!.text)
    }

    @Test
    fun addChildAndUndoRemovesIt() {
        val stack = UndoRedoStack()
        var map = sampleMap()
        val child = MindMapNode(id = "c", text = "Child")
        map = stack.execute(map, AddChildCommand(parentId = "a", child = child))
        assertEquals(1, MindMapTree.find(map, "a")!!.children.size)
        map = stack.undo(map)
        assertTrue(MindMapTree.find(map, "a")!!.children.isEmpty())
    }

    @Test
    fun addSiblingInsertsAfter() {
        val stack = UndoRedoStack()
        var map = sampleMap()
        val sibling = MindMapNode(id = "s", text = "Sibling", side = NodeSide.RIGHT)
        map = stack.execute(map, AddSiblingCommand(siblingOfId = "a", child = sibling))
        assertEquals(listOf("a", "s", "b"), map.root.children.map { it.id })
    }

    @Test
    fun deleteSubtreeCannotRemoveRoot() {
        val stack = UndoRedoStack()
        var map = sampleMap()
        map = stack.execute(map, RemoveSubtreeCommand("r"))
        assertEquals("r", map.root.id)
        assertEquals(2, map.root.children.size)
    }

    @Test
    fun deleteAndUndoRestoresSubtree() {
        val stack = UndoRedoStack()
        var map = sampleMap()
        map = stack.execute(map, AddChildCommand("a", MindMapNode(id = "a1", text = "A1")))
        map = stack.execute(map, RemoveSubtreeCommand("a"))
        assertEquals(listOf("b"), map.root.children.map { it.id })
        map = stack.undo(map)
        assertEquals(listOf("a", "b"), map.root.children.map { it.id })
        assertEquals("A1", MindMapTree.find(map, "a1")!!.text)
    }

    @Test
    fun reorderAmongSiblings() {
        val stack = UndoRedoStack()
        var map = sampleMap()
        map = stack.execute(map, MoveAmongSiblingsCommand("a", delta = 1))
        assertEquals(listOf("b", "a"), map.root.children.map { it.id })
        map = stack.undo(map)
        assertEquals(listOf("a", "b"), map.root.children.map { it.id })
    }

    @Test
    fun toggleFoldRoundTripsOnUndo() {
        val stack = UndoRedoStack()
        var map = sampleMap()
        map = stack.execute(map, AddChildCommand("a", MindMapNode(id = "a1", text = "A1")))
        assertFalse(MindMapTree.find(map, "a")!!.folded)
        map = stack.execute(map, ToggleFoldedCommand("a"))
        assertTrue(MindMapTree.find(map, "a")!!.folded)
        map = stack.undo(map)
        assertFalse(MindMapTree.find(map, "a")!!.folded)
    }

    @Test
    fun changeSide() {
        val stack = UndoRedoStack()
        var map = sampleMap()
        map = stack.execute(map, SetNodeSideCommand("a", NodeSide.RIGHT, NodeSide.LEFT))
        assertEquals(NodeSide.LEFT, MindMapTree.find(map, "a")!!.side)
        map = stack.undo(map)
        assertEquals(NodeSide.RIGHT, MindMapTree.find(map, "a")!!.side)
    }
}
