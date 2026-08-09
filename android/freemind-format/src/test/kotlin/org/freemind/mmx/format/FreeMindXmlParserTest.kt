package org.freemind.mmx.format

import org.freemind.mmx.core.NodeSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class FreeMindXmlParserTest {
    private val parser = FreeMindXmlParser()
    private val format = FreeMindFormat(parser)

    @Test
    fun parsesBasicHierarchy() {
        val map = format.parseMm(fixture("basic-hierarchy.mm"))
        assertEquals("1.0.1", map.version)
        assertEquals("root", map.root.id)
        assertEquals("Root", map.root.text)
        assertEquals(2, map.root.children.size)

        val left = map.root.children[0]
        assertEquals(NodeSide.LEFT, left.side)
        assertEquals("Left branch", left.text)
        assertEquals(1, left.children.size)
        assertEquals("Child A", left.children[0].text)

        val right = map.root.children[1]
        assertEquals(NodeSide.RIGHT, right.side)
        assertTrue(right.folded)
        assertEquals("#ff0000", right.color)
    }

    @Test
    fun joinsMmxFoldAndTimestampsById() {
        val map = format.parseMm(
            fixture("mmx-content.mm"),
            fixture("mmx-content.mmx"),
        )
        // .mm forces FOLDED=true on branches; .mmx restores real state.
        val left = map.root.children.first { it.id == "left1" }
        val right = map.root.children.first { it.id == "right1" }
        assertFalse(left.folded)
        assertTrue(right.folded)
        assertEquals(11L, left.createdAtMillis)
        assertEquals(21L, left.modifiedAtMillis)
        assertEquals(12L, right.createdAtMillis)
        assertFalse(map.root.folded)
        assertEquals(10L, map.root.createdAtMillis)
    }

    @Test
    fun withoutMmxUsesMmFoldState() {
        val map = format.parseMm(fixture("mmx-content.mm"))
        assertTrue(map.root.children.all { it.folded })
        assertNull(map.root.createdAtMillis)
    }

    @Test
    fun parsesUnicodeIconsNotesAndUnknownData() {
        val map = format.parseMm(fixture("unicode-and-extras.mm"))
        assertTrue(map.root.text.contains("привет"))
        assertTrue(map.root.text.contains("你好"))
        assertTrue(map.root.text.contains("🗺️"))
        assertEquals("https://example.com", map.root.link)

        val styled = map.root.children[0]
        assertEquals("bubble", styled.style)
        assertEquals("#eeeeee", styled.backgroundColor)
        assertEquals(listOf("idea", "yes"), styled.icons)
        assertEquals(listOf(org.freemind.mmx.core.NodeAttribute("priority", "high")), styled.attributes)
        assertNotNull(styled.noteHtml)
        assertTrue(styled.noteHtml!!.contains("Note line"))

        val unknown = map.root.children[1]
        assertEquals("keep-me", unknown.unknownAttributes["CUSTOM_ATTR"])
        assertTrue(unknown.unknownChildren.any { it.name.equals("custom_extension", ignoreCase = true) })
        assertTrue(unknown.text.contains("multiline"))
    }

    @Test
    fun parsesLegacyAutomatedTestMap() {
        val map = format.parseMm(fixture("legacy-testmap.mm"))
        assertEquals("1.0.0", map.version)
        assertEquals("Freemind_Link_140245201", map.root.id)
        assertTrue(map.root.richContentHtml != null || map.root.text.isEmpty())
        assertNotNull(map.root.richContentHtml)
        assertTrue(map.root.richContentHtml!!.contains("Test"))

        val noteNode = map.root.children.first { it.text == "Notetest" }
        assertNotNull(noteNode.noteHtml)
        assertTrue(noteNode.noteHtml!!.contains("note"))

        val foldedParent = map.root.children
            .first { it.text == "This is a node" }
            .children
            .first { it.text == "and some folded subnodes" }
        assertTrue(foldedParent.folded)
        assertEquals(3, foldedParent.children.size)

        val attrsNode = map.root.children.first { it.text == "Attributes" }
        assertEquals("attributeName", attrsNode.attributes.single().name)
        assertEquals("attributeValue", attrsNode.attributes.single().value)

        // attribute_registry preserved as unknown map-level element
        assertTrue(map.unknownElements.any { it.name == "attribute_registry" })
    }

    @Test
    fun rejectsNonMapRoot() {
        assertFailsWith<FreeMindParseException> {
            format.parseMm("<notamap><node TEXT=\"x\"/></notamap>")
        }
    }

    @Test
    fun mmxSidecarNamingMatchesLegacy() {
        assertEquals(".MyMap.mmx", MmxPaths.sidecarFileName("MyMap.mm"))
    }

    private fun fixture(name: String): String {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name")) {
            "Missing fixture fixtures/$name"
        }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
