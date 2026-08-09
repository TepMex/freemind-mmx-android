package org.freemind.mmx.core

/**
 * Placeholder for reversible editing operations (Milestone 4).
 * Editing must not require callers to manipulate XML directly.
 */
sealed interface MindMapCommand {
    fun apply(map: MindMap): MindMap
    fun invert(): MindMapCommand
}

class UndoRedoStack {
    private val undo = ArrayDeque<MindMapCommand>()
    private val redo = ArrayDeque<MindMapCommand>()

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()

    fun execute(map: MindMap, command: MindMapCommand): MindMap {
        val next = command.apply(map)
        undo.addLast(command)
        redo.clear()
        return next
    }

    fun undo(map: MindMap): MindMap {
        val command = undo.removeLastOrNull() ?: return map
        val previous = command.invert().apply(map)
        redo.addLast(command)
        return previous
    }

    fun redo(map: MindMap): MindMap {
        val command = redo.removeLastOrNull() ?: return map
        val next = command.apply(map)
        undo.addLast(command)
        return next
    }
}
