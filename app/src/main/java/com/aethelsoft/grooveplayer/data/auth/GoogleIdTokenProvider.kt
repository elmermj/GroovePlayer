package com.aethelsoft.grooveplayer.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.aethelsoft.grooveplayer.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Obtains a Google ID token via Credential Manager.
 * Uses the **Web** client ID as serverClientId (required for backend verify).
 * Android client ID must be registered in Google Cloud for this package.
 */
@Singleton
class GoogleIdTokenProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    suspend fun requestIdToken(activity: Activity): String {
        val credentialManager = CredentialManager.create(activity)
        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

        // Prefer One Tap / authorized accounts first; fall back to explicit Sign-In button flow.
        return try {
            requestViaGoogleIdOption(credentialManager, activity, serverClientId)
        } catch (e: NoCredentialException) {
            requestViaSignInWithGoogle(credentialManager, activity, serverClientId)
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (e: Exception) {
            // Retry with explicit SIWG if One Tap path fails for other recoverable reasons.
            try {
                requestViaSignInWithGoogle(credentialManager, activity, serverClientId)
            } catch (fallback: Exception) {
                throw e
            }
        }
    }

    /**
     * Clears Credential Manager / Google sign-in state so the next Sign-In
     * does not auto-pick the previous account without a clean chooser.
     */
    suspend fun clearCredentialSession() {
        try {
            CredentialManager.create(appContext)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "clearCredentialState failed (continuing local sign-out)", e)
        }
    }

    private suspend fun requestViaGoogleIdOption(
        credentialManager: CredentialManager,
        activity: Activity,
        serverClientId: String,
    ): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(activity, request)
        return extractIdToken(result.credential)
    }

    private suspend fun requestViaSignInWithGoogle(
        credentialManager: CredentialManager,
        activity: Activity,
        serverClientId: String,
    ): String {
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val result = credentialManager.getCredential(activity, request)
        return extractIdToken(result.credential)
    }

    private fun extractIdToken(credential: androidx.credentials.Credential): String {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                return google.idToken
            } catch (e: GoogleIdTokenParsingException) {
                throw IllegalStateException("Failed to parse Google ID token", e)
            }
        }
        throw IllegalStateException("Unexpected credential type: ${credential::class.java.name}")
    }

    companion object {
        private const val TAG = "GoogleIdTokenProvider"
    }
}
