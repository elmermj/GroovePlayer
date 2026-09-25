package com.aethelsoft.grooveplayer.data.library

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.aethelsoft.grooveplayer.domain.backup.ContentHash
import com.aethelsoft.grooveplayer.domain.library.ImportByteSource
import com.aethelsoft.grooveplayer.domain.library.LibraryAudioKinds
import com.aethelsoft.grooveplayer.domain.library.MediaStoreAudioRow
import com.aethelsoft.grooveplayer.domain.library.MediaStoreOriginalMatch
import java.io.InputStream

data class ListedAudioDocument(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
)

object SafAudioTree {
    fun listAudio(resolver: ContentResolver, treeUri: Uri): List<ListedAudioDocument> {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val root = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val pending = ArrayDeque<Uri>()
        pending.add(root)
        val found = mutableListOf<ListedAudioDocument>()
        val seen = HashSet<String>()
        while (pending.isNotEmpty()) {
            val directory = pending.removeFirst()
            val directoryId = DocumentsContract.getDocumentId(directory)
            if (!seen.add(directoryId)) continue
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryId)
            resolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    if (idCol < 0 || nameCol < 0) continue
                    val id = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val size = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                    val document = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.add(document)
                    } else if (LibraryAudioKinds.isAudio(name, mime)) {
                        found += ListedAudioDocument(document, name, size.coerceAtLeast(0L))
                    }
                }
            }
        }
        return found
    }

    fun open(resolver: ContentResolver, document: ListedAudioDocument): ImportByteSource {
        return object : ImportByteSource {
            override val displayName = document.displayName
            override val sizeBytes = document.sizeBytes
            override val stableKey = document.uri.toString()
            override fun open(): InputStream {
                return resolver.openInputStream(document.uri)
                    ?: error("Could not read ${document.displayName}")
            }
        }
    }

    /**
     * MediaStore row for an imported original. Name and size are not enough: the row's folder
     * must be the picked tree, and a known hash must match. Anything else returns null so the
     * caller deletes through the granted SAF tree.
     */
    fun resolveMediaStoreAudio(
        resolver: ContentResolver,
        displayName: String,
        sizeBytes: Long,
        pickedTreeDocumentId: String,
        importedSha256: String?,
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        if (displayName.isBlank() || sizeBytes <= 0L || pickedTreeDocumentId.isBlank()) return null
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.VOLUME_NAME,
        )
        val rows = runCatching {
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.DISPLAY_NAME}=? AND ${MediaStore.Audio.Media.SIZE}=?",
                arrayOf(displayName, sizeBytes.toString()),
                null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                val volumeCol = cursor.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME)
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val relative = if (pathCol >= 0 && !cursor.isNull(pathCol)) cursor.getString(pathCol) else null
                        val volume = if (volumeCol >= 0 && !cursor.isNull(volumeCol)) cursor.getString(volumeCol) else null
                        add(MediaStoreAudioRow(id = id, relativePath = relative, volumeName = volume))
                    }
                }
            }
        }.getOrNull() ?: return null
        val confirmed = rows.map { row ->
            if (importedSha256.isNullOrBlank()) return@map row
            if (!MediaStoreOriginalMatch.folderMatches(row.relativePath, pickedTreeDocumentId)) return@map row
            if (!MediaStoreOriginalMatch.volumeMatches(row.volumeName, pickedTreeDocumentId)) return@map row
            val uri = android.content.ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                row.id,
            )
            row.copy(contentHash = hash(resolver, uri))
        }
        val id = MediaStoreOriginalMatch.uniqueId(confirmed, pickedTreeDocumentId, importedSha256)
            ?: return null
        return android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }

    private fun hash(resolver: ContentResolver, uri: Uri): String? {
        return runCatching {
            resolver.openInputStream(uri)?.use { ContentHash.sha256(it) }
        }.getOrNull()
    }
}
