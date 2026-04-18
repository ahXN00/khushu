package com.kaizen.khushu.ui.screens.learn

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.kaizen.khushu.data.model.LearnSection
import com.kaizen.khushu.data.repository.LearnRepository

class LearnViewModel(application: Application) : AndroidViewModel(application) {
    val sections = mutableStateOf<List<LearnSection>>(emptyList())

    init {
        sections.value = LearnRepository.getSections(application)
    }
}
