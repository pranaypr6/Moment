package com.pranayburra.moment.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

// TEMPORARY DIAGNOSTIC result type: a Toast was tried first but disappears before the full
// message can be read (LENGTH_LONG is ~3.5s and long text wraps/scrolls). Returning the error
// as data instead lets the caller show it as persistent, on-screen (and copyable) text. Release
// builds strip Log.* calls (see proguard-rules.pro), so this is otherwise the only way to see
// the real failure reason without adb. Collapse back to a plain String? once the Google
// Sign-In "Get Started" silent-failure bug (FAST_FOLLOW.md) is actually fixed.
sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Failure(val message: String) : GoogleSignInResult()
}

object GoogleAuthHelper {

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String): GoogleSignInResult {
        val activity = context.findActivity()
            ?: return GoogleSignInResult.Failure("no Activity found from context")
        val credentialManager = CredentialManager.create(context)

        // Generate a random nonce to prevent replay attacks
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.joinToString("") { "%02x".format(it) }

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = activity,
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleIdTokenCredential.idToken)
            } else {
                GoogleSignInResult.Failure("unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuth", "Sign-in failed", e)
            GoogleSignInResult.Failure("${e.type}: ${e.errorMessage ?: e.message}")
        } catch (e: Exception) {
            // Broad catch as a safety net: if something other than GetCredentialException is
            // thrown (e.g. a missing provider / Play Services resolution issue on-device),
            // the previous code let it propagate uncaught out of this suspend function.
            Log.e("GoogleAuth", "Sign-in failed (unexpected type)", e)
            GoogleSignInResult.Failure("${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
