package com.kaizen.khushu.ui.screens.learn

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.kaizen.khushu.data.model.LearnTopic
import com.kaizen.khushu.data.repository.AudioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearnAudioViewModel(private val audioRepo: AudioRepository, private val context: Context) : ViewModel() {

    sealed interface AudioState {
        data object Idle : AudioState
        data object Loading : AudioState
        data object Playing : AudioState
        data object Paused : AudioState
        data class Error(val message: String) : AudioState
    }

    private val _audioState = MutableStateFlow<AudioState>(AudioState.Idle)
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(-1f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var currentTopicId: String? = null
    private var downloadObserverJob: Job? = null

    fun play(topic: LearnTopic) {
        val filename = topic.audioFilename ?: return
        
        if (currentTopicId == topic.id && mediaPlayer != null) {
            mediaPlayer?.start()
            _audioState.value = AudioState.Playing
            return
        }

        stop()
        currentTopicId = topic.id
        _audioState.value = AudioState.Loading

        if (audioRepo.isAssetAvailable(filename)) {
            playFromAsset(filename)
        } else {
            val localFile = audioRepo.resolveLocal(filename)
            if (localFile != null) {
                playFromFile(localFile.absolutePath)
            } else {
                startDownload(filename)
            }
        }
    }

    private fun playFromAsset(filename: String) {
        try {
            val afd = context.assets.openFd("audio/$filename")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
                setOnCompletionListener { 
                    _audioState.value = AudioState.Idle
                    currentTopicId = null
                }
                start()
            }
            _audioState.value = AudioState.Playing
            afd.close()
        } catch (e: Exception) {
            _audioState.value = AudioState.Error(e.message ?: "Failed to play asset")
        }
    }

    private fun playFromFile(path: String) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener { 
                    _audioState.value = AudioState.Idle
                    currentTopicId = null
                }
                start()
            }
            _audioState.value = AudioState.Playing
        } catch (e: Exception) {
            _audioState.value = AudioState.Error(e.message ?: "Failed to play file")
        }
    }

    private fun startDownload(filename: String) {
        audioRepo.enqueueDownload(filename)
        downloadObserverJob?.cancel()
        downloadObserverJob = viewModelScope.launch {
            audioRepo.downloadState(filename).asFlow().collect { workInfos ->
                val info = workInfos.firstOrNull() ?: return@collect
                if (info.state.isFinished) {
                    if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                        val localFile = audioRepo.resolveLocal(filename)
                        if (localFile != null) {
                            playFromFile(localFile.absolutePath)
                        } else {
                            _audioState.value = AudioState.Error("Downloaded file not found")
                        }
                    } else {
                        _audioState.value = AudioState.Error("Download failed")
                    }
                    _downloadProgress.value = -1f
                    downloadObserverJob?.cancel()
                } else {
                    val progress = info.progress.getFloat("progress", 0f)
                    _downloadProgress.value = progress
                }
            }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _audioState.value = AudioState.Paused
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentTopicId = null
        _audioState.value = AudioState.Idle
        _downloadProgress.value = -1f
        downloadObserverJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }

    companion object {
        fun factory(audioRepo: AudioRepository, context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LearnAudioViewModel(audioRepo, context) as T
            }
        }
    }
}
