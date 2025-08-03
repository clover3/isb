package com.postech.isb.boardList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BoardListViewModelFactory(
    private val repository: BoardRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BoardListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BoardListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
