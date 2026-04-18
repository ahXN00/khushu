package com.kaizen.khushu.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kaizen.khushu.data.repository.QuranAudioRepository
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class QuranAudioWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reciterId = inputData.getString("reciterId") ?: return Result.failure()
        val manifest = QuranAudioRepository.getManifest(applicationContext, reciterId)
        if (manifest.isEmpty()) return Result.failure()

        val outputDir = File(applicationContext.filesDir, "audio/$reciterId")
        if (!outputDir.exists()) outputDir.mkdirs()

        var downloadedCount = 0
        val totalCount = 114

        // Download all 114 surahs
        for (surahNum in 1..totalCount) {
            val downloadUrl = manifest[surahNum] ?: continue
            val filename = "$surahNum.mp3"
            val outputFile = File(outputDir, filename)

            if (outputFile.exists()) {
                downloadedCount++
                continue
            }

            val tempFile = File(outputDir, "$filename.tmp")
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(tempFile)
                    val data = ByteArray(8192)
                    var count: Int
                    while (inputStream.read(data).also { count = it } != -1) {
                        outputStream.write(data, 0, count)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    tempFile.renameTo(outputFile)
                }
            } catch (e: Exception) {
                if (tempFile.exists()) tempFile.delete()
                // Continue to next surah even if one fails
            }

            downloadedCount++
            setProgress(workDataOf(
                "progress" to (downloadedCount.toFloat() / totalCount),
                "count" to downloadedCount
            ))
        }

        return Result.success()
    }
}
