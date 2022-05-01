package com.s16.utils

import android.text.TextUtils
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


object Md5Checksum {

    fun calculate(content: String, md5: String): Boolean {
        if (TextUtils.isEmpty(md5) || TextUtils.isEmpty(content)) {
            return false
        }
        val calculatedDigest = calculate(content) ?: return false
        return calculatedDigest.trim { it <= ' ' }.equals(md5.trim { it <= ' ' }, ignoreCase = true)
    }

    @Throws(IOException::class)
    fun calculate(updateFile: File?, md5: String): Boolean {
        if (TextUtils.isEmpty(md5) || updateFile == null) {
            return false
        }
        val calculatedDigest = calculate(updateFile) ?: return false
        return calculatedDigest.trim { it <= ' ' }.equals(md5.trim { it <= ' ' }, ignoreCase = true)
    }

    fun calculate(content: String): String? {
        if (TextUtils.isEmpty(content)) {
            return null
        }

        // This code from the Android documentation for MessageDigest. Nearly verbatim.
        val digester: MessageDigest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            return null // Platform does not support MD5 : can't check, so return null
        }
        val bytes = content.toByteArray()
        digester.update(bytes, 0, bytes.size)
        val digest = digester.digest()
        val s = StringBuilder()
        for (i in digest.indices) {
            s.append(String.format("%1$02x", digest[i]))
        }
        return s.toString()
    }

    @Throws(IOException::class)
    fun calculate(updateFile: File?): String? {
        var md5String: String? = ""
        var stream: InputStream? = null
        try {
            stream = FileInputStream(updateFile)
            md5String = calculate(stream)
        } catch (e: FileNotFoundException) {
            return null
        } finally {
            stream?.close()
        }
        return md5String
    }

    @Throws(IOException::class)
    fun calculate(stream: InputStream): String? {
        // This code from the Android documentation for MessageDigest. Nearly verbatim.
        val digester: MessageDigest = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            return null // Platform does not support MD5 : can't check, so return null
        }

        val bytes = ByteArray(8192)
        var byteCount: Int
        while (stream.read(bytes).also { byteCount = it } > 0) {
            digester.update(bytes, 0, byteCount)
        }

        val digest = digester.digest()
        val s = StringBuilder()
        for (i in digest.indices) {
            s.append(String.format("%1$02x", digest[i]))
        }
        return s.toString()
    }
}