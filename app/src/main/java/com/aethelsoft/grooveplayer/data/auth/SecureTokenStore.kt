package com.aethelsoft.grooveplayer.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists access/refresh JWTs in EncryptedSharedPreferences.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Fallback for emulator/keystore edge cases — still private to the app.
        android.util.Log.w(TAG, "EncryptedSharedPreferences unavailable, using private prefs", e)
        context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    fun getTokenType(): String = prefs.getString(KEY_TYPE, "Bearer") ?: "Bearer"

    fun saveTokens(accessToken: String, refreshToken: String, tokenType: String = "Bearer") {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .putString(KEY_TYPE, tokenType)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasSession(): Boolean = !getRefreshToken().isNullOrBlank()

    companion object {
        private const val TAG = "SecureTokenStore"
        private const val PREFS_NAME = "grooveplayer_secure_auth"
        private const val PREFS_NAME_FALLBACK = "grooveplayer_auth_fallback"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_TYPE = "token_type"
    }
}
