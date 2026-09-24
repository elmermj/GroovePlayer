package com.aethelsoft.grooveplayer.data.auth

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.aethelsoft.grooveplayer.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Obtains a Google ID token via Credential Manager.
 *
 * Uses the **Web** client ID as serverClientId (required so the ID token `aud`
 * matches backend verify). The Android OAuth client is not passed in code; Google
 * matches package + signing SHA-1 registered in Google Cloud / Firebase.
 *
 * Profile "Sign in with Google" uses the explicit Sign-In-With-Google button flow
 * ([GetSignInWithGoogleOption]), not One Tap. Each request uses a fresh nonce and
 * refuses an immediately-available credential so a prior account is not replayed.
 * Sign-out calls [clearCredentialSession]; the button path clears again first so
 * Credential Manager does not prioritize that account when several exist.
 */
@Singleton
class GoogleIdTokenProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    suspend fun requestIdToken(activity: Activity): String {
        val credentialManager = CredentialManager.create(activity)
        val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        require(serverClientId.isNotEmpty() && serverClientId.contains(".apps.googleusercontent.com")) {
            "GOOGLE_WEB_CLIENT_ID is missing or invalid. Set it in local.properties / CI."
        }

        // Button path only (user is signed out). Clear again so a failed Sign-out
        // clear cannot leave the previous account as the prioritized session.
        clearCredentialSession(activity)

        // Explicit button → SIWG first (Google guidance). Fall back to GoogleIdOption
        // only if SIWG reports no credentials. Both paths disable silent auto-select.
        return try {
            requestViaSignInWithGoogle(credentialManager, activity, serverClientId)
        } catch (e: NoCredentialException) {
            Log.i(TAG, "SIWG returned no credentials; trying GoogleIdOption")
            try {
                requestViaGoogleIdOption(credentialManager, activity, serverClientId)
            } catch (fallback: Exception) {
                throw mapCredentialFailure(fallback)
            }
        } catch (e: GetCredentialCancellationException) {
            // Google sometimes labels SHA / config failures as canceled with "[16] …".
            throw mapCredentialFailure(e)
        } catch (e: GetCredentialException) {
            throw mapCredentialFailure(e)
        } catch (e: Exception) {
            throw mapCredentialFailure(e)
        }
    }

    /**
     * Clears Credential Manager / Google sign-in state so the next Sign-In
     * does not auto-pick the previous account when several accounts exist.
     * Does not revoke the app's Google grants — the chooser can still list them.
     */
    suspend fun clearCredentialSession(context: Context = appContext) {
        try {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "clearCredentialState failed (continuing local sign-out)", e)
        }
    }

    private fun freshNonce(): String {
        val bytes = ByteArray(16)
        nonceRandom.nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING,
        )
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
            .setNonce(freshNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .setPreferImmediatelyAvailableCredentials(false)
            .build()
        val result = credentialManager.getCredential(activity, request)
        return extractIdToken(result.credential)
    }

    private suspend fun requestViaSignInWithGoogle(
        credentialManager: CredentialManager,
        activity: Activity,
        serverClientId: String,
    ): String {
        val option = GetSignInWithGoogleOption.Builder(serverClientId)
            .setNonce(freshNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .setPreferImmediatelyAvailableCredentials(false)
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
        private val nonceRandom = SecureRandom()

        /**
         * True when the user dismissed the chooser (no config / SHA problem).
         * Google often mislabels [16] Account reauth failed as TYPE_USER_CANCELED —
         * those must NOT be treated as a quiet cancel.
         */
        fun isUserCancelledSignIn(error: Throwable): Boolean {
            if (looksLikeReauthOrShaFailure(error)) return false
            var cur: Throwable? = error
            while (cur != null) {
                val name = cur.javaClass.simpleName
                if (name.contains("Cancellation", ignoreCase = true) ||
                    name.contains("Canceled", ignoreCase = true)
                ) {
                    return true
                }
                val msg = cur.message.orEmpty()
                if (msg.contains("user canceled", ignoreCase = true) ||
                    msg.contains("user cancelled", ignoreCase = true)
                ) {
                    return true
                }
                cur = cur.cause
            }
            return false
        }

        fun humanizeSignInFailure(error: Throwable): String {
            if (looksLikeReauthOrShaFailure(error)) {
                return "Google Sign-In could not verify this app build ([16]). " +
                    "Add this APK's signing SHA-1 to the Android OAuth client in " +
                    "Google Cloud / Firebase (package com.aethelsoft.grooveplayer). " +
                    "App Tester / release builds use the upload keystore SHA-1, " +
                    "not the debug one."
            }
            return error.message?.takeIf { it.isNotBlank() } ?: "Sign-in failed"
        }

        private fun looksLikeReauthOrShaFailure(error: Throwable): Boolean {
            var cur: Throwable? = error
            while (cur != null) {
                val msg = cur.message.orEmpty()
                if (msg.contains("Account reauth failed", ignoreCase = true) ||
                    msg.contains("[16]", ignoreCase = false) ||
                    msg.contains("16:", ignoreCase = false)
                ) {
                    return true
                }
                cur = cur.cause
            }
            return false
        }

        private fun mapCredentialFailure(error: Throwable): Throwable {
            if (looksLikeReauthOrShaFailure(error)) {
                return IllegalStateException(humanizeSignInFailure(error), error)
            }
            if (isUserCancelledSignIn(error)) {
                return error
            }
            return error
        }
    }
}
