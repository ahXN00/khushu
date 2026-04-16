package com.kaizen.khushu.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kaizen.khushu.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AudioDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val filename = inputData.getString("filename") ?: return Result.failure()
        
        val baseUrl = BuildConfig.AUDIO_BASE_URL
        val downloadUrl = baseUrl + filename
        
        val outputDir = File(applicationContext.filesDir, "audio")
        if (!outputDir.exists()) outputDir.mkdirs()
        
        val outputFile = File(outputDir, filename)
        val tempFile = File(outputDir, "$filename.tmp")

        return try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure()
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (inputStream.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) {
                    setProgress(workDataOf("progress" to (total.toFloat() / fileLength)))
                }
                outputStream.write(data, 0, count)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (tempFile.renameTo(outputFile)) {
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure()
        }
    }
}
