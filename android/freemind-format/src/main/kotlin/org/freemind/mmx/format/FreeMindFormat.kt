package org.freemind.mmx.format

import org.freemind.mmx.core.MindMap

/**
 * Production FreeMind `.mm` / `.mmx` format facade.
 */
class FreeMindFormat(
    private val parser: FreeMindXmlParser = FreeMindXmlParser(),
    private val writer: FreeMindXmlWriter = FreeMindXmlWriter(),
) : MindMapFormat {
    override fun parseMm(xml: String, mmxXml: String?): MindMap =
        parser.parse(xml, mmxXml)

    override fun writeMm(map: MindMap, options: WriteOptions): String =
        writer.writeMm(map, options)

    override fun writeMmx(map: MindMap, options: WriteOptions): String =
        writer.writeMmx(map, options)
}
