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
    fun stubWriterStillEmitsMapXml() {
        val stub = StubMindMapFormat()
        val map = org.freemind.mmx.core.MindMap.blank("Root")
        val xml = stub.writeMm(map)
        assertTrue(xml.contains("<map"))
        assertTrue(xml.contains("TEXT=\"Root\""))
    }

    @Test
    fun productionFormatParsesWrittenStub() {
        val format = FreeMindFormat()
        val map = org.freemind.mmx.core.MindMap.blank("Root")
        val xml = format.writeMm(map)
        val parsed = format.parseMm(xml)
        assertEquals("Root", parsed.root.text)
    }
}
