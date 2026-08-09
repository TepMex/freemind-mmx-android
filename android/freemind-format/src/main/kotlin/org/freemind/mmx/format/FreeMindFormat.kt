package org.freemind.mmx.format

import org.freemind.mmx.core.MindMap

/**
 * Production [MindMapFormat] backed by [FreeMindXmlParser].
 * Writer remains stub-quality until Milestone 5.
 */
class FreeMindFormat(
    private val parser: FreeMindXmlParser = FreeMindXmlParser(),
    private val stubWriter: StubMindMapFormat = StubMindMapFormat(),
) : MindMapFormat {
    override fun parseMm(xml: String, mmxXml: String?): MindMap =
        parser.parse(xml, mmxXml)

    override fun writeMm(map: MindMap, options: WriteOptions): String =
        stubWriter.writeMm(map, options)

    override fun writeMmx(map: MindMap, options: WriteOptions): String =
        stubWriter.writeMmx(map, options)
}
