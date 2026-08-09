package org.freemind.mmx.layout

/**
 * FreeMind-style layout: root centered, first-level branches to left/right,
 * descendants extending outward. Folded nodes omit descendant geometry.
 */
class FreeMindLayoutEngine(
    private val horizontalGap: Float = 48f,
    private val verticalGap: Float = 16f,
) : MindMapLayoutEngine {

    override fun layout(input: LayoutInput): LayoutResult {
        val byId = input.nodes.associateBy { it.id }
        val root = byId[input.rootId]
            ?: return LayoutResult(emptyList(), emptyList(), Rect(0f, 0f, 0f, 0f))
        val childrenOf = input.nodes
            .filter { it.parentId != null }
            .groupBy { it.parentId!! }

        data class Placed(
            val node: LayoutNode,
            val x: Float,
            val y: Float,
        )

        val placed = mutableListOf<Placed>()

        fun subtreeHeight(node: LayoutNode): Float {
            val kids = if (node.folded) emptyList() else childrenOf[node.id].orEmpty()
            if (kids.isEmpty()) return node.measuredHeight
            val kidsHeight = kids.sumOf { subtreeHeight(it).toDouble() }.toFloat() +
                verticalGap * (kids.size - 1).coerceAtLeast(0)
            return maxOf(node.measuredHeight, kidsHeight)
        }

        fun placeSubtree(
            node: LayoutNode,
            toRight: Boolean,
            parentX: Float,
            parentWidth: Float,
            centerY: Float,
        ) {
            val x = if (toRight) {
                parentX + parentWidth / 2f + horizontalGap + node.measuredWidth / 2f
            } else {
                parentX - parentWidth / 2f - horizontalGap - node.measuredWidth / 2f
            }
            placed += Placed(node, x, centerY)
            val kids = if (node.folded) emptyList() else childrenOf[node.id].orEmpty()
            if (kids.isEmpty()) return
            val heights = kids.map { subtreeHeight(it) }
            val total = heights.sum() + verticalGap * (kids.size - 1).coerceAtLeast(0)
            var cursorY = centerY - total / 2f
            kids.forEachIndexed { index, child ->
                val h = heights[index]
                placeSubtree(child, toRight, x, node.measuredWidth, cursorY + h / 2f)
                cursorY += h + verticalGap
            }
        }

        fun placeSide(nodes: List<LayoutNode>, toRight: Boolean) {
            if (nodes.isEmpty()) return
            val heights = nodes.map { subtreeHeight(it) }
            val total = heights.sum() + verticalGap * (nodes.size - 1).coerceAtLeast(0)
            var cursorY = -total / 2f
            nodes.forEachIndexed { index, node ->
                val h = heights[index]
                placeSubtree(node, toRight, parentX = 0f, parentWidth = root.measuredWidth, cursorY + h / 2f)
                cursorY += h + verticalGap
            }
        }

        placed += Placed(root, 0f, 0f)

        val firstLevel = childrenOf[root.id].orEmpty()
        val left = mutableListOf<LayoutNode>()
        val right = mutableListOf<LayoutNode>()
        var unspecifiedIndex = 0
        for (node in firstLevel) {
            when (node.side?.lowercase()) {
                "left" -> left += node
                "right" -> right += node
                else -> {
                    if (unspecifiedIndex % 2 == 0) right += node else left += node
                    unspecifiedIndex++
                }
            }
        }
        placeSide(left, toRight = false)
        placeSide(right, toRight = true)

        val laidOut = placed.map { p ->
            LaidOutNode(
                id = p.node.id,
                bounds = Rect(
                    left = p.x - p.node.measuredWidth / 2f,
                    top = p.y - p.node.measuredHeight / 2f,
                    right = p.x + p.node.measuredWidth / 2f,
                    bottom = p.y + p.node.measuredHeight / 2f,
                ),
                text = p.node.text,
                folded = p.node.folded,
                hasChildren = childrenOf[p.node.id].orEmpty().isNotEmpty(),
            )
        }

        val boundsById = laidOut.associateBy { it.id }
        val connectors = mutableListOf<Connector>()
        for (p in placed) {
            val parentId = p.node.parentId ?: continue
            val parent = boundsById[parentId] ?: continue
            val child = boundsById[p.node.id] ?: continue
            val toRight = child.bounds.centerX >= parent.bounds.centerX
            connectors += Connector(
                parentId = parentId,
                childId = p.node.id,
                startX = if (toRight) parent.bounds.right else parent.bounds.left,
                startY = parent.bounds.centerY,
                endX = if (toRight) child.bounds.left else child.bounds.right,
                endY = child.bounds.centerY,
            )
        }

        val contentBounds = if (laidOut.isEmpty()) {
            Rect(0f, 0f, 0f, 0f)
        } else {
            Rect(
                left = laidOut.minOf { it.bounds.left },
                top = laidOut.minOf { it.bounds.top },
                right = laidOut.maxOf { it.bounds.right },
                bottom = laidOut.maxOf { it.bounds.bottom },
            )
        }

        return LayoutResult(laidOut, connectors, contentBounds)
    }
}
