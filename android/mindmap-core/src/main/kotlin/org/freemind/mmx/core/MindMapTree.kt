package org.freemind.mmx.core

/** Helpers for immutable tree updates used by the viewer/editor. */
object MindMapTree {

    fun find(node: MindMapNode, id: String): MindMapNode? {
        if (node.id == id) return node
        for (child in node.children) {
            find(child, id)?.let { return it }
        }
        return null
    }

    fun find(map: MindMap, id: String): MindMapNode? = find(map.root, id)

    data class ParentRef(val parent: MindMapNode, val index: Int)

    fun findParent(map: MindMap, nodeId: String): ParentRef? {
        fun walk(parent: MindMapNode): ParentRef? {
            parent.children.forEachIndexed { index, child ->
                if (child.id == nodeId) return ParentRef(parent, index)
                walk(child)?.let { return it }
            }
            return null
        }
        return walk(map.root)
    }

    fun update(node: MindMapNode, id: String, transform: (MindMapNode) -> MindMapNode): MindMapNode {
        if (node.id == id) return transform(node)
        return node.copy(children = node.children.map { update(it, id, transform) })
    }

    fun update(map: MindMap, id: String, transform: (MindMapNode) -> MindMapNode): MindMap =
        map.copy(root = update(map.root, id, transform))

    fun replaceChildren(parent: MindMapNode, children: List<MindMapNode>): MindMapNode =
        parent.copy(children = children)

    fun insertChild(map: MindMap, parentId: String, child: MindMapNode, index: Int): MindMap {
        return update(map, parentId) { parent ->
            val kids = parent.children.toMutableList()
            val at = if (index < 0 || index > kids.size) kids.size else index
            // First-level children need a side for FreeMind layout.
            val normalized = if (parentId == map.root.id && child.side == null) {
                val preferRight = kids.count { it.side == NodeSide.RIGHT } <=
                    kids.count { it.side == NodeSide.LEFT }
                child.copy(side = if (preferRight) NodeSide.RIGHT else NodeSide.LEFT)
            } else {
                child
            }
            kids.add(at, normalized)
            parent.copy(children = kids, folded = false)
        }
    }

    fun removeNode(map: MindMap, nodeId: String): Pair<MindMap, Removal?> {
        if (map.root.id == nodeId) return map to null
        val parentRef = findParent(map, nodeId) ?: return map to null
        val removed = parentRef.parent.children[parentRef.index]
        val next = update(map, parentRef.parent.id) { parent ->
            parent.copy(children = parent.children.filterIndexed { i, _ -> i != parentRef.index })
        }
        return next to Removal(parentRef.parent.id, parentRef.index, removed)
    }

    data class Removal(val parentId: String, val index: Int, val node: MindMapNode)

    fun moveNode(
        map: MindMap,
        nodeId: String,
        toParentId: String,
        toIndex: Int,
    ): MindMap {
        if (map.root.id == nodeId) return map
        if (nodeId == toParentId) return map
        // Refuse moving a node into its own descendant.
        val moving = find(map, nodeId) ?: return map
        if (find(moving, toParentId) != null) return map

        val (without, removal) = removeNode(map, nodeId)
        if (removal == null) return map
        var insertIndex = toIndex
        if (removal.parentId == toParentId && removal.index < toIndex) {
            insertIndex = (toIndex - 1).coerceAtLeast(0)
        }
        return insertChild(without, toParentId, removal.node, insertIndex)
    }

    fun reorderAmongSiblings(map: MindMap, nodeId: String, delta: Int): MindMap {
        val parentRef = findParent(map, nodeId) ?: return map
        val from = parentRef.index
        val to = (from + delta).coerceIn(0, parentRef.parent.children.lastIndex)
        if (from == to) return map
        return update(map, parentRef.parent.id) { parent ->
            val kids = parent.children.toMutableList()
            val node = kids.removeAt(from)
            kids.add(to, node)
            parent.copy(children = kids)
        }
    }

    fun toggleFolded(map: MindMap, id: String): MindMap {
        val target = find(map.root, id) ?: return map
        if (target.children.isEmpty()) return map
        return map.copy(root = update(map.root, id) { it.copy(folded = !it.folded) })
    }

    fun flatten(map: MindMap): List<Pair<MindMapNode, String?>> {
        val out = mutableListOf<Pair<MindMapNode, String?>>()
        fun walk(node: MindMapNode, parentId: String?) {
            out += node to parentId
            node.children.forEach { walk(it, node.id) }
        }
        walk(map.root, null)
        return out
    }

    fun nowMillis(): Long = System.currentTimeMillis()
}
