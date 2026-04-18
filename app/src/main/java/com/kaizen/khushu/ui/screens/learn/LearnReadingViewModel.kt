package com.kaizen.khushu.ui.screens.learn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kaizen.khushu.data.model.ContentBlock
import com.kaizen.khushu.data.repository.LearnRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LearnReadingViewModel(application: Application) : AndroidViewModel(application) {

    private val _blocks = MutableStateFlow<List<ContentBlock>?>(null)

    /**
     * null  = not yet loaded
     * empty = topic has no JSON file (screen falls back to legacy rendering)
     * non-empty = blocks ready to render
     */
    val blocks: StateFlow<List<ContentBlock>?> = _blocks

    fun loadBlocks(topicId: String) {
        if (_blocks.value != null) return // already loaded for this instance
        viewModelScope.launch(Dispatchers.IO) {
            _blocks.value = LearnRepository.getBlocks(topicId, getApplication())
        }
    }
}
