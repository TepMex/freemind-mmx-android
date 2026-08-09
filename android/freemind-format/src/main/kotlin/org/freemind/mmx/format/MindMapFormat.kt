package org.freemind.mmx.format

import org.freemind.mmx.core.MindMap

/**
 * FreeMind `.mm` / FreeMind-MMX `.mmx` parse and write facade.
 *
 * Milestone 2 will implement real XML parsing derived from legacy
 * `XMLElementAdapter` / `NodeAdapter` behavior. Milestone 1 only
 * exposes the intended API surface and MMX naming helpers.
 */
interface MindMapFormat {
    fun parseMm(xml: String, mmxXml: String? = null): MindMap
    fun writeMm(map: MindMap, options: WriteOptions = WriteOptions()): String
    fun writeMmx(map: MindMap, options: WriteOptions = WriteOptions()): String
}

data class WriteOptions(
    /** Match FreeMind-MMX: put FOLDED/CREATED/MODIFIED into `.mmx`. */
    val separateVolatileAttributes: Boolean = true,
    /** Write printable non-ASCII as UTF-8 rather than numeric entities. */
    val utf8WithoutEntities: Boolean = true,
    /** When null, the writer keeps [MindMap.version]. */
    val mapVersion: String? = null,
)

object MmxPaths {
    /**
     * Classic FreeMind-MMX sidecar name for a primary map file name.
     *
     * - `MyMap.mm` → `.MyMap.mmx`
     * - `backup.xml` → `.backup.xml.mmx`
     *
     * Matches legacy `MindMapMapModel.saveInternal` / `Tools.getActualReader`.
     */
    fun sidecarFileName(primaryFileName: String): String =
        if (primaryFileName.endsWith(".mm", ignoreCase = true)) {
            "." + primaryFileName.dropLast(3) + ".mmx"
        } else {
            ".$primaryFileName.mmx"
        }
}

/**
 * Stub format implementation for Milestone 1 skeleton builds/tests.
 * Real reader/writer arrives in Milestones 2 and 5.
 */
class StubMindMapFormat : MindMapFormat {
    override fun parseMm(xml: String, mmxXml: String?): MindMap {
        require(xml.contains("<map")) { "Not a FreeMind map document" }
        return MindMap.blank("Parsed stub — implement Milestone 2")
    }

    override fun writeMm(map: MindMap, options: WriteOptions): String =
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<map version="${options.mapVersion}">""")
            appendLine("<!-- FreeMind-MMX Android stub writer (Milestone 1) -->")
            appendLine("""<node ID="${escapeXml(map.root.id)}" TEXT="${escapeXml(map.root.text)}"/>""")
            appendLine("</map>")
        }

    override fun writeMmx(map: MindMap, options: WriteOptions): String =
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<map version="${options.mapVersion}">""")
            appendLine("<!-- FreeMind-MMX sidecar stub (Milestone 1) -->")
            appendLine("""<node ID="${escapeXml(map.root.id)}" FOLDED="false"/>""")
            appendLine("</map>")
        }

    private fun escapeXml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
