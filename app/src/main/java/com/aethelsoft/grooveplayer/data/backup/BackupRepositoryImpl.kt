package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import android.util.Log
import com.aethelsoft.grooveplayer.BuildConfig
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.data.mapper.AuthMapper
import com.aethelsoft.grooveplayer.data.remote.api.BackupApi
import com.aethelsoft.grooveplayer.data.remote.dto.BackupCompleteRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupMatchRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupDownloadUrlRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupTrimRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupUploadUrlRequestDto
import com.aethelsoft.grooveplayer.di.NetworkModule
import com.aethelsoft.grooveplayer.domain.model.BackupKinds
import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot
import com.aethelsoft.grooveplayer.domain.model.BackupObject
import com.aethelsoft.grooveplayer.domain.model.CloudBackupPhase
import com.aethelsoft.grooveplayer.domain.model.CloudBackupState
import com.aethelsoft.grooveplayer.domain.model.StorageEntitlement
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupRequest
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupResult
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import com.aethelsoft.grooveplayer.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manual cloud backup via Benny R2 API (docs/backup-api.md + docs/backup-library.md).
 *
 * Per song: POST /v1/backup/match (basename+size) preferred; GET objects belt;
 * else SHA-256 → upload-url (honor deduped) → PUT → complete.
 * Room DB snapshot always runs first. Refresh `/v1/me` storage from complete.user.
 *
 * R2 cost rules: see app/R2_COST_RULES.md — skip never remints upload-url;
 * one whole-object GetObject; reuse download signed URLs until near expiry;
 * no S3 ListObjects; HeadObject only via complete; room.db skip-if-unchanged.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GroovePlayerDatabase,
    private val musicRepository: MusicRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val backupApi: BackupApi,
    @Named(NetworkModule.R2_HTTP_CLIENT) private val r2HttpClient: OkHttpClient,
) : BackupRepository {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * In-session signed download URL cache (R2 cost rule 3).
     * Keyed by content_hash and/or r2_key; remint only near expiry / miss.
     */
    private val downloadUrlCache = ConcurrentHashMap<String, CachedSignedDownload>()

    /** Snapshot for sync HTTP error mapping (403 read_only vs over_quota). */
    @Volatile
    private var gateEntitlement: StorageEntitlement? = null

    private val _state = MutableStateFlow(
        CloudBackupState(
            lastBackupAtEpochMs = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0L },
            lastError = prefs.getString(KEY_LAST_ERROR, null),
        )
    )

    override fun observeBackupState() = _state.asStateFlow()

    override suspend fun refreshLocalState() {
        val folders = resolveIncludedFolders()
        _state.update {
            it.copy(
                includedFolders = folders,
                lastBackupAtEpochMs = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { t -> t > 0L },
                lastError = prefs.getString(KEY_LAST_ERROR, null),
            )
        }
    }

    override suspend fun resolveIncludedFolders(): List<String> = withContext(Dispatchers.IO) {
        val all = musicRepository.getMusicFolderPaths()
        val excluded = userRepository.getUserSettings().excludedFolders.toSet()
        all.filter { it !in excluded }.sorted()
    }

    override suspend fun startBackup(
        entitlement: StorageEntitlement?,
        isPremium: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isPremium) {
                setPhase(
                    CloudBackupPhase.BLOCKED_NOT_PREMIUM,
                    message = "Cloud backup requires Premium.",
                )
                error("Cloud backup requires Premium.")
            }
            val ent = entitlement
            if (ent == null) {
                setPhase(
                    CloudBackupPhase.BLOCKED_NOT_PREMIUM,
                    message = "Missing storage entitlement from /v1/me.",
                )
                error("Missing storage entitlement.")
            }
            if (ent.isOptimisticStub) {
                setPhase(
                    CloudBackupPhase.ERROR,
                    message = "Cloud quota is a local stub — refresh /v1/me before backing up. " +
                        "Real uploads need server storage (not OPTIMISTIC).",
                )
                error("Optimistic storage stub — refusing upload.")
            }
            gateEntitlement = ent
            if (ent.overQuota) {
                setPhase(
                    CloudBackupPhase.BLOCKED_QUOTA,
                    message = "Cloud backup is over quota. Free cloud space (trim) before uploading.",
                )
                error("Over quota — uploads hard-stopped.")
            }
            if (ent.readOnly && !ent.overQuota) {
                setPhase(
                    CloudBackupPhase.BLOCKED_GRACE,
                    message = "Premium is in grace — backups are read-only until you resubscribe.",
                )
                error("Backups blocked during grace.")
            }
            if (ent.hardStop || (ent.quotaBytes > 0 && ent.usedBytes >= ent.quotaBytes)) {
                setPhase(
                    CloudBackupPhase.BLOCKED_QUOTA,
                    message = "Backup storage is full. Free cloud space or buy a +20 GB pack.",
                )
                error("Quota full.")
            }

            val folders = resolveIncludedFolders()
            _state.update {
                it.copy(
                    phase = CloudBackupPhase.PREPARING,
                    progressPercent = 0,
                    bytesPrepared = 0L,
                    bytesUploaded = 0L,
                    filesTotal = 0,
                    filesCompleted = 0,
                    filesDeduped = 0,
                    filesSkipped = 0,
                    includedFolders = folders,
                    lastError = null,
                    lastRunDryRun = false,
                    canRetry = false,
                    message = "Preparing included folders…",
                )
            }

            var prepared = 0L
            val files = mutableListOf<File>()
            folders.forEachIndexed { index, path ->
                val dir = File(path)
                if (dir.isDirectory) {
                    dir.walkTopDown()
                        .filter { it.isFile && isLikelyAudio(it) }
                        .forEach { f ->
                            files += f
                            prepared += f.length()
                        }
                }
                val pct = if (folders.isEmpty()) 10 else ((index + 1) * 20) / folders.size
                _state.update {
                    it.copy(progressPercent = pct, bytesPrepared = prepared, filesTotal = files.size)
                }
            }

            // Soft warn is UI-only; allow backup under 100%.
            var uploadedBytes = 0L
            var completed = 0
            var dedupedCount = 0
            var anyDryRun = false
            var remainingQuotaHeadroom =
                if (ent.quotaBytes > 0) (ent.quotaBytes - ent.usedBytes).coerceAtLeast(0L)
                else Long.MAX_VALUE

            _state.update {
                it.copy(
                    phase = CloudBackupPhase.UPLOADING,
                    progressPercent = 20,
                    filesTotal = files.size,
                    message = "Uploading library snapshot…",
                )
            }

            // Room DB snapshot first (kind=room_db) — docs/backup-library.md
            val roomResult = uploadRoomDbSnapshot(remainingQuotaHeadroom)
            anyDryRun = anyDryRun || roomResult.anyDryRun
            dedupedCount += if (roomResult.deduped) 1 else 0
            if (!roomResult.deduped && roomResult.uploadedBytes > 0L) {
                uploadedBytes += roomResult.uploadedBytes
            }
            roomResult.remainingQuotaHeadroom?.let { remainingQuotaHeadroom = it }
            prepared += roomResult.uploadedBytes

            if (files.isEmpty()) {
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putLong(KEY_LAST_BACKUP, now)
                    .remove(KEY_LAST_ERROR)
                    .apply()
                runCatching { authRepository.refreshSession() }
                _state.update {
                    it.copy(
                        phase = CloudBackupPhase.SUCCESS,
                        progressPercent = 100,
                        bytesPrepared = prepared,
                        bytesUploaded = uploadedBytes,
                        filesCompleted = 0,
                        filesDeduped = dedupedCount,
                        filesSkipped = 0,
                        lastBackupAtEpochMs = now,
                        lastError = null,
                        lastRunDryRun = anyDryRun,
                        canRetry = false,
                        message = buildString {
                            append("Library snapshot backed up")
                            if (roomResult.deduped) append(" (unchanged)")
                            append(" — no audio in included folders")
                            if (anyDryRun) append(" · dry-run")
                        },
                    )
                }
                return@runCatching Unit
            }

            // Jorge double-guard (R2 cost rule 1): prefer POST /match (basename+size);
            // GET /v1/backup/objects (Postgres catalog) as belt. Skip hits MUST NOT remint
            // upload-url or PUT. upload-url deduped remains hash/path short-circuit only.
            var skippedCount = 0
            val catalogPairs: MutableSet<Pair<String, Long>> = runCatching {
                backupApi.listObjects(kind = BackupKinds.SONG).objects
                    .asSequence()
                    .filter { obj ->
                        val k = obj.kind?.takeIf { it.isNotBlank() } ?: BackupKinds.SONG
                        k == BackupKinds.SONG
                    }
                    .mapNotNull { obj ->
                        val path = obj.logicalPath?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val base = path.substringAfterLast('/').substringAfterLast('\\')
                        if (base.isEmpty()) null else base to obj.sizeBytes
                    }
                    .toMutableSet()
            }.getOrElse { e ->
                Log.w(TAG, "catalog list for skip failed — continuing without client skip: ${e.message}")
                mutableSetOf()
            }
            Log.i(TAG, "client skip catalog pairs=${catalogPairs.size}")

            _state.update {
                it.copy(
                    message = "Uploading 0/${files.size}…",
                    filesSkipped = 0,
                )
            }

            for (file in files) {
                val size = file.length()
                if (size <= 0L) {
                    completed++
                    continue
                }

                val basename = file.name // case-sensitive as stored on device
                // Full logical_path still sent for display/catalog; match key is basename+size.
                val logicalPath = file.absolutePath

                fun markSkipped(reason: String) {
                    skippedCount++
                    completed++
                    catalogPairs.add(basename to size)
                    Log.i(TAG, "skip $reason basename=$basename size=$size")
                    val uploadPct = 20 + ((completed * 80) / files.size.coerceAtLeast(1))
                    _state.update {
                        it.copy(
                            progressPercent = uploadPct.coerceIn(0, 99),
                            bytesPrepared = prepared,
                            bytesUploaded = uploadedBytes,
                            filesCompleted = completed,
                            filesDeduped = dedupedCount,
                            filesSkipped = skippedCount,
                            lastRunDryRun = anyDryRun,
                            message = buildString {
                                append("Uploading $completed/${files.size}")
                                if (skippedCount > 0) append(" · $skippedCount skipped")
                                if (dedupedCount > 0) append(" · $dedupedCount deduped")
                                if (anyDryRun) append(" · dry-run (R2 keys pending)")
                            },
                        )
                    }
                }

                // Belt: local catalog from GET /v1/backup/objects
                if ((basename to size) in catalogPairs) {
                    markSkipped("objects-belt")
                    continue
                }

                // Preferred: POST /v1/backup/match (server basename+size; no hash yet)
                val matchHit = runCatching {
                    backupApi.matchBackup(
                        BackupMatchRequestDto(
                            logicalPath = logicalPath,
                            sizeBytes = size,
                            contentHash = null,
                        )
                    ).matched
                }.getOrElse { e ->
                    Log.w(TAG, "match pre-check failed — falling through: ${e.message}")
                    false
                }
                if (matchHit) {
                    markSkipped("match-name_size")
                    continue
                }

                if (size > remainingQuotaHeadroom) {
                    setPhase(
                        CloudBackupPhase.BLOCKED_QUOTA,
                        message = "Backup storage quota would be exceeded " +
                            "(${formatSize(size)} file; ${formatSize(remainingQuotaHeadroom)} left).",
                    )
                    error("Quota would be exceeded.")
                }

                val hash = sha256Hex(file)
                val contentType = guessContentType(file)

                val uploadResp = try {
                    backupApi.requestUploadUrl(
                        BackupUploadUrlRequestDto(
                            contentHash = hash,
                            sizeBytes = size,
                            contentType = contentType,
                            logicalPath = logicalPath,
                            kind = BackupKinds.SONG,
                        )
                    )
                } catch (e: HttpException) {
                    throw mapBackupHttp(e)
                }

                if (uploadResp.dryRun) anyDryRun = true

                val r2Key = uploadResp.r2Key
                    ?: error("upload-url missing r2_key")

                if (uploadResp.deduped) {
                    dedupedCount++
                    // Hash short-circuit when basename/path differed — no PUT.
                    Log.i(TAG, "deduped hash=$hash key=$r2Key")
                    catalogPairs.add(basename to size)
                } else {
                    if (!uploadResp.dryRun) {
                        val url = uploadResp.uploadUrl
                            ?: error("upload-url missing upload_url")
                        putToR2(url, file, contentType)
                    } else {
                        Log.i(TAG, "dry_run skip PUT key=$r2Key url=${uploadResp.uploadUrl}")
                    }

                    val completeResp = try {
                        backupApi.completeUpload(
                            BackupCompleteRequestDto(
                                contentHash = hash,
                                sizeBytes = size,
                                r2Key = r2Key,
                                logicalPath = logicalPath,
                                kind = BackupKinds.SONG,
                            )
                        )
                    } catch (e: HttpException) {
                        // Jorge/Benny: complete HeadObject — 422 = R2 missing or size mismatch.
                        // Local library is untouched; surface Retry (re-run PUT + complete).
                        if (e.code() == 422 && !uploadResp.dryRun) {
                            throw CloudUploadIncompleteException(
                                incompleteUploadMessage(file.name),
                            )
                        }
                        throw mapBackupHttp(e)
                    }

                    // Benny: complete always returns user (incl. dedupe); keep null-safe for older servers.
                    val userDto = completeResp.user
                    if (userDto != null) {
                        val domainUser = AuthMapper.toDomainUser(userDto)
                        authRepository.applyRemoteUser(domainUser)
                        domainUser.storage?.let { gateEntitlement = it }
                        val used = userDto.storage?.usedBytes
                        val quota = userDto.storage?.quotaBytes
                        if (used != null && quota != null && quota > 0) {
                            remainingQuotaHeadroom = (quota - used).coerceAtLeast(0L)
                        }
                    } else {
                        Log.w(TAG, "complete missing user — refreshing /v1/me")
                        runCatching { authRepository.refreshSession() }
                    }
                    if (!completeResp.deduped) {
                        remainingQuotaHeadroom =
                            (remainingQuotaHeadroom - size).coerceAtLeast(0L)
                        uploadedBytes += size
                    } else {
                        dedupedCount++
                    }
                    catalogPairs.add(basename to size)
                }

                completed++
                val uploadPct = 20 + ((completed * 80) / files.size.coerceAtLeast(1))
                _state.update {
                    it.copy(
                        progressPercent = uploadPct.coerceIn(0, 99),
                        bytesPrepared = prepared,
                        bytesUploaded = uploadedBytes,
                        filesCompleted = completed,
                        filesDeduped = dedupedCount,
                        filesSkipped = skippedCount,
                        lastRunDryRun = anyDryRun,
                        message = buildString {
                            append("Uploading $completed/${files.size}")
                            if (skippedCount > 0) append(" · $skippedCount skipped")
                            if (dedupedCount > 0) append(" · $dedupedCount deduped")
                            if (anyDryRun) append(" · dry-run (R2 keys pending)")
                        },
                    )
                }
            }

            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_LAST_BACKUP, now)
                .remove(KEY_LAST_ERROR)
                .apply()

            // Refresh /v1/me storage after batch (complete may already have applied).
            runCatching { authRepository.refreshSession() }

            _state.update {
                it.copy(
                    phase = CloudBackupPhase.SUCCESS,
                    progressPercent = 100,
                    bytesPrepared = prepared,
                    bytesUploaded = uploadedBytes,
                    filesCompleted = completed,
                    filesDeduped = dedupedCount,
                    filesSkipped = skippedCount,
                    lastBackupAtEpochMs = now,
                    lastError = null,
                    lastRunDryRun = anyDryRun,
                    canRetry = false,
                    message = buildString {
                        append("Backed up $completed file(s)")
                        if (skippedCount > 0) append(" · $skippedCount skipped (already on cloud)")
                        if (dedupedCount > 0) append(" · $dedupedCount hash-deduped")
                        if (anyDryRun) append(" · dry-run URLs (catalog only until R2 live)")
                    },
                )
            }
            Log.i(
                TAG,
                "backup complete files=$completed skipped=$skippedCount deduped=$dedupedCount " +
                    "uploadedBytes=$uploadedBytes dryRun=$anyDryRun",
            )
            Unit
        }.onFailure { e ->
            if (_state.value.phase !in setOf(
                    CloudBackupPhase.BLOCKED_NOT_PREMIUM,
                    CloudBackupPhase.BLOCKED_GRACE,
                    CloudBackupPhase.BLOCKED_QUOTA,
                )
            ) {
                val incomplete = e is CloudUploadIncompleteException
                val msg = e.message ?: "Backup failed"
                prefs.edit().putString(KEY_LAST_ERROR, msg).apply()
                _state.update {
                    it.copy(
                        phase = CloudBackupPhase.ERROR,
                        lastError = msg,
                        message = msg,
                        canRetry = incomplete || it.canRetry,
                    )
                }
            }
        }
    }

    override suspend fun downloadObject(
        contentHash: String?,
        r2Key: String?,
        destFile: File,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(!contentHash.isNullOrBlank() || !r2Key.isNullOrBlank()) {
                "content_hash or r2_key required"
            }
            val resolved = resolveDownloadUrl(contentHash = contentHash, r2Key = r2Key)
            if (resolved.dryRun) {
                Log.i(TAG, "dry_run skip GET download key=${resolved.r2Key}")
                // Create empty placeholder so callers can see dry-run path worked.
                destFile.parentFile?.mkdirs()
                destFile.writeBytes(ByteArray(0))
                return@runCatching Unit
            }
            val url = resolved.url ?: error("download-url missing download_url")
            // One whole-object GetObject — no Range (R2 cost rule 2).
            getFromR2(url, destFile)
        }
    }

    override suspend fun listRemoteObjects(kind: String?): Result<List<BackupObject>> =
        withContext(Dispatchers.IO) {
            runCatching {
                backupApi.listObjects(kind = kind).objects.map {
                    BackupObject(
                        id = it.id,
                        contentHash = it.contentHash,
                        sizeBytes = it.sizeBytes,
                        r2Key = it.r2Key,
                        logicalPath = it.logicalPath,
                        kind = it.kind?.takeIf { k -> k.isNotBlank() } ?: BackupKinds.SONG,
                        schemaVersion = it.schemaVersion,
                        appVersion = it.appVersion,
                        createdAtIso = it.createdAt,
                    )
                }
            }.recoverCatching { e ->
                if (e is HttpException) throw mapBackupHttp(e) else throw e
            }
        }



    override suspend fun deleteRemoteObject(objectId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = try {
                    backupApi.deleteObject(objectId)
                } catch (e: HttpException) {
                    throw mapBackupHttp(e)
                }
                if (!response.isSuccessful) {
                    throw mapBackupHttp(HttpException(response))
                }
                val body = response.body()
                body?.user?.let { dto ->
                    authRepository.applyRemoteUser(AuthMapper.toDomainUser(dto))
                } ?: run {
                    Log.w(TAG, "delete missing/empty body — refreshing /v1/me")
                    runCatching { authRepository.refreshSession() }
                }
                Unit
            }
        }

    override suspend fun trimCloudBackup(
        request: TrimCloudBackupRequest,
    ): Result<TrimCloudBackupResult> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = try {
                backupApi.trimBackup(
                    BackupTrimRequestDto(
                        strategy = request.strategy.apiValue,
                        artist = request.artist?.takeIf { it.isNotBlank() },
                        album = request.album?.takeIf { it.isNotBlank() },
                        year = request.year,
                        limitBytes = request.limitBytes?.takeIf { it > 0L },
                    )
                )
            } catch (e: HttpException) {
                throw mapBackupHttp(e)
            }
            resp.user?.let { dto ->
                authRepository.applyRemoteUser(AuthMapper.toDomainUser(dto))
            } ?: run {
                Log.w(TAG, "trim missing user — refreshing /v1/me")
                runCatching { authRepository.refreshSession() }
            }
            TrimCloudBackupResult(
                strategy = resp.strategy ?: request.strategy.apiValue,
                deleted = resp.deleted,
                bytesFreed = resp.bytesFreed,
            )
        }
    }

    private fun putToR2(uploadUrl: String, file: File, contentType: String) {
        val media = contentType.toMediaTypeOrNull()
        val body = file.asRequestBody(media)
        val request = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .header("Content-Type", contentType)
            .build()
        r2HttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("R2 PUT failed HTTP ${response.code}")
            }
        }
    }

    /**
     * Whole-object R2 GET (cost rule 2). Never set Range — one GetObject per song.
     * Body streamed to disk (still a single Class B request).
     */
    private fun getFromR2(downloadUrl: String, destFile: File) {
        val request = Request.Builder()
            .url(downloadUrl)
            .get()
            // Explicit: no Range header (r2HttpClient also strips any Range).
            .build()
        check(request.header("Range") == null) {
            "R2 cost rule violated: Range header on GetObject"
        }
        r2HttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("R2 GET failed HTTP ${response.code}")
            }
            val body = response.body ?: error("R2 GET empty body")
            destFile.parentFile?.mkdirs()
            FileOutputStream(destFile).use { out ->
                body.byteStream().use { input -> input.copyTo(out) }
            }
        }
    }

    /**
     * Resolve a signed download URL with in-session reuse until near expiry
     * (R2 cost rule 3). Remint only on miss or when TTL < [DOWNLOAD_URL_REMIN_SKEW_MS].
     */
    private suspend fun resolveDownloadUrl(
        contentHash: String?,
        r2Key: String?,
    ): ResolvedDownloadUrl {
        val primaryKey = downloadCacheKey(contentHash, r2Key)
        val now = System.currentTimeMillis()
        downloadUrlCache[primaryKey]?.let { cached ->
            if (cached.isReusable(now)) {
                Log.i(
                    TAG,
                    "reuse download-url cacheKey=$primaryKey " +
                        "ttlMs=${cached.expiresAtEpochMs - now}",
                )
                return ResolvedDownloadUrl(
                    url = cached.url,
                    r2Key = cached.r2Key,
                    dryRun = false,
                    fromCache = true,
                )
            }
        }
        val resp = try {
            backupApi.requestDownloadUrl(
                BackupDownloadUrlRequestDto(
                    contentHash = contentHash,
                    r2Key = r2Key,
                )
            )
        } catch (e: HttpException) {
            throw mapBackupHttp(e)
        }
        if (resp.dryRun) {
            return ResolvedDownloadUrl(
                url = resp.downloadUrl,
                r2Key = resp.r2Key ?: r2Key,
                dryRun = true,
                fromCache = false,
            )
        }
        val url = resp.downloadUrl ?: error("download-url missing download_url")
        val expiresInSec = (resp.expiresInSec ?: DEFAULT_DOWNLOAD_EXPIRES_SEC).coerceAtLeast(1)
        val expiresAt = now + expiresInSec * 1000L
        val resolvedKey = resp.r2Key ?: r2Key
        val entry = CachedSignedDownload(
            url = url,
            r2Key = resolvedKey,
            contentHash = contentHash,
            expiresAtEpochMs = expiresAt,
        )
        rememberDownloadUrl(entry)
        Log.i(
            TAG,
            "mint download-url cacheKey=$primaryKey expiresInSec=$expiresInSec",
        )
        return ResolvedDownloadUrl(
            url = url,
            r2Key = resolvedKey,
            dryRun = false,
            fromCache = false,
        )
    }

    private fun rememberDownloadUrl(entry: CachedSignedDownload) {
        entry.contentHash?.takeIf { it.isNotBlank() }?.let { h ->
            downloadUrlCache["h:$h"] = entry
        }
        entry.r2Key?.takeIf { it.isNotBlank() }?.let { k ->
            downloadUrlCache["k:$k"] = entry
        }
    }

    private fun downloadCacheKey(contentHash: String?, r2Key: String?): String = when {
        !contentHash.isNullOrBlank() -> "h:$contentHash"
        !r2Key.isNullOrBlank() -> "k:$r2Key"
        else -> error("content_hash or r2_key required")
    }

    private fun mapBackupHttp(e: HttpException): Exception {
        val body = try {
            e.response()?.errorBody()?.string().orEmpty()
        } catch (_: Exception) {
            ""
        }
        val apiError = extractApiError(body)
        val haystack = listOf(apiError, body).joinToString(" ").lowercase()
        val code = e.code()
        val msg = when (code) {
            403 -> {
                // Backend: premium required | backups read-only during grace | upload not allowed.
                // read_only is set for Premium cancel grace OR over_quota — never crash; align UX.
                when {
                    "premium required" in haystack ||
                        (haystack.contains("premium") && !haystack.contains("read-only") &&
                            !haystack.contains("grace") && !haystack.contains("upload not allowed")) -> {
                        val m = "Cloud backup requires Premium (HTTP 403)."
                        setPhase(CloudBackupPhase.BLOCKED_NOT_PREMIUM, m)
                        m
                    }
                    "read-only" in haystack || "grace" in haystack || "upload not allowed" in haystack -> {
                        // Backend sets read_only for grace OR over_quota; classify from gate snapshot.
                        val storage = gateEntitlement
                        val over = storage?.overQuota == true
                        val hard = storage?.hardStop == true && storage.overQuota != true
                        val m = when {
                            over -> {
                                "Cloud backup is over quota — uploads blocked (HTTP 403). " +
                                    "Use Free cloud space below. Local library is untouched."
                            }
                            hard -> {
                                "Backup storage is full (hard_stop) — uploads blocked (HTTP 403). " +
                                    "Free cloud space or buy a +20 GB pack. Local library is untouched."
                            }
                            else -> {
                                "Cloud backups are read-only (HTTP 403) — Premium grace or over quota. " +
                                    "Resubscribe, or if over quota use Free cloud space. " +
                                    "Local library is untouched."
                            }
                        }
                        setPhase(
                            if (over || hard) CloudBackupPhase.BLOCKED_QUOTA
                            else CloudBackupPhase.BLOCKED_GRACE,
                            m,
                        )
                        m
                    }
                    else -> {
                        val m = "Backup not allowed (HTTP 403). " +
                            (apiError.ifBlank { body.ifBlank { "Forbidden" } })
                        setPhase(CloudBackupPhase.BLOCKED_GRACE, m)
                        m
                    }
                }
            }
            507 -> {
                val m = "Backup storage full / hard_stop (HTTP 507). " +
                    "Free cloud space or buy a +20 GB pack. Local library is untouched."
                setPhase(CloudBackupPhase.BLOCKED_QUOTA, m)
                m
            }
            422 -> {
                // Prefer CloudUploadIncompleteException from complete; this is a fallback.
                incompleteUploadMessage(fileName = null)
            }
            else -> apiError.ifBlank { body.ifBlank { "Backup API error (HTTP $code)" } }
        }
        return IllegalStateException(msg)
    }

    /** Backend writeErr → `{ "error": "..." }`. */
    private fun extractApiError(body: String): String {
        if (body.isBlank()) return ""
        val marker = "\"error\""
        val i = body.indexOf(marker)
        if (i < 0) return ""
        val after = body.substring(i + marker.length)
        val colon = after.indexOf(':')
        if (colon < 0) return ""
        val q1 = after.indexOf('"', startIndex = colon)
        if (q1 < 0) return ""
        val q2 = after.indexOf('"', startIndex = q1 + 1)
        if (q2 < 0) return ""
        return after.substring(q1 + 1, q2).trim()
    }

    private fun setPhase(phase: CloudBackupPhase, message: String) {
        _state.update {
            it.copy(phase = phase, message = message, lastError = message, progressPercent = 0)
        }
    }

    override suspend fun fetchCloudLibraryMetadata(): Result<CloudLibrarySnapshot?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = try {
                    backupApi.getLibrary()
                } catch (e: HttpException) {
                    throw mapBackupHttp(e)
                }
                val lib = resp.library ?: return@runCatching null
                CloudLibrarySnapshot(
                    objectId = lib.objectId,
                    contentHash = lib.contentHash,
                    sizeBytes = lib.sizeBytes,
                    r2Key = lib.r2Key,
                    logicalPath = lib.logicalPath,
                    schemaVersion = lib.schemaVersion,
                    appVersion = lib.appVersion,
                    createdAtIso = lib.createdAt,
                    dryRun = resp.dryRun,
                )
            }
        }

    override suspend fun restoreLibraryFromCloud(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = try {
                backupApi.getLibrary()
            } catch (e: HttpException) {
                throw mapBackupHttp(e)
            }
            val lib = resp.library
                ?: error("No cloud library snapshot yet — back up first.")
            val remoteSchema = lib.schemaVersion
                ?: error("Cloud library missing schema_version")
            val localSchema = try {
                database.openHelper.readableDatabase.version
            } catch (_: Exception) {
                GroovePlayerDatabase.SCHEMA_VERSION
            }
            // Compatible if remote <= local (Room can migrate forward); block if newer.
            if (remoteSchema > localSchema) {
                error(
                    "Cloud library schema $remoteSchema is newer than this app ($localSchema). " +
                        "Update GroovePlayer before restoring.",
                )
            }
            if (resp.dryRun) {
                Log.i(TAG, "dry_run skip library restore key=${lib.r2Key}")
                error("Cloud restore is dry-run only until R2 is live.")
            }
            val url = lib.downloadUrl ?: error("library missing download_url")
            // Prefer URL from library response; cache for in-session reuse (rule 3).
            val expiresInSec = (lib.expiresInSec ?: DEFAULT_DOWNLOAD_EXPIRES_SEC).coerceAtLeast(1)
            rememberDownloadUrl(
                CachedSignedDownload(
                    url = url,
                    r2Key = lib.r2Key,
                    contentHash = lib.contentHash,
                    expiresAtEpochMs = System.currentTimeMillis() + expiresInSec * 1000L,
                ),
            )
            val gz = File(context.cacheDir, "restore-room.db.gz")
            val raw = File(context.cacheDir, "restore-room.db")
            getFromR2(url, gz)
            gunzipFile(gz, raw)
            // Close Room before replacing files (WAL/SHM must go).
            database.close()
            val dbFile = context.getDatabasePath(GroovePlayerDatabase.DATABASE_NAME)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            dbFile.parentFile?.mkdirs()
            raw.copyTo(dbFile, overwrite = true)
            gz.delete()
            raw.delete()
            prefs.edit().putBoolean(KEY_NEEDS_RESTART_AFTER_RESTORE, true).apply()
            Log.i(
                TAG,
                "library restore wrote ${dbFile.absolutePath} schema=$remoteSchema — restart required",
            )
            Unit
        }
    }

    /**
     * Checkpoint WAL, gzip main Room DB, sha256 gzip bytes, upload kind=room_db.
     * Fixed R2 key on server: backups/{userId}/library/room.db.gz
     */
    private suspend fun uploadRoomDbSnapshot(
        remainingQuotaHeadroom: Long,
    ): RoomUploadResult {
        val schemaVersion = try {
            database.openHelper.readableDatabase.version
        } catch (_: Exception) {
            GroovePlayerDatabase.SCHEMA_VERSION
        }
        // Flush WAL into main DB file before copy.
        try {
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { c ->
                c.moveToFirst()
            }
        } catch (e: Exception) {
            Log.w(TAG, "wal_checkpoint failed: ${e.message}")
        }
        val dbFile = context.getDatabasePath(GroovePlayerDatabase.DATABASE_NAME)
        if (!dbFile.exists() || dbFile.length() <= 0L) {
            Log.w(TAG, "Room DB missing or empty — skip room_db upload")
            return RoomUploadResult(
                deduped = true,
                uploadedBytes = 0L,
                anyDryRun = false,
                remainingQuotaHeadroom = remainingQuotaHeadroom,
            )
        }
        val gz = File(context.cacheDir, "backup-room.db.gz")
        gzipFile(dbFile, gz)
        val size = gz.length()
        if (size <= 0L) {
            gz.delete()
            return RoomUploadResult(true, 0L, false, remainingQuotaHeadroom)
        }
        // Replace semantics: server applies quota = used - old + new; client only soft-checks size.
        if (size > remainingQuotaHeadroom && remainingQuotaHeadroom != Long.MAX_VALUE) {
            // Still attempt — server may accept if replacing a larger snapshot.
            Log.i(TAG, "room_db size=$size headroom=$remainingQuotaHeadroom — relying on server delta")
        }
        val hash = sha256Hex(gz)
        val appVersion = BuildConfig.VERSION_NAME
        // R2 cost rule 6: local skip-if-unchanged — never remint upload-url / PUT
        // when gzip hash+size match last successful or server-deduped room_db.
        val lastHash = prefs.getString(KEY_LAST_ROOM_HASH, null)
        val lastSize = prefs.getLong(KEY_LAST_ROOM_SIZE, -1L)
        if (lastHash != null && lastHash == hash && lastSize == size) {
            Log.i(TAG, "room_db skip local-hash-match hash=$hash size=$size")
            gz.delete()
            return RoomUploadResult(
                deduped = true,
                uploadedBytes = 0L,
                anyDryRun = false,
                remainingQuotaHeadroom = remainingQuotaHeadroom,
            )
        }
        val uploadResp = try {
            backupApi.requestUploadUrl(
                BackupUploadUrlRequestDto(
                    contentHash = hash,
                    sizeBytes = size,
                    contentType = CONTENT_TYPE_GZIP,
                    logicalPath = LOGICAL_PATH_ROOM_DB,
                    kind = BackupKinds.ROOM_DB,
                    schemaVersion = schemaVersion,
                    appVersion = appVersion,
                )
            )
        } catch (e: HttpException) {
            gz.delete()
            throw mapBackupHttp(e)
        }
        val anyDryRun = uploadResp.dryRun
        val r2Key = uploadResp.r2Key ?: run {
            gz.delete()
            error("upload-url missing r2_key for room_db")
        }
        if (uploadResp.deduped) {
            Log.i(TAG, "room_db unchanged hash=$hash key=$r2Key")
            rememberRoomDbFingerprint(hash, size)
            gz.delete()
            return RoomUploadResult(
                deduped = true,
                uploadedBytes = 0L,
                anyDryRun = anyDryRun,
                remainingQuotaHeadroom = remainingQuotaHeadroom,
            )
        }
        if (!uploadResp.dryRun) {
            val url = uploadResp.uploadUrl ?: run {
                gz.delete()
                error("upload-url missing upload_url for room_db")
            }
            putToR2(url, gz, CONTENT_TYPE_GZIP)
        } else {
            Log.i(TAG, "dry_run skip PUT room_db key=$r2Key")
        }
        val completeResp = try {
            backupApi.completeUpload(
                BackupCompleteRequestDto(
                    contentHash = hash,
                    sizeBytes = size,
                    r2Key = r2Key,
                    logicalPath = LOGICAL_PATH_ROOM_DB,
                    kind = BackupKinds.ROOM_DB,
                    schemaVersion = schemaVersion,
                    appVersion = appVersion,
                )
            )
        } catch (e: HttpException) {
            gz.delete()
            if (e.code() == 422 && !uploadResp.dryRun) {
                throw CloudUploadIncompleteException(
                    incompleteUploadMessage("library/room.db.gz"),
                )
            }
            throw mapBackupHttp(e)
        }
        var newHeadroom = remainingQuotaHeadroom
        val userDto = completeResp.user
        if (userDto != null) {
            val domainUser = AuthMapper.toDomainUser(userDto)
            authRepository.applyRemoteUser(domainUser)
            domainUser.storage?.let { gateEntitlement = it }
            val used = userDto.storage?.usedBytes
            val quota = userDto.storage?.quotaBytes
            if (used != null && quota != null && quota > 0) {
                newHeadroom = (quota - used).coerceAtLeast(0L)
            }
        } else {
            Log.w(TAG, "room_db complete missing user — refreshing /v1/me")
            runCatching { authRepository.refreshSession() }
        }
        gz.delete()
        rememberRoomDbFingerprint(hash, size)
        Log.i(TAG, "room_db uploaded size=$size schema=$schemaVersion replaced=${completeResp.replaced}")
        return RoomUploadResult(
            deduped = completeResp.deduped,
            uploadedBytes = if (completeResp.deduped) 0L else size,
            anyDryRun = anyDryRun,
            remainingQuotaHeadroom = newHeadroom,
        )
    }

    private fun rememberRoomDbFingerprint(hash: String, size: Long) {
        prefs.edit()
            .putString(KEY_LAST_ROOM_HASH, hash)
            .putLong(KEY_LAST_ROOM_SIZE, size)
            .apply()
    }

    private fun gzipFile(src: File, dest: File) {
        dest.parentFile?.mkdirs()
        FileInputStream(src).use { input ->
            GZIPOutputStream(FileOutputStream(dest)).use { out ->
                input.copyTo(out)
            }
        }
    }

    private fun gunzipFile(src: File, dest: File) {
        dest.parentFile?.mkdirs()
        GZIPInputStream(FileInputStream(src)).use { input ->
            FileOutputStream(dest).use { out ->
                input.copyTo(out)
            }
        }
    }

    private fun isLikelyAudio(file: File): Boolean {
        val n = file.name.lowercase()
        return n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".flac") ||
            n.endsWith(".ogg") || n.endsWith(".wav") || n.endsWith(".aac") ||
            n.endsWith(".opus") || n.endsWith(".wma")
    }

    private fun guessContentType(file: File): String {
        val n = file.name.lowercase()
        return when {
            n.endsWith(".mp3") -> "audio/mpeg"
            n.endsWith(".m4a") -> "audio/mp4"
            n.endsWith(".flac") -> "audio/flac"
            n.endsWith(".ogg") -> "audio/ogg"
            n.endsWith(".wav") -> "audio/wav"
            n.endsWith(".aac") -> "audio/aac"
            n.endsWith(".opus") -> "audio/opus"
            n.endsWith(".wma") -> "audio/x-ms-wma"
            else -> "application/octet-stream"
        }
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }

    private fun incompleteUploadMessage(fileName: String?): String {
        val which = fileName?.let { " for “$it”" }.orEmpty()
        return "Cloud upload incomplete$which — the object is missing on R2 or the size " +
            "does not match (HTTP 422). Your local music was not deleted or changed. " +
            "Tap Retry to upload again."
    }

    companion object {
        private const val TAG = "BackupRepository"
        private const val PREFS = "groove_cloud_backup"
        private const val KEY_LAST_BACKUP = "last_backup_at"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_NEEDS_RESTART_AFTER_RESTORE = "needs_restart_after_library_restore"
        private const val KEY_LAST_ROOM_HASH = "last_room_db_hash"
        private const val KEY_LAST_ROOM_SIZE = "last_room_db_size"
        private const val LOGICAL_PATH_ROOM_DB = "library/room.db.gz"
        private const val CONTENT_TYPE_GZIP = "application/gzip"
        /** Fallback when download-url omits expires_in_sec. */
        private const val DEFAULT_DOWNLOAD_EXPIRES_SEC = 900
        /** Remint when remaining TTL is below this skew (rule 3). */
        internal const val DOWNLOAD_URL_REMIN_SKEW_MS = 120_000L
    }
}

/** In-session signed download URL (R2 cost rule 3). */
private data class CachedSignedDownload(
    val url: String,
    val r2Key: String?,
    val contentHash: String?,
    val expiresAtEpochMs: Long,
) {
    fun isReusable(nowEpochMs: Long, skewMs: Long = BackupRepositoryImpl.DOWNLOAD_URL_REMIN_SKEW_MS): Boolean =
        url.isNotBlank() && nowEpochMs < expiresAtEpochMs - skewMs
}

private data class ResolvedDownloadUrl(
    val url: String?,
    val r2Key: String?,
    val dryRun: Boolean,
    val fromCache: Boolean,
)

private data class RoomUploadResult(
    val deduped: Boolean,
    val uploadedBytes: Long,
    val anyDryRun: Boolean,
    val remainingQuotaHeadroom: Long?,
)

/** complete 422: R2 HeadObject missing / size mismatch — local files safe; UI offers Retry. */
private class CloudUploadIncompleteException(message: String) : IllegalStateException(message)
