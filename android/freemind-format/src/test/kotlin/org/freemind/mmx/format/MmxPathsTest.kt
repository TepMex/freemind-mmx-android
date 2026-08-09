package org.freemind.mmx.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MmxPathsTest {
    @Test
    fun sidecarNameForMmFile() {
        assertEquals(".MyMap.mmx", MmxPaths.sidecarFileName("MyMap.mm"))
        assertEquals(".notes.mmx", MmxPaths.sidecarFileName("notes.MM"))
    }

    @Test
    fun sidecarNameForNonMmFile() {
        assertEquals(".backup.xml.mmx", MmxPaths.sidecarFileName("backup.xml"))
    }
}

class StubMindMapFormatTest {
    @Test
    fun stubRoundTripProducesMapXml() {
        val format = StubMindMapFormat()
        val map = org.freemind.mmx.core.MindMap.blank("Root")
        val xml = format.writeMm(map)
        assertTrue(xml.contains("<map"))
        assertTrue(xml.contains("TEXT=\"Root\""))
        val parsed = format.parseMm(xml)
        assertEquals("Parsed stub — implement Milestone 2", parsed.root.text)
    }
}
