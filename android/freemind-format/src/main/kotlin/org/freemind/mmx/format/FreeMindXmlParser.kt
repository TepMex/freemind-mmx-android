package org.freemind.mmx.format

import org.freemind.mmx.core.MindMap
import org.freemind.mmx.core.MindMapNode
import org.freemind.mmx.core.NodeAttribute
import org.freemind.mmx.core.NodeSide
import org.freemind.mmx.core.RawXmlElement
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/**
 * Parses FreeMind `.mm` XML into the domain model.
 *
 * Behavior derived from legacy `XMLElementAdapter` / `NodeAdapter` and
 * FreeMind-MMX join semantics (`freemind_join_mm_mmx.xslt`): when an `.mmx`
 * document is supplied, node attributes from the sidecar overlay the `.mm`
 * node that shares the same `ID`.
 */
class FreeMindXmlParser {
    fun parse(mmXml: String, mmxXml: String? = null): MindMap {
        val mmDocument = parseDocument(mmXml)
        val mapElement = mmDocument.documentElement
            ?: throw FreeMindParseException("Missing document element")
        if (!mapElement.tagName.equals("map", ignoreCase = true)) {
            throw FreeMindParseException("Root element must be <map>, found <${mapElement.tagName}>")
        }

        val mmxById: Map<String, Element> =
            if (mmxXml.isNullOrBlank()) {
                emptyMap()
            } else {
                indexMmxNodesById(parseDocument(mmxXml).documentElement)
            }

        val version = mapElement.getAttribute("version").ifBlank { MindMap.DEFAULT_VERSION }
        val unknownElements = mutableListOf<RawXmlElement>()
        var root: MindMapNode? = null

        for (child in mapElement.elementChildren()) {
            when (child.tagName.lowercase()) {
                "node" -> {
                    if (root != null) {
                        throw FreeMindParseException("Multiple root <node> elements are not supported")
                    }
                    root = parseNode(child, mmxById)
                }
                else -> unknownElements += toRawXml(child)
            }
        }

        return MindMap(
            version = version,
            root = root ?: throw FreeMindParseException("Map has no root <node>"),
            unknownElements = unknownElements,
        )
    }

    private fun parseDocument(xml: String) =
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isIgnoringComments = false
                isCoalescing = true
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        } catch (e: Exception) {
            throw FreeMindParseException("Failed to parse XML: ${e.message}", e)
        }

    private fun indexMmxNodesById(root: Element?): Map<String, Element> {
        if (root == null) return emptyMap()
        val result = LinkedHashMap<String, Element>()
        fun walk(element: Element) {
            if (element.tagName.equals("node", ignoreCase = true)) {
                val id = element.getAttribute("ID")
                if (id.isNotEmpty()) {
                    result[id] = element
                }
            }
            for (child in element.elementChildren()) {
                walk(child)
            }
        }
        walk(root)
        return result
    }

    private fun parseNode(element: Element, mmxById: Map<String, Element>): MindMapNode {
        val attrs = linkedMapOf<String, String>()
        val named = element.attributes
        for (i in 0 until named.length) {
            val item = named.item(i)
            attrs[item.nodeName.uppercase()] = item.nodeValue
        }

        val id = attrs["ID"].orEmpty().ifBlank { MindMapNode.newId() }
        mmxById[id]?.let { mmxNode ->
            val mmxAttrs = mmxNode.attributes
            for (i in 0 until mmxAttrs.length) {
                val item = mmxAttrs.item(i)
                attrs[item.nodeName.uppercase()] = item.nodeValue
            }
        }

        val knownKeys = setOf(
            "ID", "TEXT", "FOLDED", "POSITION", "CREATED", "MODIFIED",
            "COLOR", "BACKGROUND_COLOR", "STYLE", "LINK",
            "HGAP", "VGAP", "VSHIFT", "ENCRYPTED_CONTENT",
        )
        val unknownAttributes = attrs.filterKeys { it !in knownKeys }

        val children = mutableListOf<MindMapNode>()
        val icons = mutableListOf<String>()
        val attributes = mutableListOf<NodeAttribute>()
        val unknownChildren = mutableListOf<RawXmlElement>()
        var noteHtml: String? = null
        var richContentHtml: String? = null

        for (child in element.elementChildren()) {
            when (child.tagName.lowercase()) {
                "node" -> children += parseNode(child, mmxById)
                "icon" -> {
                    val builtin = child.getAttribute("BUILTIN")
                    if (builtin.isNotBlank()) icons += builtin
                    else unknownChildren += toRawXml(child)
                }
                "attribute" -> attributes += NodeAttribute(
                    name = child.getAttribute("NAME"),
                    value = child.getAttribute("VALUE"),
                )
                "richcontent" -> {
                    val type = child.getAttribute("TYPE").uppercase()
                    val html = extractInnerXml(child)
                    when (type) {
                        "NOTE" -> noteHtml = html
                        "NODE", "" -> richContentHtml = html
                        else -> unknownChildren += toRawXml(child)
                    }
                }
                // Preserve for later milestones / round-trip; not modeled deeply yet.
                "edge", "cloud", "font", "arrowlink", "linktarget", "hook",
                "attribute_layout",
                -> unknownChildren += toRawXml(child)
                else -> unknownChildren += toRawXml(child)
            }
        }

        val text = attrs["TEXT"].orEmpty()
        val folded = attrs["FOLDED"].equals("true", ignoreCase = true)
        val side = when (attrs["POSITION"]?.lowercase()) {
            "left" -> NodeSide.LEFT
            "right" -> NodeSide.RIGHT
            else -> null
        }

        return MindMapNode(
            id = id,
            text = text,
            children = children,
            side = side,
            folded = folded,
            createdAtMillis = attrs["CREATED"]?.toLongOrNull(),
            modifiedAtMillis = attrs["MODIFIED"]?.toLongOrNull(),
            style = attrs["STYLE"],
            color = attrs["COLOR"],
            backgroundColor = attrs["BACKGROUND_COLOR"],
            link = attrs["LINK"],
            icons = icons,
            noteHtml = noteHtml,
            richContentHtml = richContentHtml,
            attributes = attributes,
            unknownAttributes = unknownAttributes,
            unknownChildren = unknownChildren,
        )
    }

    private fun toRawXml(element: Element): RawXmlElement {
        val attributes = linkedMapOf<String, String>()
        val named = element.attributes
        for (i in 0 until named.length) {
            val item = named.item(i)
            attributes[item.nodeName] = item.nodeValue
        }
        val children = mutableListOf<RawXmlElement>()
        val textParts = StringBuilder()
        val nodes = element.childNodes
        for (i in 0 until nodes.length) {
            when (val n = nodes.item(i)) {
                is Element -> children += toRawXml(n)
                else -> if (n.nodeType == Node.TEXT_NODE || n.nodeType == Node.CDATA_SECTION_NODE) {
                    textParts.append(n.nodeValue)
                }
            }
        }
        val text = textParts.toString().takeIf { it.isNotBlank() }
        return RawXmlElement(
            name = element.tagName,
            attributes = attributes,
            children = children,
            text = text,
        )
    }

    private fun extractInnerXml(element: Element): String {
        val builder = StringBuilder()
        val children = element.childNodes
        for (i in 0 until children.length) {
            builder.append(serializeNode(children.item(i)))
        }
        return builder.toString().trim()
    }

    private fun serializeNode(node: Node): String =
        when (node.nodeType) {
            Node.ELEMENT_NODE -> {
                val el = node as Element
                buildString {
                    append('<').append(el.tagName)
                    val attrs = el.attributes
                    for (i in 0 until attrs.length) {
                        val a = attrs.item(i)
                        append(' ')
                            .append(a.nodeName)
                            .append("=\"")
                            .append(xmlEscape(a.nodeValue))
                            .append('"')
                    }
                    if (!el.hasChildNodes()) {
                        append("/>")
                    } else {
                        append('>')
                        val kids = el.childNodes
                        for (i in 0 until kids.length) {
                            append(serializeNode(kids.item(i)))
                        }
                        append("</").append(el.tagName).append('>')
                    }
                }
            }
            Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> node.nodeValue.orEmpty()
            Node.COMMENT_NODE -> "<!--${node.nodeValue}-->"
            else -> ""
        }

    private fun xmlEscape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}

class FreeMindParseException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

private fun Element.elementChildren(): List<Element> {
    val result = mutableListOf<Element>()
    val children = childNodes
    for (i in 0 until children.length) {
        val n = children.item(i)
        if (n is Element) result += n
    }
    return result
}
