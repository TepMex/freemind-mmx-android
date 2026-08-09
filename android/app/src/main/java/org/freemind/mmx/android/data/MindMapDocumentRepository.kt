package org.freemind.mmx.android.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.freemind.mmx.core.MindMap
import org.freemind.mmx.format.FreeMindFormat
import org.freemind.mmx.format.MmxPaths
import org.freemind.mmx.format.WriteOptions
import java.io.File

data class OpenedDocument(
    val map: MindMap,
    val title: String,
    val mmUri: Uri? = null,
    val mmxUri: Uri? = null,
    val mmxWarning: String? = null,
)

data class SaveResult(
    val mmUri: Uri,
    val mmxUri: Uri?,
    val title: String,
    val warning: String? = null,
)

/**
 * Storage Access Framework helpers for `.mm` / `.mmx` documents.
 */
class MindMapDocumentRepository(
    private val context: Context,
    private val format: FreeMindFormat = FreeMindFormat(),
) {
    suspend fun open(uri: Uri, takePermission: Boolean = true): OpenedDocument =
        withContext(Dispatchers.IO) {
            if (takePermission) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
            val mmXml = readText(uri)
            val title = displayName(uri) ?: "Opened map"
            val mmxUri = findSidecarUri(uri, title)
            val mmxXml = mmxUri?.let { runCatching { readText(it) }.getOrNull() }
            val warning = when {
                mmxUri == null -> "No .mmx sidecar found; fold/timestamps may reflect .mm defaults."
                mmxXml == null -> "Found .mmx URI but could not read it."
                else -> null
            }
            OpenedDocument(
                map = format.parseMm(mmXml, mmxXml),
                title = title,
                mmUri = uri,
                mmxUri = mmxUri,
                mmxWarning = warning,
            )
        }

    suspend fun save(
        map: MindMap,
        mmUri: Uri,
        mmxUri: Uri?,
        options: WriteOptions = WriteOptions(separateVolatileAttributes = true),
    ): SaveResult =
        withContext(Dispatchers.IO) {
            val mmXml = format.writeMm(map, options)
            val mmxXml = format.writeMmx(map, options)
            writeTextAtomic(mmUri, mmXml)

            var resolvedMmx = mmxUri
            var warning: String? = null

            if (options.separateVolatileAttributes) {
                if (resolvedMmx != null) {
                    writeTextAtomic(resolvedMmx, mmxXml)
                } else {
                    val created = tryCreateSidecar(mmUri, displayName(mmUri) ?: "map.mm", mmxXml)
                    if (created != null) {
                        resolvedMmx = created
                    } else {
                        // Keep volatile state in app cache so it is not silently discarded.
                        cacheMmx(mmUri, mmxXml)
                        warning = "Saved .mm; could not write sibling .mmx. " +
                            "Fold/timestamps cached in-app — use Save MMX… to pick a sidecar location."
                    }
                }
            }

            SaveResult(
                mmUri = mmUri,
                mmxUri = resolvedMmx,
                title = displayName(mmUri) ?: "map.mm",
                warning = warning,
            )
        }

    fun writeMmxOnly(mmxUri: Uri, map: MindMap) {
        val xml = format.writeMmx(map)
        writeTextAtomic(mmxUri, xml)
    }

    fun cachedMmx(mmUri: Uri): String? {
        val file = cacheFile(mmUri)
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    private fun cacheMmx(mmUri: Uri, mmxXml: String) {
        cacheFile(mmUri).writeText(mmxXml, Charsets.UTF_8)
    }

    private fun cacheFile(mmUri: Uri): File {
        val dir = File(context.filesDir, "mmx-cache").apply { mkdirs() }
        val key = Integer.toHexString(mmUri.toString().hashCode())
        return File(dir, "$key.mmx")
    }

    private fun readText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Unable to read $uri")

    private fun writeTextAtomic(uri: Uri, text: String) {
        // Provider-safe: write full payload to the destination stream.
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)
            ?.use { it.write(text) }
            ?: error("Unable to write $uri")
    }

    private fun displayName(uri: Uri): String? {
        DocumentFile.fromSingleUri(context, uri)?.name?.let { return it }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun findSidecarUri(mmUri: Uri, title: String): Uri? {
        val sidecarName = MmxPaths.sidecarFileName(title)
        // file:// path
        if (mmUri.scheme == "file") {
            val file = File(mmUri.path ?: return null)
            val sibling = File(file.parentFile, sidecarName)
            if (sibling.exists()) return Uri.fromFile(sibling)
        }
        // DocumentFile sibling in the same folder when available
        val doc = DocumentFile.fromSingleUri(context, mmUri) ?: return null
        val parent = doc.parentFile ?: return cacheFile(mmUri).takeIf { it.exists() }?.let { Uri.fromFile(it) }
        return parent.findFile(sidecarName)?.uri
            ?: parent.findFile(sidecarName.removePrefix("."))?.uri
    }

    private fun tryCreateSidecar(mmUri: Uri, title: String, mmxXml: String): Uri? {
        val sidecarName = MmxPaths.sidecarFileName(title)
        if (mmUri.scheme == "file") {
            val file = File(mmUri.path ?: return null)
            val sibling = File(file.parentFile, sidecarName)
            sibling.writeText(mmxXml, Charsets.UTF_8)
            return Uri.fromFile(sibling)
        }
        val doc = DocumentFile.fromSingleUri(context, mmUri) ?: return null
        val parent = doc.parentFile ?: return null
        val existing = parent.findFile(sidecarName)
        val target = existing
            ?: parent.createFile("application/xml", sidecarName)
            ?: parent.createFile("text/xml", sidecarName.removePrefix("."))
            ?: return null
        writeTextAtomic(target.uri, mmxXml)
        return target.uri
    }
}
