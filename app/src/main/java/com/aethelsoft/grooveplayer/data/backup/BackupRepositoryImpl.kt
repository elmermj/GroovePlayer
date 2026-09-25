package com.aethelsoft.grooveplayer.data.backup

import android.content.Context
import android.util.Log
import com.aethelsoft.grooveplayer.BuildConfig
import com.aethelsoft.grooveplayer.data.local.db.GroovePlayerDatabase
import com.aethelsoft.grooveplayer.data.local.db.RoomDbSwapFiles
import com.aethelsoft.grooveplayer.data.mapper.AuthMapper
import com.aethelsoft.grooveplayer.data.remote.api.BackupApi
import com.aethelsoft.grooveplayer.data.remote.dto.BackupCompleteRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupLeaseRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupLeaseResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupObjectDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupMatchRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupDownloadUrlRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupTrimRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupUploadUrlRequestDto
import com.aethelsoft.grooveplayer.di.NetworkModule
import com.aethelsoft.grooveplayer.domain.backup.AppPrivateLibrary
import com.aethelsoft.grooveplayer.domain.backup.BackupCatalogPaths
import com.aethelsoft.grooveplayer.domain.backup.BackupJobGate
import com.aethelsoft.grooveplayer.domain.backup.BackupLeaseCopy
import com.aethelsoft.grooveplayer.domain.backup.BackupLeasePolicy
import com.aethelsoft.grooveplayer.domain.backup.BackupProgressLabel
import com.aethelsoft.grooveplayer.domain.backup.BackupTransfer
import com.aethelsoft.grooveplayer.domain.backup.RemoteBackupLease
import com.aethelsoft.grooveplayer.domain.backup.CloudHashDedup
import com.aethelsoft.grooveplayer.domain.backup.ContentHash
import com.aethelsoft.grooveplayer.domain.library.BackupLibraryFiles
import com.aethelsoft.grooveplayer.domain.backup.DbSwapStep
import com.aethelsoft.grooveplayer.domain.backup.GrooveDownloadPlacement
import com.aethelsoft.grooveplayer.domain.backup.HashedAudio
import com.aethelsoft.grooveplayer.domain.backup.LegacyLibraryAdoption
import com.aethelsoft.grooveplayer.domain.backup.PlacedCloudSong
import com.aethelsoft.grooveplayer.domain.backup.R2GetException
import com.aethelsoft.grooveplayer.domain.backup.LoginRestorePrompt
import com.aethelsoft.grooveplayer.domain.backup.RestoreDownloadRetry
import com.aethelsoft.grooveplayer.domain.backup.RestorePhase
import com.aethelsoft.grooveplayer.domain.backup.RestoreProgress
import com.aethelsoft.grooveplayer.domain.backup.RestoreProgressLabel
import com.aethelsoft.grooveplayer.domain.backup.RestoreProgressSnapshot
import com.aethelsoft.grooveplayer.domain.backup.StagingVerdict
import com.aethelsoft.grooveplayer.domain.model.BackupJobStep
import com.aethelsoft.grooveplayer.domain.model.BackupKinds
import com.aethelsoft.grooveplayer.domain.model.CloudLibrarySnapshot
import com.aethelsoft.grooveplayer.domain.model.BackupObject
import com.aethelsoft.grooveplayer.domain.model.CloudBackupPhase
import com.aethelsoft.grooveplayer.domain.model.CloudBackupState
import com.aethelsoft.grooveplayer.domain.model.isUploadInProgress
import com.aethelsoft.grooveplayer.domain.model.StorageEntitlement
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupRequest
import com.aethelsoft.grooveplayer.domain.model.TrimCloudBackupResult
import com.aethelsoft.grooveplayer.domain.repository.AuthRepository
import com.aethelsoft.grooveplayer.domain.repository.BackupRepository
import com.aethelsoft.grooveplayer.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Manual cloud backup via Benny R2 API (docs/backup-api.md + docs/backup-library.md).
 *
 * Uploads songs that already live in the app-private library. Same SHA-256 + size already
 * in the cloud is skipped.
 * Song bytes are confirmed first; the Room snapshot is uploaded only after that.
 *
 * R2 cost rules: skip never remints upload-url;
 * one whole-object GetObject; reuse download signed URLs until near expiry;
 * no S3 ListObjects; HeadObject only via complete; room.db skip-if-unchanged.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GroovePlayerDatabase,
    private val restoreSession: LibraryRestoreSession,
    private val grooveDownloads: GrooveDownloadsLocator,
    private val jobGate: BackupJobGate,
    private val musicRepository: MusicRepository,
    private val authRepository: AuthRepository,
    private val loginRestorePromptMemory: LoginRestorePromptMemory,
    private val backupApi: BackupApi,
    private val installDeviceId: InstallDeviceId,
    private val moshi: Moshi,
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

    /** Set by the heartbeat when the lease is lost or another device takes it. */
    private val backupLeaseLost = AtomicReference<LeaseLoss?>(null)

    /** Bumped when a backup acquire starts so an in-flight status GET cannot overwrite it. */
    private val leaseRefreshGeneration = AtomicLong(0)

    private val leaseAdapter by lazy { moshi.adapter(BackupLeaseResponseDto::class.java) }

    private val _state = MutableStateFlow(restoredBackupState())
    private val restoreProgress = RestoreProgress()
    private val _restoreProgress = MutableStateFlow(restoreProgress.snapshot())

    override fun observeBackupState() = _state.asStateFlow()

    override fun observeRestoreProgress(): Flow<RestoreProgressSnapshot> =
        _restoreProgress.asStateFlow()

    override fun restorePhase(): RestorePhase = restoreSession.phase()

    private fun publishRestore() {
        _restoreProgress.value = restoreProgress.snapshot()
    }

    override suspend fun refreshLocalState() {
        _state.update {
            it.copy(
                lastBackupAtEpochMs = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { t -> t > 0L },
                lastError = prefs.getString(KEY_LAST_ERROR, null),
                canRetry = prefs.getBoolean(KEY_CAN_RETRY, false),
                phase = if (prefs.getBoolean(KEY_CAN_RETRY, false) &&
                    it.phase !in ACTIVE_BACKUP_PHASES
                ) {
                    CloudBackupPhase.ERROR
                } else {
                    it.phase
                },
                message = if (prefs.getBoolean(KEY_CAN_RETRY, false)) {
                    prefs.getString(KEY_LAST_ERROR, null)
                } else {
                    it.message
                },
            )
        }
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
            if (restoreSession.phase() != RestorePhase.IDLE ||
                !jobGate.tryAcquire(BackupTransfer.BACKUP)
            ) {
                error(BackupJobGate.BUSY_MESSAGE)
            }
            var heldLease: LeaseGate.Acquired? = null
            val deviceId = installDeviceId.get()
            try {
                when (val gate = acquireBackupLease(deviceId)) {
                    is LeaseGate.Acquired -> heldLease = gate
                    LeaseGate.HeldByOther -> throw OtherDeviceBackupException()
                    LeaseGate.EndpointMissing ->
                        Log.w(TAG, "backup lease endpoint missing — continuing without cross-device lock")
                }
                backupLeaseLost.set(null)
                coroutineScope {
                    val heartbeat = heldLease?.let { held ->
                        launch {
                            heartbeatBackupLease(
                                deviceId,
                                held.leaseId,
                                held.heartbeatIntervalMs,
                            )
                        }
                    }
                    try {
            _state.update {
                it.copy(
                    phase = CloudBackupPhase.PREPARING,
                    progressPercent = 0,
                    bytesPrepared = 0L,
                    bytesUploaded = 0L,
                    jobStep = BackupJobStep.PREPARING,
                    filesTotal = 0,
                    filesCompleted = 0,
                    consolidateCompleted = 0,
                    consolidateTotal = 0,
                    uploadCompleted = 0,
                    uploadTotal = 0,
                    filesDeduped = 0,
                    filesSkipped = 0,
                    lastError = null,
                    lastRunDryRun = false,
                    canRetry = false,
                    message = BackupProgressLabel.status(
                        BackupJobStep.PREPARING, 0, 0, 0, 0,
                    ),
                )
            }
            prefs.edit().putBoolean(KEY_CAN_RETRY, false).remove(KEY_LAST_ERROR).apply()

            throwIfBackupLeaseLost()
            val privateRoot = grooveDownloads.directory().absolutePath
            val uploadFiles = BackupLibraryFiles.select(musicRepository.getAllSongs()) { path ->
                AppPrivateLibrary.isInside(path, privateRoot) &&
                    File(path).isFile &&
                    File(path).length() > 0L
            }.map { File(it) }
            var prepared = uploadFiles.sumOf { it.length() }
            _state.update {
                it.copy(
                    progressPercent = BackupProgressLabel.PREPARING_PERCENT_CAP,
                    bytesPrepared = prepared,
                    filesTotal = uploadFiles.size,
                )
            }
            val consolidateCompleted = 0
            val consolidateTotal = 0

            // Soft warn is UI-only; allow backup under 100%.
            var uploadedBytes = 0L
            var completed = 0
            var dedupedCount = 0
            var anyDryRun = false
            var remainingQuotaHeadroom =
                if (ent.quotaBytes > 0) (ent.quotaBytes - ent.usedBytes).coerceAtLeast(0L)
                else Long.MAX_VALUE

            // Skip only when cloud already has the same SHA-256 and size. A name match is not identity.
            var skippedCount = 0
            val cloudIdentities: MutableSet<Pair<String, Long>> = runCatching {
                backupApi.listObjects(kind = BackupKinds.SONG).objects
                    .asSequence()
                    .filter { obj ->
                        val k = obj.kind?.takeIf { it.isNotBlank() } ?: BackupKinds.SONG
                        k == BackupKinds.SONG
                    }
                    .mapNotNull { obj ->
                        val hash = obj.contentHash.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        hash.lowercase() to obj.sizeBytes
                    }
                    .toMutableSet()
            }.getOrElse { e ->
                Log.w(TAG, "catalog list for hash skip failed — continuing: ${e.message}")
                mutableSetOf()
            }
            Log.i(TAG, "cloud hash identities=${cloudIdentities.size}")

            publishStep(
                step = BackupJobStep.UPLOADING_FILES,
                phase = CloudBackupPhase.UPLOADING,
                consolidateCompleted = consolidateCompleted,
                consolidateTotal = consolidateTotal,
                uploadCompleted = 0,
                uploadTotal = uploadFiles.size,
            )

            for (file in uploadFiles) {
                throwIfBackupLeaseLost()
                val size = file.length()
                if (size <= 0L) {
                    completed++
                    continue
                }

                val logicalPath = file.absolutePath
                val hash = sha256Hex(file)

                fun markSkipped(reason: String) {
                    skippedCount++
                    completed++
                    cloudIdentities.add(hash.lowercase() to size)
                    Log.i(TAG, "skip $reason hash=$hash size=$size")
                    publishUploadProgress(
                        consolidateCompleted = consolidateCompleted,
                        consolidateTotal = consolidateTotal,
                        uploadCompleted = completed,
                        uploadTotal = uploadFiles.size,
                        prepared = prepared,
                        uploadedBytes = uploadedBytes,
                        dedupedCount = dedupedCount,
                        skippedCount = skippedCount,
                        anyDryRun = anyDryRun,
                    )
                }

                if (CloudHashDedup.alreadyStored(cloudIdentities, hash, size)) {
                    markSkipped("hash")
                    continue
                }

                val match = runCatching {
                    backupApi.matchBackup(
                        BackupMatchRequestDto(
                            logicalPath = logicalPath,
                            sizeBytes = size,
                            contentHash = hash,
                        )
                    )
                }.getOrElse { e ->
                    Log.w(TAG, "match pre-check failed — falling through: ${e.message}")
                    null
                }
                val matchHash = match?.contentHash
                val matchSize = match?.sizeBytes ?: 0L
                if (match?.matched == true &&
                    !matchHash.isNullOrBlank() &&
                    matchHash.equals(hash, ignoreCase = true) &&
                    (matchSize <= 0L || matchSize == size)
                ) {
                    markSkipped("hash-match")
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
                    cloudIdentities.add(hash.lowercase() to size)
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
                    cloudIdentities.add(hash.lowercase() to size)
                }

                completed++
                publishUploadProgress(
                    consolidateCompleted = consolidateCompleted,
                    consolidateTotal = consolidateTotal,
                    uploadCompleted = completed,
                    uploadTotal = uploadFiles.size,
                    prepared = prepared,
                    uploadedBytes = uploadedBytes,
                    dedupedCount = dedupedCount,
                    skippedCount = skippedCount,
                    anyDryRun = anyDryRun,
                )
            }

            publishStep(
                step = BackupJobStep.UPLOADING_CATALOG,
                phase = CloudBackupPhase.UPLOADING,
                consolidateCompleted = consolidateCompleted,
                consolidateTotal = consolidateTotal,
                uploadCompleted = completed,
                uploadTotal = uploadFiles.size,
            )
            throwIfBackupLeaseLost()
            val roomResult = uploadRoomDbSnapshot(remainingQuotaHeadroom)
            anyDryRun = anyDryRun || roomResult.anyDryRun
            dedupedCount += if (roomResult.deduped) 1 else 0
            if (!roomResult.deduped && roomResult.uploadedBytes > 0L) {
                uploadedBytes += roomResult.uploadedBytes
            }
            prepared += roomResult.uploadedBytes

            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_LAST_BACKUP, now)
                .remove(KEY_LAST_ERROR)
                .putBoolean(KEY_CAN_RETRY, false)
                .apply()

            // Refresh /v1/me storage after batch (complete may already have applied).
            runCatching { authRepository.refreshSession() }

            _state.update {
                it.copy(
                    phase = CloudBackupPhase.SUCCESS,
                    jobStep = BackupJobStep.UPLOADING_CATALOG,
                    progressPercent = 100,
                    bytesPrepared = prepared,
                    bytesUploaded = uploadedBytes,
                    filesCompleted = completed,
                    consolidateCompleted = consolidateCompleted,
                    consolidateTotal = consolidateTotal,
                    uploadCompleted = completed,
                    uploadTotal = uploadFiles.size,
                    filesDeduped = dedupedCount,
                    filesSkipped = skippedCount,
                    lastBackupAtEpochMs = now,
                    lastError = null,
                    lastRunDryRun = anyDryRun,
                    canRetry = false,
                    message = buildString {
                        if (uploadFiles.isEmpty()) {
                            append("Library snapshot backed up")
                            if (roomResult.deduped) append(" (unchanged)")
                            append(" — no songs in the library")
                        } else {
                            append("Backed up $completed file(s)")
                            if (skippedCount > 0) append(" · $skippedCount skipped (same content hash)")
                            if (dedupedCount > 0) append(" · $dedupedCount hash-deduped")
                        }
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
                    } finally {
                        heartbeat?.cancel()
                        if (heartbeat != null) runCatching { heartbeat.join() }
                    }
                }
            } finally {
                val releaseId = heldLease?.leaseId
                if (releaseId != null) {
                    withContext(NonCancellable) { releaseBackupLease(deviceId, releaseId) }
                }
                jobGate.release(BackupTransfer.BACKUP)
            }
        }.onFailure { e ->
            if (e is OtherDeviceBackupException) {
                _state.update { it.copy(otherDeviceHoldingLease = true) }
                if (_state.value.phase !in ACTIVE_BACKUP_PHASES) return@onFailure
            }
            if (_state.value.phase !in setOf(
                    CloudBackupPhase.BLOCKED_NOT_PREMIUM,
                    CloudBackupPhase.BLOCKED_GRACE,
                    CloudBackupPhase.BLOCKED_QUOTA,
                )
            ) {
                val current = _state.value
                val msg = BackupProgressLabel.failure(
                    step = current.jobStep,
                    consolidateCompleted = current.consolidateCompleted,
                    consolidateTotal = current.consolidateTotal,
                    uploadCompleted = current.uploadCompleted,
                    uploadTotal = current.uploadTotal,
                    detail = e.message,
                )
                prefs.edit()
                    .putString(KEY_LAST_ERROR, msg)
                    .putBoolean(KEY_CAN_RETRY, true)
                    .apply()
                _state.update {
                    it.copy(
                        phase = CloudBackupPhase.ERROR,
                        lastError = msg,
                        message = msg,
                        canRetry = true,
                    )
                }
            }
        }
    }

    override suspend fun refreshBackupLease() {
        if (!authRepository.isSignedIn()) {
            _state.update { it.copy(otherDeviceHoldingLease = false) }
            return
        }
        if (_state.value.phase.isUploadInProgress()) return
        val generation = leaseRefreshGeneration.get()
        val deviceId = installDeviceId.get()
        val response = try {
            backupApi.getLease(deviceId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "backup lease status failed: ${e.message}")
            return
        }
        if (generation != leaseRefreshGeneration.get()) return
        if (_state.value.phase.isUploadInProgress()) return
        applyLeaseStatus(response, deviceId)
    }

    private sealed class LeaseGate {
        data class Acquired(
            val leaseId: String,
            val heartbeatIntervalMs: Long,
        ) : LeaseGate()

        data object HeldByOther : LeaseGate()
        data object EndpointMissing : LeaseGate()
    }

    private enum class LeaseLoss {
        OTHER_DEVICE,
        NOT_FOUND,
    }

    private suspend fun acquireBackupLease(deviceId: String): LeaseGate {
        leaseRefreshGeneration.incrementAndGet()
        refreshBackupLease()
        val response = try {
            backupApi.acquireLease(
                BackupLeaseRequestDto(
                    deviceId = deviceId,
                    deviceLabel = installDeviceId.deviceName(),
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw IllegalStateException(
                "Can't reach the backup lock. Check the connection, then try again.",
                e,
            )
        }
        val code = response.code()
        if (code == 403) throw mapBackupHttp(HttpException(response))
        if (code == 409) {
            val snapshot = parseLeaseBody(response).toRemoteLease(System.currentTimeMillis())
            if (leaseBlocks(snapshot, deviceId, 409)) {
                _state.update { it.copy(otherDeviceHoldingLease = true) }
                return LeaseGate.HeldByOther
            }
            throw IllegalStateException(
                "Backup lock was refused. Tap Back up now to try again.",
            )
        }
        if (code == 404 || code == 405 || code == 501) {
            val snapshot = parseLeaseBody(response).toRemoteLease(System.currentTimeMillis())
            if (snapshot.code == BackupLeasePolicy.CODE_NOT_FOUND) {
                throw IllegalStateException(
                    "Backup lock expired. Tap Retry backup to start again.",
                )
            }
            if (leaseBlocks(snapshot, deviceId, code)) {
                _state.update { it.copy(otherDeviceHoldingLease = true) }
                return LeaseGate.HeldByOther
            }
            _state.update { it.copy(otherDeviceHoldingLease = false) }
            return LeaseGate.EndpointMissing
        }
        if (!response.isSuccessful) throw mapBackupHttp(HttpException(response))
        val dto = response.body()
        val snapshot = dto.toRemoteLease(System.currentTimeMillis())
        if (leaseBlocks(snapshot, deviceId, code)) {
            _state.update { it.copy(otherDeviceHoldingLease = true) }
            return LeaseGate.HeldByOther
        }
        val body = dto ?: error("backup lease missing lease_id")
        val leaseId = body.leaseId?.takeIf { it.isNotBlank() }
            ?: error("backup lease missing lease_id")
        _state.update { it.copy(otherDeviceHoldingLease = false) }
        return LeaseGate.Acquired(
            leaseId = leaseId,
            heartbeatIntervalMs = BackupLeasePolicy.heartbeatIntervalMs(body.heartbeatIntervalSec),
        )
    }

    private suspend fun heartbeatBackupLease(
        deviceId: String,
        leaseId: String,
        intervalMs: Long,
    ) {
        var interval = intervalMs
        while (currentCoroutineContext().isActive) {
            delay(interval)
            if (!currentCoroutineContext().isActive) return
            try {
                val response = backupApi.heartbeatLease(
                    BackupLeaseRequestDto(deviceId = deviceId, leaseId = leaseId),
                )
                when (response.code()) {
                    200 -> {
                        interval = BackupLeasePolicy.heartbeatIntervalMs(
                            response.body()?.heartbeatIntervalSec,
                        )
                    }
                    409 -> {
                        backupLeaseLost.set(LeaseLoss.OTHER_DEVICE)
                        _state.update { it.copy(otherDeviceHoldingLease = true) }
                        return
                    }
                    404 -> {
                        backupLeaseLost.set(LeaseLoss.NOT_FOUND)
                        return
                    }
                    else -> Log.w(TAG, "lease heartbeat HTTP ${response.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "lease heartbeat failed: ${e.message}")
            }
        }
    }

    private suspend fun releaseBackupLease(deviceId: String, leaseId: String) {
        try {
            val response = backupApi.releaseLease(
                BackupLeaseRequestDto(deviceId = deviceId, leaseId = leaseId),
            )
            if (!response.isSuccessful) {
                Log.w(TAG, "lease release HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "lease release failed: ${e.message}")
        }
    }

    private fun applyLeaseStatus(
        response: retrofit2.Response<BackupLeaseResponseDto>,
        deviceId: String,
    ) {
        val code = response.code()
        if (code == 404 || code == 405 || code == 501) {
            _state.update { it.copy(otherDeviceHoldingLease = false) }
            return
        }
        if (!response.isSuccessful && code != 409) {
            Log.w(TAG, "backup lease status HTTP $code")
            return
        }
        val dto = if (response.isSuccessful) response.body() else parseLeaseBody(response)
        val httpStatus = if (code == 409) 409 else null
        _state.update {
            it.copy(otherDeviceHoldingLease = leaseBlocks(dto.toRemoteLease(System.currentTimeMillis()), deviceId, httpStatus))
        }
    }

    private fun leaseBlocks(
        snapshot: RemoteBackupLease,
        deviceId: String,
        httpStatus: Int?,
    ): Boolean = BackupLeasePolicy.blocksThisDevice(
        lease = snapshot,
        localDeviceId = deviceId,
        nowEpochMs = System.currentTimeMillis(),
        httpStatus = httpStatus,
    )

    private fun parseLeaseBody(
        response: retrofit2.Response<BackupLeaseResponseDto>,
    ): BackupLeaseResponseDto? {
        val raw = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        } ?: return null
        return runCatching { leaseAdapter.fromJson(raw) }.getOrNull()
    }

    private fun BackupLeaseResponseDto?.toRemoteLease(nowEpochMs: Long): RemoteBackupLease {
        if (this == null) return RemoteBackupLease()
        val expiry = AuthMapper.parseIsoEpochMs(expiresAt)
            ?: expiresInSec?.let { nowEpochMs + it * 1000L }
        return RemoteBackupLease(
            active = active,
            heldByThisDevice = heldByThisDevice,
            otherDeviceActive = otherDeviceActive,
            code = code,
            deviceId = deviceId,
            expiresAtEpochMs = expiry,
        )
    }

    private fun throwIfBackupLeaseLost() {
        when (backupLeaseLost.get()) {
            LeaseLoss.OTHER_DEVICE -> throw OtherDeviceBackupException()
            LeaseLoss.NOT_FOUND -> error("Backup lock expired. Tap Retry backup to start again.")
            null -> Unit
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
            transferWholeObject(
                contentHash = contentHash,
                r2Key = r2Key,
                destFile = destFile,
                seededUrl = null,
            )
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
     * A dropped socket or rejected signature throws [R2GetException] so the caller can remint.
     */
    private fun getFromR2(
        downloadUrl: String,
        destFile: File,
        onBytes: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
    ) {
        val request = Request.Builder()
            .url(downloadUrl)
            .get()
            // Explicit: no Range header (r2HttpClient also strips any Range).
            .build()
        check(request.header("Range") == null) {
            "R2 cost rule violated: Range header on GetObject"
        }
        try {
            r2HttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw R2GetException(
                        httpCode = response.code,
                        message = "R2 GET failed HTTP ${response.code}",
                    )
                }
                val body = response.body ?: throw R2GetException(
                    httpCode = null,
                    message = "R2 GET empty body",
                )
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { out ->
                    body.byteStream().use { input ->
                        copyDownload(input, out, body.contentLength(), onBytes)
                    }
                }
            }
        } catch (e: R2GetException) {
            destFile.delete()
            throw e
        } catch (e: IOException) {
            destFile.delete()
            throw R2GetException(
                httpCode = null,
                message = e.message?.takeIf { it.isNotBlank() } ?: "connection abort",
                cause = e,
            )
        }
    }

    private fun copyDownload(
        input: java.io.InputStream,
        out: FileOutputStream,
        contentLength: Long,
        onBytes: ((bytesRead: Long, totalBytes: Long) -> Unit)?,
    ) {
        var readTotal = 0L
        var sinceEmit = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            readTotal += read
            sinceEmit += read
            if (onBytes != null && sinceEmit >= ContentHash.PROGRESS_STEP_BYTES) {
                emitDownloadBytes(onBytes, readTotal, contentLength)
                sinceEmit = 0L
            }
        }
        if (onBytes != null && (sinceEmit > 0L || readTotal == 0L)) {
            emitDownloadBytes(onBytes, readTotal, contentLength)
        }
    }

    private fun emitDownloadBytes(
        onBytes: (bytesRead: Long, totalBytes: Long) -> Unit,
        read: Long,
        total: Long,
    ) {
        runCatching { onBytes(read, total) }
    }

    /**
     * Mint (or reuse) a signed URL, then GET the whole object.
     * A dropped connection or rejected signature retries that one object.
     * A dead session still stops the restore.
     */
    private suspend fun transferWholeObject(
        contentHash: String?,
        r2Key: String?,
        destFile: File,
        seededUrl: String?,
        onBytes: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
        onRetry: ((retryNumber: Int, maxAttempts: Int) -> Unit)? = null,
    ) {
        var lastError: Exception? = null
        for (attempt in 1..RestoreDownloadRetry.MAX_ATTEMPTS) {
            coroutineContext.ensureActive()
            try {
                val url = if (attempt == 1 && !seededUrl.isNullOrBlank()) {
                    seededUrl
                } else {
                    val resolved = resolveDownloadUrl(
                        contentHash = contentHash,
                        r2Key = r2Key,
                        forceRefresh = attempt > 1,
                    )
                    if (resolved.dryRun) {
                        Log.i(TAG, "dry_run skip GET download key=${resolved.r2Key}")
                        destFile.parentFile?.mkdirs()
                        destFile.writeBytes(ByteArray(0))
                        return
                    }
                    resolved.url ?: error("download-url missing download_url")
                }
                getFromR2(url, destFile, onBytes)
                return
            } catch (e: CancellationException) {
                destFile.delete()
                throw e
            } catch (e: Exception) {
                destFile.delete()
                coroutineContext.ensureActive()
                lastError = e
                if (RestoreDownloadRetry.isTerminalUnauthorized(e)) throw e
                val httpCode = (e as? R2GetException)?.httpCode
                val retry = attempt < RestoreDownloadRetry.MAX_ATTEMPTS &&
                    when (e) {
                        is R2GetException -> RestoreDownloadRetry.r2GetShouldRetry(httpCode, e)
                        is IOException -> RestoreDownloadRetry.isTransientTransferError(e)
                        else -> false
                    }
                if (!retry) throw e
                runCatching { onRetry?.invoke(attempt, RestoreDownloadRetry.MAX_ATTEMPTS) }
                Log.w(
                    TAG,
                    "restore GET retry $attempt/${RestoreDownloadRetry.MAX_ATTEMPTS} " +
                        "hash=$contentHash key=$r2Key",
                    e,
                )
                evictDownloadUrl(contentHash, r2Key)
                delay(RestoreDownloadRetry.backoffMs(attempt))
            }
        }
        throw lastError ?: IllegalStateException("R2 GET failed")
    }

    /**
     * Resolve a signed download URL with in-session reuse until near expiry
     * (R2 cost rule 3). Remint only on miss, after a failed GET, or when TTL
     * is under [DOWNLOAD_URL_REMIN_SKEW_MS].
     */
    private suspend fun resolveDownloadUrl(
        contentHash: String?,
        r2Key: String?,
        forceRefresh: Boolean = false,
    ): ResolvedDownloadUrl {
        val primaryKey = downloadCacheKey(contentHash, r2Key)
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
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
        }
        val resp = try {
            callRestoreApi {
                backupApi.requestDownloadUrl(
                    BackupDownloadUrlRequestDto(
                        contentHash = contentHash,
                        r2Key = r2Key,
                    )
                )
            }
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

    private fun evictDownloadUrl(contentHash: String?, r2Key: String?) {
        contentHash?.takeIf { it.isNotBlank() }?.let { downloadUrlCache.remove("h:$it") }
        r2Key?.takeIf { it.isNotBlank() }?.let { downloadUrlCache.remove("k:$it") }
    }

    /**
     * Access tokens expire in 15 minutes. A 401 is `{ "error": "unauthorized" }`.
     * Refresh once and repeat the call. A failed refresh leaves the 401 in place.
     * A dropped socket ("connection abort") retries the same small JSON call.
     */
    private suspend fun <T> callRestoreApi(block: suspend () -> T): T {
        var lastError: Exception? = null
        for (attempt in 1..RestoreDownloadRetry.MAX_ATTEMPTS) {
            coroutineContext.ensureActive()
            try {
                return withFreshAccess(block)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                throw e
            } catch (e: IOException) {
                lastError = e
                if (attempt == RestoreDownloadRetry.MAX_ATTEMPTS ||
                    !RestoreDownloadRetry.isTransientTransferError(e)
                ) {
                    throw e
                }
                Log.w(TAG, "restore API retry $attempt/${RestoreDownloadRetry.MAX_ATTEMPTS}", e)
                delay(RestoreDownloadRetry.backoffMs(attempt))
            }
        }
        throw lastError ?: IllegalStateException("backup API failed")
    }

    private suspend fun <T> withFreshAccess(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            if (!RestoreDownloadRetry.apiNeedsAccessRefresh(e.code())) throw e
            Log.w(TAG, "backup API 401 — refreshing access token")
            val refreshed = authRepository.refreshSession()
            if (refreshed.isFailure) throw e
            return block()
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
            401 -> {
                val detail = apiError.ifBlank { "unauthorized" }
                "$detail — session expired. Sign in again, then retry restore."
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

    private fun restoredBackupState(): CloudBackupState {
        val retry = prefs.getBoolean(KEY_CAN_RETRY, false)
        val error = prefs.getString(KEY_LAST_ERROR, null)
        return CloudBackupState(
            phase = if (retry) CloudBackupPhase.ERROR else CloudBackupPhase.IDLE,
            lastBackupAtEpochMs = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0L },
            lastError = error,
            message = if (retry) error else null,
            canRetry = retry,
        )
    }

    private fun publishStep(
        step: BackupJobStep,
        phase: CloudBackupPhase,
        consolidateCompleted: Int,
        consolidateTotal: Int,
        uploadCompleted: Int,
        uploadTotal: Int,
    ) {
        val completed = when (step) {
            BackupJobStep.UPLOADING_FILES -> uploadCompleted
            else -> 0
        }
        val total = when (step) {
            BackupJobStep.UPLOADING_FILES -> uploadTotal
            else -> 0
        }
        _state.update {
            val computed = BackupProgressLabel.percent(step, completed, total)
            val percent = if (step == BackupJobStep.PREPARING) {
                computed
            } else {
                max(it.progressPercent, computed).coerceAtMost(99)
            }
            it.copy(
                phase = phase,
                jobStep = step,
                progressPercent = percent,
                filesCompleted = completed,
                filesTotal = total,
                consolidateCompleted = consolidateCompleted,
                consolidateTotal = consolidateTotal,
                uploadCompleted = uploadCompleted,
                uploadTotal = uploadTotal,
                message = BackupProgressLabel.status(
                    step,
                    consolidateCompleted,
                    consolidateTotal,
                    uploadCompleted,
                    uploadTotal,
                ),
                canRetry = false,
            )
        }
    }

    private fun publishUploadProgress(
        consolidateCompleted: Int,
        consolidateTotal: Int,
        uploadCompleted: Int,
        uploadTotal: Int,
        prepared: Long,
        uploadedBytes: Long,
        dedupedCount: Int,
        skippedCount: Int,
        anyDryRun: Boolean,
    ) {
        publishStep(
            step = BackupJobStep.UPLOADING_FILES,
            phase = CloudBackupPhase.UPLOADING,
            consolidateCompleted = consolidateCompleted,
            consolidateTotal = consolidateTotal,
            uploadCompleted = uploadCompleted,
            uploadTotal = uploadTotal,
        )
        _state.update {
            it.copy(
                bytesPrepared = prepared,
                bytesUploaded = uploadedBytes,
                filesDeduped = dedupedCount,
                filesSkipped = skippedCount,
                lastRunDryRun = anyDryRun,
            )
        }
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

    override suspend fun stageLibraryRestore(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_state.value.phase.isUploadInProgress()) {
            return@withContext Result.failure(IllegalStateException(BackupJobGate.BUSY_MESSAGE))
        }
        val alreadyHeld = jobGate.current() == BackupTransfer.RESTORE
        if (!alreadyHeld && !jobGate.tryAcquire(BackupTransfer.RESTORE)) {
            return@withContext Result.failure(IllegalStateException(BackupJobGate.BUSY_MESSAGE))
        }
        // Hold the gate through apply. A resume after process death has an empty gate and
        // must still be allowed to continue the persisted SCRUM-67 phase.
        var holdForApply = alreadyHeld
        try {
            restoreProgress.reset()
            publishRestore()
            // Same refresh backup does before upload. Without it, a token older than
            // 15 minutes makes the first library call return "unauthorized".
            val refreshed = authRepository.refreshSession()
            if (refreshed.isFailure) {
                Log.w(
                    TAG,
                    "restore access refresh failed; continuing with current token",
                    refreshed.exceptionOrNull(),
                )
            }
            val phaseAtStart = restoreSession.phase()
            if (phaseAtStart == RestorePhase.IDLE || phaseAtStart == RestorePhase.COMMITTED) {
                // A fresh job only. A kill while DOWNLOADING must not wipe a finished snapshot.
                restoreSession.beginDownload()
            }
            val reuseSnapshot = phaseAtStart != RestorePhase.IDLE &&
                phaseAtStart != RestorePhase.DOWNLOADING &&
                phaseAtStart != RestorePhase.COMMITTED &&
                restoreSession.peekStagingVerdict() == StagingVerdict.VALID
            if (!reuseSnapshot) {
                if (restoreSession.phase() != RestorePhase.DOWNLOADING) {
                    restoreSession.beginDownload()
                }
                downloadLibrarySnapshot()
                restoreSession.markStaged()
            }
            ensureCloudSongs(restoreSession.stagingDatabase())
            restoreSession.markFilesReady()
            Log.i(TAG, "restore files ready at ${restoreSession.stagingDatabase().absolutePath}")
            holdForApply = true
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (restoreSession.peekStagingVerdict() != StagingVerdict.VALID) {
                restoreSession.discard()
            }
            Result.failure(e)
        } finally {
            if (!holdForApply) jobGate.release(BackupTransfer.RESTORE)
        }
    }

    override suspend fun applyStagedLibraryRestore(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_state.value.phase.isUploadInProgress()) {
            return@withContext Result.failure(IllegalStateException(BackupJobGate.BUSY_MESSAGE))
        }
        val alreadyHeld = jobGate.current() == BackupTransfer.RESTORE
        if (!alreadyHeld && !jobGate.tryAcquire(BackupTransfer.RESTORE)) {
            return@withContext Result.failure(IllegalStateException(BackupJobGate.BUSY_MESSAGE))
        }
        try {
            val phase = restoreSession.phase()
            if (phase != RestorePhase.FILES_READY && phase != RestorePhase.SWAPPING) {
                val staged = stageLibraryRestore()
                if (staged.isFailure) return@withContext staged
            } else {
                ensureCloudSongs(restoreSession.stagingDatabase())
            }
            val staged = restoreSession.stagingDatabase()
            if (restoreSession.peekStagingVerdict() != StagingVerdict.VALID) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Restored library file was incomplete. Your music was not changed.",
                    ),
                )
            }
            checkpointLiveDatabase()
            restoreSession.markSwapping()
            val swap = RoomDbSwapFiles.forContext(context)
            swap.arm(staged)
            swap.commit()
            restoreSession.markCommitted()
            Log.i(TAG, "library database swapped; restarting onto the restored file")
            suppressLoginPromptForRestoredBackup()
            ProcessRestarter.restart(context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val swap = RoomDbSwapFiles.forContext(context)
            if (swap.step() == DbSwapStep.COMMITTED) {
                restoreSession.markCommitted()
                suppressLoginPromptForRestoredBackup()
                ProcessRestarter.restart(context)
            }
            runCatching { swap.rollback() }
            if (restoreSession.peekStagingVerdict() == StagingVerdict.VALID) {
                restoreSession.markFilesReady()
            }
            Result.failure(e)
        } finally {
            jobGate.release(BackupTransfer.RESTORE)
        }
    }

    private suspend fun downloadLibrarySnapshot() {
        val resp = try {
            callRestoreApi { backupApi.getLibrary() }
        } catch (e: HttpException) {
            throw mapBackupHttp(e)
        }
        val lib = resp.library
            ?: error("No cloud library snapshot yet — back up first.")
        val remoteSchema = lib.schemaVersion
            ?: error("Cloud library missing schema_version")
        val localSchema = GroovePlayerDatabase.SCHEMA_VERSION
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
        restoreSession.rememberLibraryIdentity(
            CloudLibrarySnapshot(
                objectId = lib.objectId,
                contentHash = lib.contentHash,
                sizeBytes = lib.sizeBytes,
                r2Key = lib.r2Key,
                logicalPath = lib.logicalPath,
                schemaVersion = remoteSchema,
                appVersion = lib.appVersion,
                createdAtIso = lib.createdAt,
                dryRun = false,
            ),
        )
        val snapshotBytes = lib.sizeBytes
        restoreProgress.onLibrarySnapshot(0L, snapshotBytes)
        publishRestore()
        val expiresInSec = (lib.expiresInSec ?: DEFAULT_DOWNLOAD_EXPIRES_SEC).coerceAtLeast(1)
        rememberDownloadUrl(
            CachedSignedDownload(
                url = url,
                r2Key = lib.r2Key,
                contentHash = lib.contentHash,
                expiresAtEpochMs = System.currentTimeMillis() + expiresInSec * 1000L,
            ),
        )
        val gz = restoreSession.partialGzip()
        val raw = restoreSession.partialDatabase()
        transferWholeObject(
            contentHash = lib.contentHash,
            r2Key = lib.r2Key,
            destFile = gz,
            seededUrl = url,
            onBytes = { read, length ->
                val total = if (snapshotBytes > 0L) snapshotBytes else length
                restoreProgress.onLibrarySnapshot(read, total)
                publishRestore()
            },
            onRetry = { retryNumber, maxAttempts ->
                restoreProgress.onSnapshotRetry(retryNumber, maxAttempts)
                publishRestore()
            },
        )
        gunzipFile(gz, raw)
        val staged = restoreSession.stagingDatabase()
        if (staged.exists()) staged.delete()
        if (!raw.renameTo(staged)) {
            raw.copyTo(staged, overwrite = true)
            raw.delete()
        }
        gz.delete()
        when (val verdict = restoreSession.peekStagingVerdict()) {
            StagingVerdict.VALID -> Unit
            StagingVerdict.NEWER_THAN_APP -> error(
                "Cloud library schema is newer than this app. Update GroovePlayer before restoring.",
            )
            StagingVerdict.TRUNCATED, StagingVerdict.NOT_SQLITE, null -> error(
                "Downloaded library file was incomplete. Your music was not changed.",
            )
        }
        Log.i(TAG, "library snapshot staged schema=$remoteSchema path=${staged.absolutePath}")
    }


    /**
     * Copy readable leftovers from shared Music/Groove Downloads (and older app folders)
     * into the private library and point Room at the new paths. Shared leftovers are left
     * in place so a MediaProvider EPERM never becomes a user cleanup task.
     */
    private suspend fun adoptLegacyLibraryFiles(privateRoot: File) {
        val plan = LegacyLibraryAdoption.plan(grooveDownloads.legacyDirectories(), privateRoot)
        for (item in plan) {
            if (item.needsBytes) {
                if (item.destination.exists()) {
                    Log.w(TAG, "Refusing to overwrite ${item.destination.absolutePath}")
                    continue
                }
                try {
                    RoomDbSwapFiles.copyDurable(item.source, item.destination)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Left leftover library file in place: ${item.source.absolutePath}", e)
                    continue
                }
                if (!item.destination.isFile || item.destination.length() != item.source.length()) {
                    item.destination.delete()
                    continue
                }
            }
            val aliases = buildList {
                add(item.source.absolutePath)
                runCatching { item.source.canonicalPath }.getOrNull()?.let { add(it) }
            }.distinct()
            for (old in aliases) {
                database.songDao().retargetSourcePath(old, item.destination.absolutePath)
            }
            if (item.deleteSourceAfterCopy && !item.source.delete()) {
                Log.i(TAG, "App-owned leftover kept at ${item.source.absolutePath}")
            }
        }
    }

    private fun grooveDownloadFiles(dir: File): List<File> {
        val children = dir.listFiles() ?: return emptyList()
        return children.filter { file ->
            file.isFile && file.length() > 0L && !AppPrivateLibrary.isScratchFile(file.name)
        }
    }

    private fun indexDownloads(dir: File): List<HashedAudio> {
        return grooveDownloadFiles(dir).map { file ->
            HashedAudio(file.absolutePath, sha256Hex(file), file.length())
        }
    }

    /**
     * Download cloud songs that are not already in the app library under the same hash,
     * rewrite the staged catalog to those paths, and refuse to swap if any required file is missing.
     * Tracks that were never uploaded are not required and their on-disk files are not deleted.
     */
    private suspend fun ensureCloudSongs(stagedDb: File) {
        val objects = try {
            callRestoreApi { backupApi.listObjects(kind = BackupKinds.SONG).objects }
        } catch (e: HttpException) {
            throw mapBackupHttp(e)
        }
        val songs = objects.filter { obj ->
            val kind = obj.kind?.takeIf { it.isNotBlank() } ?: BackupKinds.SONG
            kind == BackupKinds.SONG && obj.contentHash.isNotBlank()
        }
        val downloadsDir = grooveDownloads.directory()
        adoptLegacyLibraryFiles(downloadsDir)
        val existing = indexDownloads(downloadsDir).toMutableList()
        val plannedSizes = plannedDownloadSizes(songs, downloadsDir, existing)
        restoreProgress.planDownloads(plannedSizes)
        publishRestore()
        val placements = mutableListOf<PlacedCloudSong>()
        var downloadIndex = 0
        for (obj in songs) {
            val hash = obj.contentHash
            val size = obj.sizeBytes
            val cosmetic = obj.logicalPath?.takeIf { it.isNotBlank() }?.let(GrooveDownloadPlacement::fileName)
                ?: GrooveDownloadPlacement.hashedFileName("audio.bin", hash)
            val placement = GrooveDownloadPlacement.place(
                downloadsDir = downloadsDir.absolutePath,
                cosmeticFileName = cosmetic,
                contentHash = hash,
                sizeBytes = size,
                existing = existing,
            )
            val dest = File(placement.destinationPath)
            if (!placement.reusedExisting) {
                val index = downloadIndex
                val tracked = index < plannedSizes.size
                if (tracked) {
                    restoreProgress.beginFile(index)
                    publishRestore()
                }
                val partial = File(stagedDb.parentFile, "$hash.partial")
                transferWholeObject(
                    contentHash = hash,
                    r2Key = obj.r2Key,
                    destFile = partial,
                    seededUrl = null,
                    onBytes = { read, length ->
                        if (!tracked) return@transferWholeObject
                        val total = if (size > 0L) size else length
                        restoreProgress.onFileBytes(index, read, total)
                        publishRestore()
                    },
                    onRetry = { retryNumber, maxAttempts ->
                        if (!tracked) return@transferWholeObject
                        restoreProgress.onRetry(index, retryNumber, maxAttempts)
                        publishRestore()
                    },
                )
                if (size > 0L && partial.length() != size) {
                    partial.delete()
                    error("Downloaded song size does not match the cloud catalog")
                }
                if (tracked) {
                    restoreProgress.beginVerify(index)
                    publishRestore()
                }
                val got = sha256Hex(partial) { read, total ->
                    if (!tracked) return@sha256Hex
                    restoreProgress.onVerifyBytes(index, read, total)
                    publishRestore()
                }
                if (!got.equals(hash, ignoreCase = true)) {
                    partial.delete()
                    error("Downloaded song hash does not match the cloud catalog")
                }
                RoomDbSwapFiles.copyDurable(partial, dest)
                partial.delete()
                existing += HashedAudio(dest.absolutePath, hash, dest.length())
                if (tracked) {
                    restoreProgress.finishFile(index)
                    publishRestore()
                    downloadIndex++
                }
            } else if (size > 0L && dest.length() != size) {
                error("The app library already has different bytes for this song")
            }
            placements += PlacedCloudSong(
                contentHash = hash,
                sizeBytes = size,
                logicalPath = obj.logicalPath,
                localPath = dest.absolutePath,
            )
        }
        StagedCatalogRewriter.rewrite(stagedDb, placements)
        val missing = BackupCatalogPaths.missingLocalBytes(placements) { path ->
            val file = File(path)
            if (!file.isFile) -1L else file.length()
        }
        if (missing.isNotEmpty()) {
            error(
                "Restore is missing ${missing.size} song file(s) in the app library. " +
                    "The library database was not replaced.",
            )
        }
        // Hashing each file is faster than a frame, so "Verifying N of M" never
        // painted between "Downloading M of M" and Applying. Hold it once here.
        if (plannedSizes.isNotEmpty()) {
            restoreProgress.showVerifyingFiles()
        } else {
            restoreProgress.onCatalogVerify()
        }
        publishRestore()
        delay(RestoreProgressLabel.MIN_VERIFY_VISIBLE_MS)
    }

    /**
     * Files this restore will download, in loop order. Same placement rules as the
     * download loop, so the count matches real work and reused files stay out of it.
     */
    private fun plannedDownloadSizes(
        songs: List<BackupObjectDto>,
        downloadsDir: File,
        existing: List<HashedAudio>,
    ): List<Long?> {
        val sim = existing.toMutableList()
        val sizes = mutableListOf<Long?>()
        for (obj in songs) {
            val hash = obj.contentHash
            val size = obj.sizeBytes
            val cosmetic = obj.logicalPath?.takeIf { it.isNotBlank() }
                ?.let(GrooveDownloadPlacement::fileName)
                ?: GrooveDownloadPlacement.hashedFileName("audio.bin", hash)
            val placement = GrooveDownloadPlacement.place(
                downloadsDir = downloadsDir.absolutePath,
                cosmeticFileName = cosmetic,
                contentHash = hash,
                sizeBytes = size,
                existing = sim,
            )
            if (!placement.reusedExisting) {
                sizes += size.takeIf { it > 0L }
                sim += HashedAudio(placement.destinationPath, hash, size)
            }
        }
        return sizes
    }

    /**
     * After a committed swap, remember this cloud backup so the login restore
     * prompt does not ask again for the revision that was just applied.
     */
    private suspend fun suppressLoginPromptForRestoredBackup() {
        val snap = restoreSession.libraryIdentity()
            ?: fetchCloudLibraryMetadata().getOrNull()
            ?: return
        val revision = LoginRestorePrompt.revisionOf(snap) ?: return
        val userId = authRepository.getAuthUser()?.id?.trim().orEmpty()
        if (userId.isEmpty()) return
        runCatching {
            loginRestorePromptMemory.markRestored(LoginRestorePrompt.memoryKey(userId, revision))
        }
    }

    private fun checkpointLiveDatabase() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
            cursor.moveToFirst()
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

    private fun sha256Hex(
        file: File,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
    ): String = ContentHash.sha256(file, onProgress)

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
        private const val PREFS = LibraryRestoreSession.PREFS
        private const val KEY_LAST_BACKUP = "last_backup_at"
        private const val KEY_LAST_ERROR = LibraryRestoreSession.KEY_LAST_ERROR
        private const val KEY_CAN_RETRY = "backup_can_retry"
        private val ACTIVE_BACKUP_PHASES = setOf(
            CloudBackupPhase.PREPARING,
            CloudBackupPhase.UPLOADING,
        )
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

/** Another install holds the backup lease. The primary button uses the exact disabled label. */
private class OtherDeviceBackupException :
    IllegalStateException(BackupLeaseCopy.OTHER_DEVICE_BUTTON)
