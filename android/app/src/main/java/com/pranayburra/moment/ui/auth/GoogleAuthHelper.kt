package com.pranayburra.moment.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

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

    fun getLegacySignInIntent(context: Context, webClientId: String): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        client.signOut()
        return client.signInIntent
    }

    fun getAppSignatureSha1(context: Context): String {
        return try {
            val pm = context.packageManager
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val sig = signatures?.firstOrNull() ?: return "Unknown"
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(sig.toByteArray())
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun parseLegacySignInResult(context: Context, webClientId: String, data: Intent?): GoogleSignInResult {
        if (data == null) {
            return GoogleSignInResult.Failure("Sign-in cancelled.")
        }
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrBlank()) {
                GoogleSignInResult.Success(idToken)
            } else {
                GoogleSignInResult.Failure("Google Sign-In failed. Please try again.")
            }
        } catch (e: ApiException) {
            val appSha1 = getAppSignatureSha1(context)
            Log.e("GoogleAuth", "Legacy Sign-in failed (code ${e.statusCode}): ${e.message} [App SHA1: $appSha1, WebClientID: $webClientId]", e)
            val userFacingMessage = when (e.statusCode) {
                12501 -> "Sign-in cancelled."
                7 -> "Network error. Please check your connection and try again."
                else -> "Google Sign-In failed. Please try again."
            }
            GoogleSignInResult.Failure(userFacingMessage)
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Legacy Sign-in unexpected error", e)
            GoogleSignInResult.Failure("Google Sign-In failed. Please try again.")
        }
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

        val signInWithGoogleOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(hashedNonce)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = activity,
            )

            val credential = result.credential
            if (credential is CustomCredential) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleSignInResult.Success(googleIdTokenCredential.idToken)
                } catch (e: Exception) {
                    GoogleSignInResult.Failure("Failed to parse Google ID token: ${e.message}")
                }
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
