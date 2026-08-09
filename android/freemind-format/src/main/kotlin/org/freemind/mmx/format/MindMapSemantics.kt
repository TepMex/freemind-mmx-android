package org.freemind.mmx.format

import org.freemind.mmx.core.MindMap
import org.freemind.mmx.core.MindMapNode
import org.freemind.mmx.core.NodeAttribute
import org.freemind.mmx.core.RawXmlElement

/**
 * Semantic comparison for FreeMind maps — ignores XML attribute order / whitespace.
 */
object MindMapSemantics {
    fun assertEquivalent(expected: MindMap, actual: MindMap, message: String = "Mind maps differ") {
        check(expected.version == actual.version) {
            "$message: version ${expected.version} vs ${actual.version}"
        }
        assertNodeEquivalent(expected.root, actual.root, path = "root", message)
        check(expected.unknownElements.size == actual.unknownElements.size) {
            "$message: unknownElements size ${expected.unknownElements.size} vs ${actual.unknownElements.size}"
        }
        expected.unknownElements.zip(actual.unknownElements).forEachIndexed { i, (a, b) ->
            assertRawEquivalent(a, b, "unknownElements[$i]", message)
        }
    }

    fun equivalent(a: MindMap, b: MindMap): Boolean =
        runCatching { assertEquivalent(a, b) }.isSuccess

    private fun assertNodeEquivalent(
        expected: MindMapNode,
        actual: MindMapNode,
        path: String,
        message: String,
    ) {
        fun checkField(name: String, e: Any?, a: Any?) {
            check(e == a) { "$message at $path.$name: <$e> vs <$a>" }
        }
        checkField("id", expected.id, actual.id)
        checkField("text", expected.text, actual.text)
        checkField("side", expected.side, actual.side)
        checkField("folded", expected.folded, actual.folded)
        checkField("createdAtMillis", expected.createdAtMillis, actual.createdAtMillis)
        checkField("modifiedAtMillis", expected.modifiedAtMillis, actual.modifiedAtMillis)
        checkField("style", expected.style, actual.style)
        checkField("color", expected.color, actual.color)
        checkField("backgroundColor", expected.backgroundColor, actual.backgroundColor)
        checkField("link", expected.link, actual.link)
        checkField("icons", expected.icons, actual.icons)
        checkField("noteHtml", normalizeHtml(expected.noteHtml), normalizeHtml(actual.noteHtml))
        checkField(
            "richContentHtml",
            normalizeHtml(expected.richContentHtml),
            normalizeHtml(actual.richContentHtml),
        )
        checkField("attributes", expected.attributes, actual.attributes)
        checkField("unknownAttributes", expected.unknownAttributes, actual.unknownAttributes)
        check(expected.children.size == actual.children.size) {
            "$message at $path.children size ${expected.children.size} vs ${actual.children.size}"
        }
        expected.children.zip(actual.children).forEachIndexed { i, (e, a) ->
            assertNodeEquivalent(e, a, "$path/children[$i]", message)
        }
        check(expected.unknownChildren.size == actual.unknownChildren.size) {
            "$message at $path.unknownChildren size"
        }
        expected.unknownChildren.zip(actual.unknownChildren).forEachIndexed { i, (e, a) ->
            assertRawEquivalent(e, a, "$path.unknownChildren[$i]", message)
        }
    }

    private fun assertRawEquivalent(
        expected: RawXmlElement,
        actual: RawXmlElement,
        path: String,
        message: String,
    ) {
        check(expected.name.equals(actual.name, ignoreCase = true)) {
            "$message at $path.name"
        }
        check(expected.attributes == actual.attributes) { "$message at $path.attributes" }
        check(normalizeHtml(expected.text) == normalizeHtml(actual.text)) {
            "$message at $path.text"
        }
        check(expected.children.size == actual.children.size) { "$message at $path.children" }
        expected.children.zip(actual.children).forEachIndexed { i, (e, a) ->
            assertRawEquivalent(e, a, "$path/$i", message)
        }
    }

    private fun normalizeHtml(value: String?): String? =
        value?.replace(Regex("\\s+"), " ")?.trim()
}
