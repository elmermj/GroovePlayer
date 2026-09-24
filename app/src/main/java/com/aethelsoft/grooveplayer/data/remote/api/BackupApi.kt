package com.aethelsoft.grooveplayer.data.remote.api

import com.aethelsoft.grooveplayer.data.remote.dto.BackupCompleteRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupCompleteResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupLeaseRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupLeaseResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupDeleteResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupDownloadUrlRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupDownloadUrlResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupLibraryResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupMatchRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupMatchResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupObjectsResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupTrimRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupTrimResponseDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupUploadUrlRequestDto
import com.aethelsoft.grooveplayer.data.remote.dto.BackupUploadUrlResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Benny backup <-> R2 — docs/backup-api.md + docs/backup-library.md
 *
 * Auth via NetworkModule Bearer interceptor.
 * R2 PUT/GET uses a separate plain OkHttpClient (no Bearer on signed URLs).
 * Trim never deletes kind=room_db; explicit DELETE resets cloud library snapshot.
 */
interface BackupApi {
    @POST("/v1/backup/upload-url")
    suspend fun requestUploadUrl(@Body body: BackupUploadUrlRequestDto): BackupUploadUrlResponseDto

    /** Pre-check: basename(logical_path)+size_bytes (optional content_hash). */
    @POST("/v1/backup/match")
    suspend fun matchBackup(@Body body: BackupMatchRequestDto): BackupMatchResponseDto

    @POST("/v1/backup/complete")
    suspend fun completeUpload(@Body body: BackupCompleteRequestDto): BackupCompleteResponseDto

    @POST("/v1/backup/download-url")
    suspend fun requestDownloadUrl(@Body body: BackupDownloadUrlRequestDto): BackupDownloadUrlResponseDto

    @GET("/v1/backup/objects")
    suspend fun listObjects(@Query("kind") kind: String? = null): BackupObjectsResponseDto

    /** Room DB snapshot metadata + download_url. */
    @GET("/v1/backup/library")
    suspend fun getLibrary(): BackupLibraryResponseDto

    @DELETE("/v1/backup/objects/{id}")
    suspend fun deleteObject(@Path("id") id: String): Response<BackupDeleteResponseDto>

    @POST("/v1/backup/trim")
    suspend fun trimBackup(@Body body: BackupTrimRequestDto): BackupTrimResponseDto

    /**
     * Cross-device backup lease (SCRUM-73).
     *
     * - GET  /v1/backup/lease?device_id= — status. `device_id` lets the server set
     *   held_by_this_device; the client also compares device_id itself.
     * - POST /v1/backup/lease — acquire. Same device_id may re-acquire (200).
     *   Another device holding a non-expired lease → 409.
     * - POST /v1/backup/lease/heartbeat — extend TTL while consolidating/uploading.
     * - POST /v1/backup/lease/release — idempotent release for this device_id.
     *
     * Assumed lease TTL is at least 45s. The client heartbeats every 15s.
     * 404/405/501 means the route is not deployed yet; backup then proceeds without the lock.
     */
    @GET("/v1/backup/lease")
    suspend fun getLease(@Query("device_id") deviceId: String): Response<BackupLeaseResponseDto>

    @POST("/v1/backup/lease")
    suspend fun acquireLease(@Body body: BackupLeaseRequestDto): Response<BackupLeaseResponseDto>

    @POST("/v1/backup/lease/heartbeat")
    suspend fun heartbeatLease(@Body body: BackupLeaseRequestDto): Response<BackupLeaseResponseDto>

    @POST("/v1/backup/lease/release")
    suspend fun releaseLease(@Body body: BackupLeaseRequestDto): Response<BackupLeaseResponseDto>
}
