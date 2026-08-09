package org.freemind.mmx.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MindMapTest {
    @Test
    fun blankMapHasRootText() {
        val map = MindMap.blank("Hello")
        assertEquals("Hello", map.root.text)
        assertTrue(map.root.id.isNotBlank())
        assertEquals(MindMap.DEFAULT_VERSION, map.version)
    }

    @Test
    fun undoStackStartsEmpty() {
        val stack = UndoRedoStack()
        assertEquals(false, stack.canUndo)
        assertEquals(false, stack.canRedo)
    }
}
