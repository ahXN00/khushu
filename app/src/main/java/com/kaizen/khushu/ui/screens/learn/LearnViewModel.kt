package com.kaizen.khushu.ui.screens.learn

import androidx.lifecycle.ViewModel
import com.kaizen.khushu.data.model.LearnSection
import com.kaizen.khushu.data.repository.LearnRepository

class LearnViewModel : ViewModel() {
    val sections: List<LearnSection> = LearnRepository.getSections()
}