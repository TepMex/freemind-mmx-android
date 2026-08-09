package org.freemind.mmx.format

import org.freemind.mmx.core.MindMap
import org.freemind.mmx.core.MindMapNode
import org.freemind.mmx.core.NodeAttribute
import org.freemind.mmx.core.NodeSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreeMindXmlWriterTest {
    private val format = FreeMindFormat()

    @Test
    fun vanillaRoundTripPreservesSemantics() {
        val original = format.parseMm(fixture("unicode-and-extras.mm"))
        val xml = format.writeMm(original, WriteOptions(separateVolatileAttributes = false))
        val again = format.parseMm(xml)
        MindMapSemantics.assertEquivalent(original, again)
    }

    @Test
    fun mmxPairRoundTripPreservesFoldAndTimestamps() {
        val original = format.parseMm(fixture("basic-hierarchy.mm"))
        val mm = format.writeMm(original, WriteOptions(separateVolatileAttributes = true))
        val mmx = format.writeMmx(original, WriteOptions(separateVolatileAttributes = true))
        assertTrue(mm.contains("FOLDED=\"true\""))
        assertFalse(mm.contains("CREATED="))
        assertTrue(mmx.contains("CREATED="))
        assertTrue(mmx.contains("FOLDED="))
        assertFalse(mmx.contains("TEXT="))

        val joined = format.parseMm(mm, mmx)
        MindMapSemantics.assertEquivalent(original, joined)
    }

    @Test
    fun legacyTestMapRoundTripWithMmx() {
        val original = format.parseMm(fixture("legacy-testmap.mm"))
        val mm = format.writeMm(original, WriteOptions(separateVolatileAttributes = true))
        val mmx = format.writeMmx(original)
        val joined = format.parseMm(mm, mmx)
        MindMapSemantics.assertEquivalent(original, joined)
    }

    @Test
    fun writesUtf8WithoutNumericEntities() {
        val map = MindMap(
            root = MindMapNode(
                id = "r",
                text = "привет 你好",
                children = listOf(
                    MindMapNode(id = "c", text = "café", side = NodeSide.RIGHT),
                ),
            ),
        )
        val xml = format.writeMm(map, WriteOptions(separateVolatileAttributes = false, utf8WithoutEntities = true))
        assertTrue(xml.contains("привет"))
        assertTrue(xml.contains("你好"))
        assertTrue(xml.contains("café"))
        assertFalse(xml.contains("&#x"))
    }

    @Test
    fun preservesUnknownAttributesAndElements() {
        val original = format.parseMm(fixture("unicode-and-extras.mm"))
        val unknown = original.root.children.first { it.id == "u3" }
        assertEquals("keep-me", unknown.unknownAttributes["CUSTOM_ATTR"])
        val xml = format.writeMm(original, WriteOptions(separateVolatileAttributes = false))
        val again = format.parseMm(xml)
        val u3 = again.root.children.first { it.id == "u3" }
        assertEquals("keep-me", u3.unknownAttributes["CUSTOM_ATTR"])
        assertTrue(u3.unknownChildren.any { it.name.equals("custom_extension", ignoreCase = true) })
    }

    @Test
    fun openingMmxContentWithoutSidecarShowsForcedFold() {
        val map = format.parseMm(fixture("mmx-content.mm"))
        assertTrue(map.root.children.all { it.folded })
        val joined = format.parseMm(fixture("mmx-content.mm"), fixture("mmx-content.mmx"))
        assertFalse(joined.root.children.first { it.id == "left1" }.folded)
        assertTrue(joined.root.children.first { it.id == "right1" }.folded)
    }

    @Test
    fun editedMapRoundTrips() {
        val map = MindMap(
            root = MindMapNode(
                id = "r",
                text = "Root",
                createdAtMillis = 1,
                modifiedAtMillis = 2,
                children = listOf(
                    MindMapNode(
                        id = "a",
                        text = "Child",
                        side = NodeSide.RIGHT,
                        icons = listOf("idea"),
                        attributes = listOf(NodeAttribute("k", "v")),
                        link = "https://example.com",
                        createdAtMillis = 3,
                        modifiedAtMillis = 4,
                        folded = true,
                        children = listOf(MindMapNode(id = "a1", text = "Grand", folded = false)),
                    ),
                ),
            ),
        )
        val joined = format.parseMm(format.writeMm(map), format.writeMmx(map))
        MindMapSemantics.assertEquivalent(map, joined)
    }

    private fun fixture(name: String): String {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name")) {
            "Missing fixture fixtures/$name"
        }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
