package com.aethelsoft.grooveplayer.di

import com.aethelsoft.grooveplayer.BuildConfig
import com.aethelsoft.grooveplayer.data.auth.SecureTokenStore
import com.aethelsoft.grooveplayer.data.remote.api.AuthApi
import com.aethelsoft.grooveplayer.data.remote.api.BackupApi
import com.aethelsoft.grooveplayer.data.remote.api.BillingApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    /**
     * Plain client for R2 signed PUT/GET — must NOT attach Bearer.
     * Longer timeouts for large audio uploads.
     *
     * R2 cost rules:
     * - One whole-object GetObject (no Range spam)
     * - No client HeadObject (HEAD rejected; complete does server Head once)
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
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .callTimeout(10, TimeUnit.MINUTES)
            .addInterceptor(r2CostGuard)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
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
}
