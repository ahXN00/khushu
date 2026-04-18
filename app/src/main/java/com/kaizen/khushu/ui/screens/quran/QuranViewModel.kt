package com.kaizen.khushu.ui.screens.quran

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kaizen.khushu.data.model.SurahMeta
import com.kaizen.khushu.data.repository.QuranRepository
import com.kaizen.khushu.data.repository.LearnRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    val chapters = mutableStateOf<List<SurahMeta>>(emptyList())
    val currentAyahs = mutableStateOf<List<Pair<Int, String>>>(emptyList())
    val currentTranslation = mutableStateOf<Map<Int, String>>(emptyMap())
    val scriptMap = mutableStateOf<Map<String, String>>(emptyMap())
    val isLoading = mutableStateOf(false)

    fun loadChapters() {
        if (chapters.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val list = QuranRepository.getChapters(getApplication())
            withContext(Dispatchers.Main) {
                chapters.value = list
            }
        }
    }

    fun loadSurah(surahNumber: Int, translationId: String) {
        isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val ayahs = QuranRepository.getAyahs(getApplication(), surahNumber)
            val translation = QuranRepository.getTranslation(getApplication(), surahNumber, translationId)
            withContext(Dispatchers.Main) {
                currentAyahs.value = ayahs
                currentTranslation.value = translation
                isLoading.value = false
            }
        }
    }

    fun loadTranslation(context: android.content.Context, surahNumber: Int, translationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val translation = QuranRepository.getTranslation(context, surahNumber, translationId)
            withContext(Dispatchers.Main) {
                currentTranslation.value = translation
            }
        }
    }

    fun loadScript(context: android.content.Context, script: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (script == "uthmani") {
                withContext(Dispatchers.Main) {
                    scriptMap.value = emptyMap()
                }
            } else {
                val map = LearnRepository.getScriptMap(context, script)
                withContext(Dispatchers.Main) {
                    scriptMap.value = map
                }
            }
        }
    }
}
