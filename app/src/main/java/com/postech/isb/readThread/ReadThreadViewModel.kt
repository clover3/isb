package com.postech.isb.readThread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.postech.isb.boardList.BoardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// CORRECTED: Removed nextKey and prevKey
data class ThreadContent(
    val key: Int,
    val title: String,
    val author: String,
    val date: String,
    val content: CharSequence,
    val comments: List<Comment> = emptyList()
)

data class ReadThreadUiState(
    val isLoading: Boolean = false,
    val threadContent: ThreadContent? = null,
    val hasNextThread: Boolean = false,
    val hasPreviousThread: Boolean = false,
    val error: String? = null
)

class ReadThreadViewModel(
    private val repository: BoardRepository,
    private val boardName: String,
    private var currentThreadKey: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadThreadUiState())
    val uiState: StateFlow<ReadThreadUiState> = _uiState.asStateFlow()

    init {
        loadThread(currentThreadKey)
    }

    private fun loadThread(threadKey: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val content = repository.getThreadContent(boardName, threadKey)
                currentThreadKey = threadKey // Update the current key

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        threadContent = content,
                        // CORRECTED: Logic to determine next/prev is now in the ViewModel
                        hasNextThread = true, // Assume there is always a next thread, handle error on fetch
                        hasPreviousThread = currentThreadKey > 1
                    )
                }
            } catch (e: Exception) {
                // This is how the original app knew there were no more threads
                if (e is NullPointerException) { // Or whatever exception IsbSession throws
                    _uiState.update { it.copy(isLoading = false, hasNextThread = false, error = "No more threads.") }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load thread.") }
                }
            }
        }
    }

    fun loadNextThread() {
        // CORRECTED: Navigate by incrementing the key
        loadThread(currentThreadKey + 1)
    }

    fun loadPreviousThread() {
        // CORRECTED: Navigate by decrementing the key
        if (currentThreadKey > 1) {
            loadThread(currentThreadKey - 1)
        }
    }

    fun postComment(commentText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.postComment(boardName, currentThreadKey, commentText)
                loadThread(currentThreadKey) // Reload to see the new comment
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to post comment.") }
            }
        }
    }
}

// Factory remains the same
class ReadThreadViewModelFactory(
    private val repository: BoardRepository,
    private val boardName: String,
    private val threadKey: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReadThreadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReadThreadViewModel(repository, boardName, threadKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
