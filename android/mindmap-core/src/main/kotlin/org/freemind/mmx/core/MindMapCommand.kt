package org.freemind.mmx.core

/**
 * Reversible editing operations. Each command captures enough prior state
 * to invert without depending on UI/XML.
 */
sealed interface MindMapCommand {
    fun apply(map: MindMap): MindMap
    fun invert(): MindMapCommand
}

data class SetNodeTextCommand(
    val nodeId: String,
    val oldText: String,
    val newText: String,
    val oldRichContentHtml: String? = null,
    val oldModifiedAtMillis: Long? = null,
    val newModifiedAtMillis: Long = MindMapTree.nowMillis(),
) : MindMapCommand {
    override fun apply(map: MindMap): MindMap =
        MindMapTree.update(map, nodeId) {
            it.copy(
                text = newText,
                richContentHtml = null,
                modifiedAtMillis = newModifiedAtMillis,
            )
        }

    override fun invert(): MindMapCommand = RestoreNodeTextCommand(
        nodeId = nodeId,
        text = oldText,
        richContentHtml = oldRichContentHtml,
        modifiedAtMillis = oldModifiedAtMillis,
        redo = this,
    )
}

data class RestoreNodeTextCommand(
    val nodeId: String,
    val text: String,
    val richContentHtml: String?,
    val modifiedAtMillis: Long?,
    val redo: SetNodeTextCommand,
) : MindMapCommand {
    override fun apply(map: MindMap): MindMap =
        MindMapTree.update(map, nodeId) {
            it.copy(
                text = text,
                richContentHtml = richContentHtml,
                modifiedAtMillis = modifiedAtMillis,
            )
        }

    override fun invert(): MindMapCommand = redo
}

data class AddChildCommand(
    val parentId: String,
    val child: MindMapNode,
    val index: Int = -1,
) : MindMapCommand {
    override fun apply(map: MindMap): MindMap =
        MindMapTree.insertChild(map, parentId, child, index)

    override fun invert(): MindMapCommand = RemoveSubtreeCommand(nodeId = child.id)
}

data class AddSiblingCommand(
    val siblingOfId: String,
    val child: MindMapNode,
) : MindMapCommand {
    override fun apply(map: MindMap): MindMap {
        val parent = MindMapTree.findParent(map, siblingOfId) ?: return map
        return MindMapTree.insertChild(map, parent.parent.id, child, parent.index + 1)
    }

    override fun invert(): MindMapCommand = RemoveSubtreeCommand(nodeId = child.id)
}

class RemoveSubtreeCommand(
    val nodeId: String,
) : MindMapCommand {
    private var removal: MindMapTree.Removal? = null

    override fun apply(map: MindMap): MindMap {
        if (map.root.id == nodeId) return map
        val (next, rem) = MindMapTree.removeNode(map, nodeId)
        removal = rem
        return next
    }

    override fun invert(): MindMapCommand {
        val rem = removal ?: return NoOpCommand
        return AddChildCommand(parentId = rem.parentId, child = rem.node, index = rem.index)
    }
}

data class MoveAmongSiblingsCommand(
    val nodeId: String,
    val delta: Int,
) : MindMapCommand {
    override fun apply(map: MindMap): MindMap =
        MindMapTree.reorderAmongSiblings(map, nodeId, delta)

    override fun invert(): MindMapCommand =
        MoveAmongSiblingsCommand(nodeId = nodeId, delta = -delta)
}

data class ToggleFoldedCommand(
    val nodeId: String,
) : MindMapCommand {
    override fun apply(map: MindMap): MindMap = MindMapTree.toggleFolded(map, nodeId)
    override fun invert(): MindMapCommand = this
}

data class SetNodeSideCommand(
    val nodeId: String,
    val oldSide: NodeSide?,
    val newSide: NodeSide?,
) : MindMapCommand {
    override fun apply(map: MindMap): MindMap =
        MindMapTree.update(map, nodeId) { it.copy(side = newSide) }

    override fun invert(): MindMapCommand =
        SetNodeSideCommand(nodeId, oldSide = newSide, newSide = oldSide)
}

data object NoOpCommand : MindMapCommand {
    override fun apply(map: MindMap): MindMap = map
    override fun invert(): MindMapCommand = this
}

class UndoRedoStack {
    private val undo = ArrayDeque<MindMapCommand>()
    private val redo = ArrayDeque<MindMapCommand>()

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()

    fun clear() {
        undo.clear()
        redo.clear()
    }

    fun execute(map: MindMap, command: MindMapCommand): MindMap {
        if (command is NoOpCommand) return map
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
