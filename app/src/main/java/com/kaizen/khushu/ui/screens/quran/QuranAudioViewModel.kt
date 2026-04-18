package com.kaizen.khushu.ui.screens.quran

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.work.*
import com.kaizen.khushu.data.model.AyahBlock
import com.kaizen.khushu.data.model.ContentBlock
import com.kaizen.khushu.data.repository.QuranAudioRepository
import com.kaizen.khushu.data.worker.QuranAudioWorker
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuranAudioViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface AudioState {
        data object Idle : AudioState
        data object Loading : AudioState
        data object Playing : AudioState
        data object Paused : AudioState
        data class Error(val msg: String) : AudioState
    }

    val audioState = mutableStateOf<AudioState>(AudioState.Idle)
    val currentSurah = mutableStateOf<Int?>(null)
    val playingAyahIndex = mutableStateOf<Int?>(null)
    
    private var mediaPlayer: MediaPlayer? = null
    private var nextMediaPlayer: MediaPlayer? = null
    
    private val workManager = WorkManager.getInstance(application)
    
    private var currentBlocks: List<ContentBlock>? = null
    private var isSequenceMode: Boolean = false
    private var currentReciterId: String? = null

    private fun getAudioDir(reciterId: String): File {
        val dir = File(getApplication<Application>().filesDir, "audio/$reciterId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isReciterDownloaded(reciterId: String): Boolean {
        val dir = getAudioDir(reciterId)
        val files = dir.listFiles { _, name -> name.endsWith(".mp3") }
        return files?.size == 114
    }

    fun getReciterDownloadProgress(reciterId: String): Flow<Pair<Float, Int>?> {
        val tag = "reciter_download_$reciterId"
        return workManager.getWorkInfosByTagLiveData(tag).asFlow().map { infos ->
            val info = infos.firstOrNull()
            if (info?.state == WorkInfo.State.RUNNING) {
                val progress = info.progress.getFloat("progress", 0f)
                val count = info.progress.getInt("count", 0)
                progress to count
            } else if (info?.state == WorkInfo.State.SUCCEEDED) {
                1f to 114
            } else {
                null
            }
        }
    }

    fun playSurah(surahNumber: Int, url: String, reciterId: String) {
        stop() 
        currentReciterId = reciterId
        isSequenceMode = false
        playingAyahIndex.value = null
        
        val localSurahFile = File(getAudioDir(reciterId), "$surahNumber.mp3")
        val dataSource = if (localSurahFile.exists()) localSurahFile.absolutePath else url
        
        playInternal(surahNumber, dataSource, reciterId)
    }

    private fun getAyahUrl(surahNumber: Int, ayahIndex: Int, blocks: List<ContentBlock>, reciterId: String): String? {
        val block = blocks.getOrNull(ayahIndex) as? AyahBlock ?: return null
        val paddedSurah = surahNumber.toString().padStart(3, '0')
        val paddedAyah = block.ayah.toString().padStart(3, '0')
        val fileName = "$paddedSurah$paddedAyah.mp3"
        
        val relativePath = when(reciterId) {
            "mishari"    -> "Alafasy/mp3/$fileName"
            "abdulbaset" -> "AbdulBaset/Mujawwad/mp3/$fileName"
            "sudais"     -> "Sudais/mp3/$fileName"
            "husary"     -> "Husary_64kbps/$fileName"
            "minshawi"   -> "Minshawi/Murattal/mp3/$fileName"
            else         -> "Alafasy/mp3/$fileName"
        }
        
        return if (reciterId == "husary") {
            "https://mirrors.quranicaudio.com/everyayah/$relativePath"
        } else {
            "https://audio.qurancdn.com/$relativePath"
        }
    }

    fun playAyah(
        surahNumber: Int,
        ayahIndex: Int,
        blocks: List<ContentBlock>,
        reciterId: String,
        sequence: Boolean
    ) {
        stop() 
        currentBlocks = blocks
        isSequenceMode = sequence
        playingAyahIndex.value = ayahIndex
        currentReciterId = reciterId
        
        val url = getAyahUrl(surahNumber, ayahIndex, blocks, reciterId) ?: return
        playInternal(surahNumber, url, reciterId)
    }

    private fun playInternal(surahNumber: Int, dataSource: String, reciterId: String) {
        currentSurah.value = surahNumber
        currentReciterId = reciterId
        audioState.value = AudioState.Loading

        try {
            mediaPlayer = createMediaPlayer(dataSource)
            mediaPlayer?.setOnPreparedListener {
                it.start()
                audioState.value = AudioState.Playing
                if (isSequenceMode) {
                    prepareNextAyahGapless()
                }
            }
            mediaPlayer?.setOnCompletionListener {
                handleAyahCompletion()
            }
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            audioState.value = AudioState.Error(e.message ?: "Failed to play audio")
        }
    }

    private fun handleAyahCompletion() {
        if (isSequenceMode && nextMediaPlayer != null) {
            // Transition to the pre-buffered player
            mediaPlayer?.release()
            mediaPlayer = nextMediaPlayer
            nextMediaPlayer = null
            
            // Advance the highlighting index
            val currentIdx = playingAyahIndex.value ?: -1
            val nextIdx = findNextAyahIndex(currentIdx + 1)
            playingAyahIndex.value = nextIdx
            
            if (nextIdx != null) {
                audioState.value = AudioState.Playing
                // Crucial: Set the completion listener for the new primary player
                mediaPlayer?.setOnCompletionListener { handleAyahCompletion() }
                prepareNextAyahGapless()
            } else {
                stop()
            }
        } else {
            stop()
        }
    }

    private fun createMediaPlayer(dataSource: String): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(dataSource)
            setOnErrorListener { mp, what, extra ->
                audioState.value = AudioState.Error("Playback error: $what")
                stop()
                true
            }
        }
    }

    private fun prepareNextAyahGapless() {
        val surahNum = currentSurah.value ?: return
        val reciterId = currentReciterId ?: return
        val blocks = currentBlocks ?: return
        val currentIdx = playingAyahIndex.value ?: return
        
        val nextIdx = findNextAyahIndex(currentIdx + 1) ?: return
        val nextUrl = getAyahUrl(surahNum, nextIdx, blocks, reciterId) ?: return
        
        try {
            val next = createMediaPlayer(nextUrl)
            next.setOnPreparedListener { preparedNext ->
                mediaPlayer?.setNextMediaPlayer(preparedNext)
                nextMediaPlayer = preparedNext
            }
            next.prepareAsync()
        } catch (e: Exception) {
            nextMediaPlayer = null
        }
    }

    private fun findNextAyahIndex(startIndex: Int): Int? {
        val blocks = currentBlocks ?: return null
        if (startIndex < 0 || startIndex >= blocks.size) return null
        for (i in startIndex until blocks.size) {
            if (blocks[i] is AyahBlock) return i
        }
        return null
    }

    fun downloadReciter(reciterId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val inputData = Data.Builder().putString("reciterId", reciterId).build()
        val request = OneTimeWorkRequestBuilder<QuranAudioWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("reciter_download_$reciterId")
            .build()
        workManager.enqueue(request)
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                audioState.value = AudioState.Paused
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            audioState.value = AudioState.Playing
        }
    }

    fun stop() {
        mediaPlayer?.let {
            try { if (it.isPlaying) it.stop() } catch (e: Exception) {}
            it.release()
        }
        mediaPlayer = null
        
        nextMediaPlayer?.let {
            try { it.release() } catch (e: Exception) {}
        }
        nextMediaPlayer = null
        
        audioState.value = AudioState.Idle
        playingAyahIndex.value = null
        currentSurah.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
