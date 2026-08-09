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

    fun update(node: MindMapNode, id: String, transform: (MindMapNode) -> MindMapNode): MindMapNode {
        if (node.id == id) return transform(node)
        return node.copy(children = node.children.map { update(it, id, transform) })
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
}
