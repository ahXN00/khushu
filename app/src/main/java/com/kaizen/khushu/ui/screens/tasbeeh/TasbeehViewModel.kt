package com.kaizen.khushu.ui.screens.tasbeeh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaizen.khushu.data.TasbeehCollection
import com.kaizen.khushu.data.TasbeehDao
import kotlinx.coroutines.launch

class TasbeehViewModel(private val dao: TasbeehDao) : ViewModel() {

    val collections = dao.getAll()

    suspend fun insert(collection: TasbeehCollection) = dao.insert(collection)

    fun delete(collection: TasbeehCollection) {
        viewModelScope.launch { dao.delete(collection) }
    }

    companion object {
        fun factory(dao: TasbeehDao) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TasbeehViewModel(dao) as T
            }
        }
    }
}
