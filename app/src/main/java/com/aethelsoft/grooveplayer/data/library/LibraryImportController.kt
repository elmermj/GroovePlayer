package com.aethelsoft.grooveplayer.data.library

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.DocumentsContract
import com.aethelsoft.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelsoft.grooveplayer.data.local.db.dao.SongDao
import com.aethelsoft.grooveplayer.domain.backup.ContentHash
import com.aethelsoft.grooveplayer.domain.library.ImportFileResult
import com.aethelsoft.grooveplayer.domain.library.ImportFileStatus
import com.aethelsoft.grooveplayer.domain.library.ImportOriginalDeletion
import com.aethelsoft.grooveplayer.domain.library.ImportPromptCopy
import com.aethelsoft.grooveplayer.domain.library.LibraryFileCopy
import com.aethelsoft.grooveplayer.domain.library.LibraryImporter
import com.aethelsoft.grooveplayer.domain.library.OriginalDeleteChoice
import com.aethelsoft.grooveplayer.domain.library.SongHashRemap
import com.aethelsoft.grooveplayer.domain.library.folderDisplayName
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.cloudObjectMatchesSong
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LibraryImportUiState {
    data object Idle : LibraryImportUiState
    data class Running(val completed: Int, val total: Int, val fileName: String) : LibraryImportUiState
    data class NeedsDeleteDecision(
        val copied: Int,
        val alreadyInLibrary: Int,
        val failed: Int,
        val folderName: String,
        val body: String,
        val note: String?,
    ) : LibraryImportUiState
    data class NeedsSystemDelete(val mediaStoreUris: List<Uri>) : LibraryImportUiState
    data class Finished(val message: String) : LibraryImportUiState
}

@Singleton
class LibraryImportController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identity: SongIdentityStore,
    private val songDao: SongDao,
    private val grooveDownloads: GrooveDownloadsLocator,
    private val musicRepository: MusicRepository,
    private val backupRepository: BackupRepository,
) {
    private val gate = Mutex()
    private val cancel = AtomicBoolean(false)
    private val _state = MutableStateFlow<LibraryImportUiState>(LibraryImportUiState.Idle)
    val state: StateFlow<LibraryImportUiState> = _state.asStateFlow()

    private var eligibleOriginals: List<ImportFileResult> = emptyList()
    private var pickedTreeDocumentId: String = ""
    private var documentDeleted = 0
    private var documentFailed = 0

    fun requestCancel() {
        cancel.set(true)
    }

    fun dismiss() {
        eligibleOriginals = emptyList()
        documentDeleted = 0
        documentFailed = 0
        _state.value = LibraryImportUiState.Idle
    }

    /** Keep, back, or outside tap. Nothing is deleted. */
    fun keepOriginals() {
        val current = _state.value as? LibraryImportUiState.NeedsDeleteDecision
        eligibleOriginals = emptyList()
        _state.value = LibraryImportUiState.Finished(
            current?.let { summaryLine(it.copied, it.alreadyInLibrary, it.failed, it.note) }
                ?: "Import finished. Original files were kept.",
        )
    }

    suspend fun run(treeUri: Uri) {
        if (!gate.tryLock()) return
        try {
            cancel.set(false)
            eligibleOriginals = emptyList()
            pickedTreeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrDefault("")
            val root = grooveDownloads.directory()
            root.mkdirs()
            val documents = withContext(Dispatchers.IO) {
                SafAudioTree.listAudio(context.contentResolver, treeUri)
            }
            val known = knownPrivateHashes(root)
            val free = { freeBytes(root) }
            if (!LibraryImporter.hasRoomFor(documents.sumOf { it.sizeBytes }, free())) {
                _state.value = LibraryImportUiState.Finished(LibraryFileCopy.OUT_OF_SPACE)
                return
            }
            val result = withContext(Dispatchers.IO) {
                LibraryImporter.importAll(
                    sources = documents.map { SafAudioTree.open(context.contentResolver, it) },
                    root = root,
                    knownPrivateHashes = known,
                    freeBytes = free,
                    cancelled = { cancel.get() },
                    onProgress = { index, total, name ->
                        _state.value = LibraryImportUiState.Running(
                            completed = (index + 1).coerceAtMost(total.coerceAtLeast(1)),
                            total = total,
                            fileName = name,
                        )
                    },
                )
            }
            for (file in result.files) {
                if (file.status != ImportFileStatus.COPIED || file.libraryPath == null || file.sha256 == null) continue
                val copied = File(file.libraryPath)
                val tags = readTags(copied)
                saveArtwork(root, file.sha256, tags.artwork)
                indexHashForSize(file.sha256, file.sizeBytes)
                identity.adoptVerifiedCopy(
                    hash = file.sha256,
                    libraryPath = copied.absolutePath,
                    title = tags.title,
                    durationMs = tags.durationMs,
                )
            }
            musicRepository.bumpCatalogGeneration()
            val folder = folderDisplayName(
                runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrDefault(treeUri.toString()),
            )
            val note = result.stoppedReason
            if (result.cancelled && result.copied == 0 && result.alreadyInLibrary == 0) {
                _state.value = LibraryImportUiState.Finished("Import cancelled.")
                return
            }
            if (ImportOriginalDeletion.shouldPrompt(result.copied, result.alreadyInLibrary)) {
                eligibleOriginals = ImportOriginalDeletion.eligibleKeys(result.files)
                _state.value = LibraryImportUiState.NeedsDeleteDecision(
                    copied = result.copied,
                    alreadyInLibrary = result.alreadyInLibrary,
                    failed = result.failed.size,
                    folderName = folder,
                    body = ImportPromptCopy.body(result.copied, folder),
                    note = note,
                )
            } else {
                _state.value = LibraryImportUiState.Finished(
                    summaryLine(result.copied, result.alreadyInLibrary, result.failed.size, note ?: failureReasons(result.files)),
                )
            }
        } finally {
            gate.unlock()
        }
    }

    /**
     * User tapped Delete originals. Re-checks hashes, deletes SAF documents, and
     * returns MediaStore URIs that still need the system delete dialog.
     */
    suspend fun deleteOriginals(): List<Uri> = withContext(Dispatchers.IO) {
        val stillPresent = eligibleOriginals.mapNotNull { it.sha256?.lowercase() }
            .filter { identity.libraryHasHash(it) }
            .toSet()
        val eligible = ImportOriginalDeletion.keysToDelete(
            choice = OriginalDeleteChoice.DELETE,
            eligible = eligibleOriginals,
            libraryStillHasHash = { hash -> hash.lowercase() in stillPresent },
        )
        eligibleOriginals = emptyList()
        documentDeleted = 0
        documentFailed = 0
        val media = mutableListOf<Uri>()
        for (item in eligible) {
            val document = runCatching { Uri.parse(item.stableKey) }.getOrNull()
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                SafAudioTree.resolveMediaStoreAudio(
                    context.contentResolver,
                    item.displayName,
                    item.sizeBytes,
                    pickedTreeDocumentId,
                    item.sha256,
                )
            } else {
                null
            }
            if (resolved != null) {
                media += resolved
            } else if (document != null) {
                val removed = runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, document)
                }.getOrDefault(false)
                if (removed) documentDeleted++ else documentFailed++
            } else {
                documentFailed++
            }
        }
        if (media.isEmpty()) {
            _state.value = LibraryImportUiState.Finished(
                ImportPromptCopy.deleteReport(documentDeleted, documentFailed),
            )
        } else {
            _state.value = LibraryImportUiState.NeedsSystemDelete(media)
        }
        media
    }

    fun onSystemDeleteFinished(confirmed: Boolean, requested: Int) {
        if (confirmed) documentDeleted += requested else documentFailed += requested
        _state.value = LibraryImportUiState.Finished(
            ImportPromptCopy.deleteReport(documentDeleted, documentFailed),
        )
    }

    suspend fun restore(song: Song) = withContext(Dispatchers.IO) {
        val row = songDao.getSong(song.id)
        val path = song.filePath ?: row?.sourcePath
        val hash = row?.contentHash
        val objects = backupRepository.listRemoteObjects("song").getOrElse { error ->
            _state.value = LibraryImportUiState.Finished(error.message ?: "Could not reach the cloud backup")
            return@withContext
        }
        val match = objects.firstOrNull { obj ->
            (!hash.isNullOrBlank() && obj.contentHash.equals(hash, ignoreCase = true)) ||
                cloudObjectMatchesSong(path, song.fileSizeBytes ?: obj.sizeBytes, obj.logicalPath, obj.sizeBytes)
        }
        if (match == null) {
            _state.value = LibraryImportUiState.Finished("No cloud copy is available for this song.")
            return@withContext
        }
        val root = grooveDownloads.directory()
        val name = match.logicalPath?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "${song.title}.mp3"
        val dest = com.aethelsoft.grooveplayer.domain.library.PrivateLibrarySongs.destination(root, name)
        val downloaded = backupRepository.downloadObject(match.contentHash, match.r2Key, dest)
        if (downloaded.isFailure || !dest.isFile || (match.sizeBytes > 0L && dest.length() != match.sizeBytes)) {
            if (dest.exists()) dest.delete()
            _state.value = LibraryImportUiState.Finished("Restore failed. The library copy was not kept.")
            return@withContext
        }
        val tags = readTags(dest)
        saveArtwork(root, match.contentHash, tags.artwork)
        identity.adoptVerifiedCopy(
            hash = match.contentHash,
            libraryPath = dest.absolutePath,
            title = tags.title.ifBlank { song.title },
            durationMs = tags.durationMs.takeIf { it > 0L } ?: song.durationMs,
        )
        musicRepository.bumpCatalogGeneration()
        _state.value = LibraryImportUiState.Finished("Restored ${song.title} into your library.")
    }

    private suspend fun knownPrivateHashes(root: File): Set<String> {
        val known = SongHashRemap.privateHashes(identity.rows()).toMutableSet()
        val files = root.listFiles()?.filter {
            it.isFile && com.aethelsoft.grooveplayer.domain.library.PrivateLibrarySongs.isListedAudio(it.name)
        }.orEmpty()
        for (file in files) {
            val row = songDao.findBySourcePath(file.absolutePath)
            val hash = row?.contentHash?.takeIf { it.isNotBlank() }
                ?: ContentHash.sha256(file).also { computed ->
                    if (row != null) songDao.updateContentHash(row.songId, computed)
                }
            known += hash.lowercase()
        }
        return known
    }

    private suspend fun indexHashForSize(hash: String, size: Long) {
        if (size <= 0L) return
        for (song in songDao.songsMissingHash()) {
            val path = song.sourcePath ?: continue
            val file = File(path)
            if (!file.isFile || file.length() != size || !file.canRead()) continue
            val computed = runCatching { ContentHash.sha256(file) }.getOrNull() ?: continue
            songDao.updateContentHash(song.songId, computed)
            if (computed.equals(hash, ignoreCase = true)) return
        }
    }

    private fun freeBytes(root: File): Long {
        return runCatching { StatFs(root.absolutePath).availableBytes }.getOrDefault(Long.MAX_VALUE)
    }

    private fun readTags(file: File): ImportedTags {
        return try {
            val audio = AudioFileIO.read(file)
            val tag = audio.tag
            val title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() }
                ?: file.name.substringBeforeLast('.', file.name)
            val seconds = audio.audioHeader?.trackLength?.toLong() ?: 0L
            ImportedTags(
                title = title,
                durationMs = seconds * 1000L,
                artwork = tag?.firstArtwork?.binaryData,
            )
        } catch (_: Exception) {
            ImportedTags(file.name.substringBeforeLast('.', file.name), 0L, null)
        }
    }

    private fun saveArtwork(root: File, hash: String, bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) return
        val dir = File(root, ".artwork")
        dir.mkdirs()
        val dest = File(dir, "${hash.lowercase()}.jpg")
        if (!dest.exists()) dest.writeBytes(bytes)
    }

    private fun failureReasons(files: List<ImportFileResult>): String? {
        return files.filter { it.status == ImportFileStatus.FAILED }
            .mapNotNull { it.failureReason }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("; ")
    }

    private fun summaryLine(copied: Int, already: Int, failed: Int, note: String?): String {
        return buildString {
            append("Copied $copied")
            append(" · already in library $already")
            append(" · failed $failed")
            if (!note.isNullOrBlank()) {
                append(". ")
                append(note)
            }
        }
    }

    private data class ImportedTags(val title: String, val durationMs: Long, val artwork: ByteArray?)
}
