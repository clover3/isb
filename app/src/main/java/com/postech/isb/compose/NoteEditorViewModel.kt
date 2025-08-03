package com.postech.isb.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.postech.isb.boardList.BoardRepository
import com.postech.isb.readThread.ThreadContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val isLoading: Boolean = false,
    val initialContent: ThreadContent? = null,
    val error: String? = null,
    val isSaveComplete: Boolean = false
)

class NoteEditorViewModel(private val repository: BoardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    fun loadInitialContent(boardName: String, threadKey: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val content = repository.getThreadContent(boardName, threadKey)
                _uiState.update { it.copy(isLoading = false, initialContent = content) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load content.") }
            }
        }
    }

    fun saveChanges(boardName: String, threadKey: Int, title: String, content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = repository.editPost(boardName, threadKey, title, content)
                if (success) {
                    _uiState.update { it.copy(isLoading = false, isSaveComplete = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to save changes.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Connection error.") }
            }
        }
    }
}

class NoteEditorViewModelFactory(
    private val repository: BoardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteEditorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
