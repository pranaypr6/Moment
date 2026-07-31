package com.pranayburra.moment.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

object GoogleAuthHelper {

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    // TEMPORARY DIAGNOSTIC: surfaces the real failure reason as an on-screen Toast.
    // Release builds strip Log.* calls (see proguard-rules.pro) so logcat has been useless
    // for this bug - this makes the failure visible without needing adb at all. Remove once
    // the Google Sign-In "Get Started" silent-failure bug (FAST_FOLLOW.md) is actually fixed,
    // or replace with a proper permanent error-state UI at that point.
    private fun toast(context: Context, msg: String) {
        Log.e("GoogleAuth", msg)
        Toast.makeText(context.applicationContext, "Sign-in error: $msg", Toast.LENGTH_LONG).show()
    }

    suspend fun signInWithGoogle(context: Context, webClientId: String): String? {
        val activity = context.findActivity()
        if (activity == null) {
            toast(context, "no Activity found from context")
            return null
        }
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
                googleIdTokenCredential.idToken
            } else {
                toast(context, "unexpected credential type: ${credential.type}")
                null
            }
        } catch (e: GetCredentialException) {
            toast(context, "${e.type}: ${e.errorMessage ?: e.message}")
            null
        } catch (e: Exception) {
            // Broad catch as a safety net: if something other than GetCredentialException is
            // thrown (e.g. a missing provider / Play Services resolution issue on-device),
            // the previous code let it propagate uncaught out of this suspend function,
            // which would crash the coroutine silently with no Toast and no Crashlytics
            // report if the exception happened to be swallowed upstream. Surface it instead.
            toast(context, "${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
}
