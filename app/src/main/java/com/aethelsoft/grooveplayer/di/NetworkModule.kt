package com.aethelsoft.grooveplayer.di

import com.aethelsoft.grooveplayer.BuildConfig
import com.aethelsoft.grooveplayer.data.auth.SecureTokenStore
import com.aethelsoft.grooveplayer.data.auth.TokenRefreshAuthenticator
import com.aethelsoft.grooveplayer.data.remote.api.AuthApi
import com.aethelsoft.grooveplayer.data.remote.api.BackupApi
import com.aethelsoft.grooveplayer.data.remote.api.BillingApi
import com.aethelsoft.grooveplayer.data.remote.api.PlaybackApi
import com.aethelsoft.grooveplayer.data.remote.Ipv4FirstDns
import com.aethelsoft.grooveplayer.data.remote.R2Dns
import com.aethelsoft.grooveplayer.domain.backup.R2Connect
import com.aethelsoft.grooveplayer.domain.backup.RestoreDownloadRetry
import com.aethelsoft.grooveplayer.domain.auth.StartupSessionRecovery
import com.aethelsoft.grooveplayer.domain.network.Ipv4First
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val R2_HTTP_CLIENT = "r2HttpClient"
    const val TOKEN_REFRESH_API = "tokenRefreshApi"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenStore: SecureTokenStore): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val path = original.url.encodedPath
        // Skip only unauthenticated auth endpoints. Logout needs Bearer access
        // so the server can revoke all refresh tokens for the user.
        val skipAuth = path == "/v1/auth/google" ||
            path == "/v1/auth/refresh" ||
            path == "/healthz"
        val access = tokenStore.getAccessToken()
        val request = if (!skipAuth && !access.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $access")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    /**
     * Refresh calls must not use the API client: that client's authenticator
     * would call refresh again and deadlock on a 401.
     */
    @Provides
    @Singleton
    @Named(TOKEN_REFRESH_API)
    fun provideTokenRefreshAuthApi(moshi: Moshi): AuthApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .backendDns()
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
        return retrofit(client, moshi).create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTokenRefreshAuthenticator(
        tokenStore: SecureTokenStore,
        @Named(TOKEN_REFRESH_API) refreshApi: AuthApi,
    ): Authenticator = TokenRefreshAuthenticator(tokenStore, refreshApi)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        tokenRefreshAuthenticator: Authenticator,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        // IPv4 before IPv6, short handshake. A blackholed AAAA route used to
        // outlast the 12s startup cap, so /v1/me never ran and Profile hid quota.
        return OkHttpClient.Builder()
            .backendDns()
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(StartupSessionRecovery.RETRY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(tokenRefreshAuthenticator)
            .build()
    }

    /**
     * Plain client for R2 signed PUT/GET — must NOT attach Bearer.
     * Read/write timeouts cover stalled sockets. There is no call timeout:
     * a 10-minute cap aborted long Call/Voice downloads (connection abort)
     * before restore could reach Applying. HTTP/1.1 avoids HTTP/2 stream resets
     * on those whole-object bodies.
     *
     * Connect is short, and DNS prefers A records. The library snapshot is the
     * first request to R2; a 30s connect timeout on blackholed AAAA addresses
     * held "0 B" for about a minute before any snapshot byte arrived.
     *
     * R2 cost rules for backup restore only:
     * - One whole-object GetObject (no Range spam)
     * - No client HeadObject (HEAD rejected; complete does server Head once)
     *
     * Playback `stream_url` does not use this client. ExoPlayer may Range-GET it.
     */
    @Provides
    @Singleton
    @Named(R2_HTTP_CLIENT)
    fun provideR2OkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        // Cost guard: strip Range (force whole-object GET) and refuse HEAD.
        val r2CostGuard = Interceptor { chain ->
            val req = chain.request()
            if (req.method.equals("HEAD", ignoreCase = true)) {
                throw IllegalStateException(
                    "R2 cost rule: client must not HeadObject — " +
                        "HeadObject is only allowed server-side on POST /v1/backup/complete",
                )
            }
            val range = req.header("Range")
            val next = if (range != null) {
                android.util.Log.w(
                    "R2Http",
                    "R2 cost rule: stripping Range header ($range) — one whole-object GetObject only",
                )
                req.newBuilder().removeHeader("Range").build()
            } else {
                req
            }
            chain.proceed(next)
        }
        val builder = OkHttpClient.Builder()
            .dns(R2Dns())
            .connectTimeout(R2Connect.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .callTimeout(RestoreDownloadRetry.R2_CALL_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        if (RestoreDownloadRetry.R2_HTTP1_ONLY) {
            builder.protocols(listOf(Protocol.HTTP_1_1))
        }
        return builder
            .addInterceptor(r2CostGuard)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit = retrofit(client, moshi)

    /** Shared by every Retrofit client that talks to grooveplayer-backend. */
    private fun OkHttpClient.Builder.backendDns(): OkHttpClient.Builder =
        dns(Ipv4FirstDns())
            .connectTimeout(Ipv4First.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)

    private fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
        val base = BuildConfig.API_BASE_URL.trimEnd('/') + "/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideBillingApi(retrofit: Retrofit): BillingApi = retrofit.create(BillingApi::class.java)

    @Provides
    @Singleton
    fun provideBackupApi(retrofit: Retrofit): BackupApi = retrofit.create(BackupApi::class.java)

    @Provides
    @Singleton
    fun providePlaybackApi(retrofit: Retrofit): PlaybackApi =
        retrofit.create(PlaybackApi::class.java)
}
