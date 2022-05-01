package com.s16.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import java.util.Arrays

/**
 * @author Trident_Surya Devi
 */
class AppSignatureHelper(context: Context) : ContextWrapper(context) {

    /**
     * Get all the app signatures for the current package
     */
    // Get all package signatures for the current package
    // For each signature create a compatible hash
    @Suppress("DEPRECATION")
    val appSignatures: List<String>
        @SuppressLint("PackageManagerGetSignatures")
        get() {
            val appCodes = ArrayList<String>()

            try {
                val packageName = packageName
                val packageManager = packageManager
                val signatures = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures
                for (signature in signatures) {
                    val hash = hash(packageName, signature.toCharsString())
                    if (hash != null) {
                        appCodes.add(String.format("%s", hash))
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
            }

            return appCodes
        }

    @SuppressLint("ObsoleteSdkInt")
    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        try {
            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
            } else {
                messageDigest.update(appInfo.toByteArray(Charset.forName("UTF-8")))
            }
            var hashSignature = messageDigest.digest()

            // truncated into NUM_HASHED_BYTES
            hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES)
            // encode into Base64
            var base64Hash =
                Base64.encodeToString(hashSignature, Base64.NO_PADDING or Base64.NO_WRAP)
            base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR)

            return base64Hash
        } catch (e: NoSuchAlgorithmException) {
        }

        return null
    }

    companion object {
        @Volatile
        private var instance: AppSignatureHelper? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance
            ?: synchronized(LOCK) {
                instance
                    ?: AppSignatureHelper(context).also { instance = it }
            }

        private val HASH_TYPE = "SHA-256"
        private val NUM_HASHED_BYTES = 9
        private val NUM_BASE64_CHAR = 11
    }
}