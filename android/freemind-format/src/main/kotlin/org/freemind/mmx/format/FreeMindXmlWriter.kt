package org.freemind.mmx.format

import org.freemind.mmx.core.MindMap
import org.freemind.mmx.core.MindMapNode
import org.freemind.mmx.core.NodeSide
import org.freemind.mmx.core.RawXmlElement

/**
 * Serializes [MindMap] to FreeMind `.mm` / FreeMind-MMX `.mmx` XML.
 *
 * `managedAttr` mirrors legacy `NodeAdapter.save(..., managed_attr)`:
 * - 0: `.mm` content file (volatile attrs omitted / fold forced)
 * - 1: `.mmx` sidecar (ID + FOLDED + CREATED + MODIFIED only)
 * - 2: vanilla all-in-one `.mm`
 */
class FreeMindXmlWriter {
    fun writeMm(map: MindMap, options: WriteOptions = WriteOptions()): String {
        val mode = if (options.separateVolatileAttributes) 0 else 2
        return write(map, options, managedAttr = mode)
    }

    fun writeMmx(map: MindMap, options: WriteOptions = WriteOptions()): String =
        write(map, options, managedAttr = 1)

    private fun write(map: MindMap, options: WriteOptions, managedAttr: Int): String =
        buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<map version=\"")
            append(xmlEscape(options.mapVersion ?: map.version))
            append("\">\n")
            append("<!-- This file is saved using FreeMind-MMX Android. -->\n")
            when (managedAttr) {
                0 -> append("<!-- This .mm file is VCS friendly; some atts are saved in .mmx. -->\n")
                1 -> append("<!-- This .mmx file stores auxiliary attributes (fold/timestamps). -->\n")
            }
            if (managedAttr != 1) {
                for (element in map.unknownElements) {
                    appendRaw(element, indent = 0)
                }
            }
            appendNode(map.root, managedAttr, options, isRoot = true, parentIsRoot = false)
            append("</map>\n")
        }

    private fun StringBuilder.appendNode(
        node: MindMapNode,
        managedAttr: Int,
        options: WriteOptions,
        isRoot: Boolean,
        parentIsRoot: Boolean,
    ) {
        append("<node")
        // Always emit ID — required for MMX join and stable editing.
        appendAttr("ID", node.id)

        when (managedAttr) {
            1 -> {
                appendAttr("FOLDED", if (node.folded) "true" else "false")
                node.createdAtMillis?.let { appendAttr("CREATED", it.toString()) }
                node.modifiedAtMillis?.let { appendAttr("MODIFIED", it.toString()) }
            }
            else -> {
                val hasRichNode = !node.richContentHtml.isNullOrBlank()
                if (!hasRichNode) {
                    appendAttr("TEXT", node.text, options.utf8WithoutEntities)
                }
                if (managedAttr == 0 && options.separateVolatileAttributes) {
                    if (!isRoot && node.children.isNotEmpty()) {
                        appendAttr("FOLDED", "true")
                    }
                    // CREATED/MODIFIED intentionally omitted from .mm
                } else {
                    appendAttr("FOLDED", if (node.folded) "true" else "false")
                    node.createdAtMillis?.let { appendAttr("CREATED", it.toString()) }
                    node.modifiedAtMillis?.let { appendAttr("MODIFIED", it.toString()) }
                }
                if (parentIsRoot && node.side != null) {
                    appendAttr(
                        "POSITION",
                        when (node.side) {
                            NodeSide.LEFT -> "left"
                            NodeSide.RIGHT -> "right"
                            null -> error("unreachable")
                        },
                    )
                }
                node.color?.let { appendAttr("COLOR", it) }
                node.backgroundColor?.let { appendAttr("BACKGROUND_COLOR", it) }
                node.style?.let { appendAttr("STYLE", it) }
                node.link?.let { appendAttr("LINK", it, options.utf8WithoutEntities) }
                for ((key, value) in node.unknownAttributes) {
                    appendAttr(key, value, options.utf8WithoutEntities)
                }
            }
        }

        val writeContentChildren = managedAttr != 1
        val hasElementChildren = when {
            managedAttr == 1 -> node.children.isNotEmpty()
            else -> {
                hasRichNodeBody(node) ||
                    !node.noteHtml.isNullOrBlank() ||
                    node.icons.isNotEmpty() ||
                    node.attributes.isNotEmpty() ||
                    node.unknownChildren.isNotEmpty() ||
                    node.children.isNotEmpty()
            }
        }

        if (!hasElementChildren) {
            append("/>\n")
            return
        }
        append(">\n")

        if (writeContentChildren) {
            if (!node.richContentHtml.isNullOrBlank()) {
                append("<richcontent TYPE=\"NODE\">")
                append(node.richContentHtml)
                append("</richcontent>\n")
            }
            if (!node.noteHtml.isNullOrBlank()) {
                append("<richcontent TYPE=\"NOTE\">")
                append(node.noteHtml)
                append("</richcontent>\n")
            }
            for (icon in node.icons) {
                append("<icon BUILTIN=\"")
                append(xmlEscape(icon, options.utf8WithoutEntities))
                append("\"/>\n")
            }
            for (attr in node.attributes) {
                append("<attribute NAME=\"")
                append(xmlEscape(attr.name, options.utf8WithoutEntities))
                append("\" VALUE=\"")
                append(xmlEscape(attr.value, options.utf8WithoutEntities))
                append("\"/>\n")
            }
            for (child in node.unknownChildren) {
                appendRaw(child, indent = 0)
            }
        }

        for (child in node.children) {
            appendNode(
                child,
                managedAttr,
                options,
                isRoot = false,
                parentIsRoot = isRoot,
            )
        }
        append("</node>\n")
    }

    private fun hasRichNodeBody(node: MindMapNode): Boolean =
        !node.richContentHtml.isNullOrBlank()

    private fun StringBuilder.appendAttr(
        name: String,
        value: String,
        utf8Raw: Boolean = true,
    ) {
        append(' ')
        append(name)
        append("=\"")
        append(xmlEscape(value, utf8Raw))
        append('"')
    }

    private fun StringBuilder.appendRaw(element: RawXmlElement, indent: Int) {
        append('<').append(element.name)
        for ((k, v) in element.attributes) {
            append(' ').append(k).append("=\"").append(xmlEscape(v)).append('"')
        }
        if (element.children.isEmpty() && element.text.isNullOrBlank()) {
            append("/>\n")
            return
        }
        append('>')
        if (!element.text.isNullOrEmpty()) {
            append(element.text)
        }
        for (child in element.children) {
            appendRaw(child, indent + 1)
        }
        append("</").append(element.name).append(">\n")
    }
}

internal fun xmlEscape(value: String, utf8Raw: Boolean = true): String {
    val sb = StringBuilder(value.length + 8)
    for (ch in value) {
        when (ch) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\n' -> sb.append("&#xa;")
            '\r' -> sb.append("&#xd;")
            '\t' -> sb.append("&#x9;")
            else -> {
                val code = ch.code
                if (!utf8Raw && code > 126) {
                    sb.append("&#x").append(code.toString(16)).append(';')
                } else if (code < 32) {
                    sb.append("&#x").append(code.toString(16)).append(';')
                } else {
                    sb.append(ch)
                }
            }
        }
    }
    return sb.toString()
}
