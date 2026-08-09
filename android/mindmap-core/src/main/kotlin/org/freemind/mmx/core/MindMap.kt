package org.freemind.mmx.core

/**
 * Side of a first-level branch relative to the root.
 * Deeper descendants inherit their ancestor's side for layout.
 */
enum class NodeSide {
    LEFT,
    RIGHT,
}

/**
 * Stable domain representation of a FreeMind-compatible mind map.
 * Independent of Android UI and XML serialization details.
 */
data class MindMap(
    val version: String = DEFAULT_VERSION,
    val root: MindMapNode,
    val unknownElements: List<RawXmlElement> = emptyList(),
) {
    companion object {
        const val DEFAULT_VERSION: String = "1.0.1"

        fun blank(rootText: String = "New mind map"): MindMap =
            MindMap(root = MindMapNode(id = MindMapNode.newId(), text = rootText))
    }
}

data class MindMapNode(
    val id: String,
    val text: String = "",
    val children: List<MindMapNode> = emptyList(),
    val side: NodeSide? = null,
    val folded: Boolean = false,
    val createdAtMillis: Long? = null,
    val modifiedAtMillis: Long? = null,
    val style: String? = null,
    val color: String? = null,
    val backgroundColor: String? = null,
    val link: String? = null,
    val icons: List<String> = emptyList(),
    val noteHtml: String? = null,
    val richContentHtml: String? = null,
    val attributes: List<NodeAttribute> = emptyList(),
    val unknownAttributes: Map<String, String> = emptyMap(),
    val unknownChildren: List<RawXmlElement> = emptyList(),
) {
    companion object {
        fun newId(): String = "ID_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}

data class NodeAttribute(
    val name: String,
    val value: String,
)

/**
 * Opaque XML retained for round-trip compatibility.
 * Parsed/serialized by :freemind-format; ignored by core editing until understood.
 */
data class RawXmlElement(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val children: List<RawXmlElement> = emptyList(),
    val text: String? = null,
)
