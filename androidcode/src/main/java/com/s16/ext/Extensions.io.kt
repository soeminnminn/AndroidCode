package com.s16.ext

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


@Throws(IOException::class)
fun File.copyTo(outFile: File) {
    val fis = FileInputStream(this)
    val output = FileOutputStream(outFile)

    val buffer = ByteArray(1024)
    var length: Int = fis.read(buffer)
    while (length > 0) {
        output.write(buffer, 0, length)
        length = fis.read(buffer)
    }
    output.flush()
    output.close()
    fis.close()
}

val Context.externalDataDir : File?
    get() {
    val dataFolder = "/Android/data/${packageName}/"
    return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
        val path = "${Environment.getExternalStorageDirectory().absolutePath}${dataFolder}"
        val folder = File(path)
        if (!folder.exists() && !folder.mkdirs()) {
            null
        } else {
            folder
        }
    } else {
        null
    }
}

val Context.downloadsDir: File?
    get() {
    return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
        val folder = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (folder != null && folder.exists()) {
            folder
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }

    } else {
        null
    }
}

@Suppress("DEPRECATION")
val Context.isNetworkAvailable: Boolean
    @SuppressLint("MissingPermission")
    get() {
        var isMobile = false
        var isWifi = false

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val infoAvailableNetworks = cm.allNetworkInfo
        for (network in infoAvailableNetworks) {
            if (network.type == ConnectivityManager.TYPE_WIFI) {
                if (network.isConnected && network.isAvailable) isWifi = true
            }
            if (network.type == ConnectivityManager.TYPE_MOBILE) {
                if (network.isConnected && network.isAvailable) isMobile = true
            }
        }

        return isMobile || isWifi
    }