package org.freemind.mmx.layout

/**
 * Screen-space rectangle for a laid-out node.
 * Units are arbitrary logical pixels; the UI applies viewport transform.
 */
data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean =
        x in left..right && y in top..bottom
}

data class LaidOutNode(
    val id: String,
    val bounds: Rect,
    val text: String,
    val folded: Boolean,
    val hasChildren: Boolean,
)

data class Connector(
    val parentId: String,
    val childId: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

data class LayoutResult(
    val nodes: List<LaidOutNode>,
    val connectors: List<Connector>,
    val contentBounds: Rect,
)

/**
 * Milestone 3 will implement FreeMind-style radial/side layout.
 * Skeleton returns an empty layout so the app module can depend on the API.
 */
interface MindMapLayoutEngine {
    fun layout(input: LayoutInput): LayoutResult
}

data class LayoutInput(
    val rootId: String,
    val nodes: List<LayoutNode>,
)

data class LayoutNode(
    val id: String,
    val parentId: String?,
    val text: String,
    val folded: Boolean,
    val side: String?,
    val measuredWidth: Float,
    val measuredHeight: Float,
)

class StubMindMapLayoutEngine : MindMapLayoutEngine {
    override fun layout(input: LayoutInput): LayoutResult {
        val root = input.nodes.firstOrNull { it.id == input.rootId }
            ?: return LayoutResult(emptyList(), emptyList(), Rect(0f, 0f, 0f, 0f))
        val bounds = Rect(
            left = -root.measuredWidth / 2f,
            top = -root.measuredHeight / 2f,
            right = root.measuredWidth / 2f,
            bottom = root.measuredHeight / 2f,
        )
        return LayoutResult(
            nodes = listOf(
                LaidOutNode(
                    id = root.id,
                    bounds = bounds,
                    text = root.text,
                    folded = root.folded,
                    hasChildren = input.nodes.any { it.parentId == root.id },
                ),
            ),
            connectors = emptyList(),
            contentBounds = bounds,
        )
    }
}
