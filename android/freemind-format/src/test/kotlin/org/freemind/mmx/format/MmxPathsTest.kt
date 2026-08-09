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

class FreeMindFormatSmokeTest {
    @Test
    fun writeThenParsePlainMap() {
        val format = FreeMindFormat()
        val map = org.freemind.mmx.core.MindMap.blank("Root")
        val xml = format.writeMm(map, WriteOptions(separateVolatileAttributes = false))
        assertTrue(xml.contains("<map"))
        assertTrue(xml.contains("TEXT=\"Root\""))
        val parsed = format.parseMm(xml)
        assertEquals("Root", parsed.root.text)
    }
}
