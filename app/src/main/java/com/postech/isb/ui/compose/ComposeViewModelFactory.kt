package com.postech.isb.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.postech.isb.boardList.BoardRepository

class ComposeViewModelFactory(
    private val repository: BoardRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComposeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ComposeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
