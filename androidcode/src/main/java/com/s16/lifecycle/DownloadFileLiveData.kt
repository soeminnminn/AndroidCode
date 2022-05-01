package com.s16.lifecycle

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import androidx.lifecycle.LiveData
import java.io.File

class DownloadContext(
    context: Context,
    val uri: Uri,
    val file: File,
    val description: String = file.name,
    val title: String = file.name
) : ContextWrapper(context)

data class DownloadQueryResult(
    val id: Long,
    val action: String = "",
    val title: String = "",
    val description: String = "",
    val uri: Uri = Uri.EMPTY,
    val mediaType: String = "",
    val totalSize: Long = 0,
    val localUri: Uri = Uri.EMPTY,
    val status: Int = 0,
    val reason: Int = 0,
    val downloadedSoFar: Long = 0,
    val lastModified: Long = 0
) {
    val isEmpty: Boolean = id == -1L
}

class DownloadFileLiveData(private val context: DownloadContext) : LiveData<DownloadQueryResult>() {

    private var downloadReference: Long = -1L
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == downloadReference) {
                when(intent.action) {
                    DownloadManager.ACTION_NOTIFICATION_CLICKED,
                    DownloadManager.ACTION_VIEW_DOWNLOADS,
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                        postValue(queryResult(downloadId, intent.action!!))
                    }
                }
            }
        }
    }

    override fun onActive() {
        super.onActive()
        download()
    }

    override fun onInactive() {
        context.unregisterReceiver(broadcastReceiver)
        super.onInactive()
    }

    private fun queryResult(refId: Long, action: String) : DownloadQueryResult {
        val query = DownloadManager.Query()
        query.setFilterById(refId)
        val data = downloadManager.query(query)
        if (data.moveToNext()) {
            var colIdx = data.getColumnIndex(DownloadManager.COLUMN_ID)
            val id = if (colIdx != -1) { data.getLong(colIdx) } else 0L

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val title = if (colIdx != -1) { data.getString(colIdx) } else ""

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION)
            val description = if (colIdx != -1) { data.getString(colIdx) } else ""

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_URI)
            val uri = if (colIdx != -1) { Uri.parse(data.getString(colIdx)) } else Uri.EMPTY

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
            val mediaType = if (colIdx != -1) { data.getString(colIdx) } else ""

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val totalSize = if (colIdx != -1) { data.getLong(colIdx) } else 0L

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val localUri = if (colIdx != -1) { Uri.parse(data.getString(colIdx)) } else Uri.EMPTY

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = if (colIdx != -1) { data.getInt(colIdx) } else 0

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_REASON)
            val reason = if (colIdx != -1) { data.getInt(colIdx) } else 0

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val downloadedSoFar = if (colIdx != -1) { data.getLong(colIdx) } else 0L

            colIdx = data.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)
            val lastModified = if (colIdx != -1) { data.getLong(colIdx) } else 0L

            return DownloadQueryResult(
                id, action, title, description, uri,
                mediaType, totalSize, localUri, status,
                reason, downloadedSoFar, lastModified)
        }
        return DownloadQueryResult(-1)
    }

    private fun download() {
        if (downloadReference >= 0) {
            val result = queryResult(downloadReference, "")
            if (result.isEmpty || result.status == DownloadManager.STATUS_FAILED) {
                downloadReference = -1L
            } else {
                postValue(result)
            }
        }

        if (downloadReference == -1L) {
            val request = DownloadManager.Request(context.uri).apply {
                setDescription(context.description)
                setTitle(context.title)
            }
            val downloadFile = context.file
            if (downloadFile.exists()) {
                downloadFile.delete()
            }

            request.setDestinationUri(Uri.fromFile(downloadFile))
            downloadReference = downloadManager.enqueue(request)
        }

        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }
}