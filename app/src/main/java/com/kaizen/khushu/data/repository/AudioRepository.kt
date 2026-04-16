package com.kaizen.khushu.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.kaizen.khushu.data.worker.AudioDownloadWorker
import java.io.File

class AudioRepository(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun resolveLocal(filename: String): File? {
        // 1. Check context.filesDir/audio/
        val internalFile = File(File(context.filesDir, "audio"), filename)
        if (internalFile.exists()) return internalFile

        // Assets are handled by MediaPlayer.setDataSource(AssetFileDescriptor) 
        // but for simplicity in resolveLocal returning File, we might need a different approach 
        // for assets. Or we can just check if asset exists.
        return null
    }

    fun isAssetAvailable(filename: String): Boolean {
        return try {
            context.assets.open("audio/$filename").close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun enqueueDownload(filename: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("filename", filename)
            .build()

        val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("audio_download_$filename")
            .build()

        workManager.enqueue(request)
    }

    fun downloadState(filename: String): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData("audio_download_$filename")
    }
}
