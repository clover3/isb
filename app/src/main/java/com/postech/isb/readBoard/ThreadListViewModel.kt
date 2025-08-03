package com.postech.isb.readBoard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.postech.isb.boardList.BoardRepository
import com.postech.isb.boardList.ThreadAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.postech.isb.boardList.ThreadListPage


data class ThreadListUiState(
    val isLoading: Boolean = false,
    val threads: List<IsbThread> = emptyList(),
    val boardTitle: String = "",
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false,
    val error: String? = null,
    val singleEventMessage: String? = null,
    val readersInfo: String? = null
)


class ThreadListViewModel(
    private val repository: BoardRepository,
    private val boardName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThreadListUiState())
    val uiState: StateFlow<ThreadListUiState> = _uiState.asStateFlow()

    // State variables migrated from ReadBoards.java
    private var currentFirstIndex = 1
    private var currentLastIndex = 0
    private var totalThreads = 0
    private val threadsPerPage = 76 // From original code (NUM_ROWS - 4)

    init {
        // Load the last page initially, as per original onResume logic
        loadLastPage()
    }

    private fun updateStateWithPageData(pageData: ThreadListPage) {
        currentFirstIndex = pageData.firstIndex
        currentLastIndex = pageData.lastIndex
        if (pageData.totalThreads > totalThreads) {
            totalThreads = pageData.totalThreads
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                threads = pageData.threads,
                hasPreviousPage = currentFirstIndex > 1,
                hasNextPage = currentLastIndex < totalThreads
            )
        }
    }

    private fun loadPageByIndex(firstIndex: Int, lastIndex: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val pageData = repository.getThreadsByIndexRange(boardName, firstIndex, lastIndex)
                updateStateWithPageData(pageData)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load threads.") }
            }
        }
    }

    private fun loadLastPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, boardTitle = boardName) }
            try {
                val pageData = repository.getLastPageThreads(boardName)
                updateStateWithPageData(pageData)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load threads.") }
            }
        }
    }

    fun loadNextPage() {
        if (uiState.value.hasNextPage) {
            val nextFirstIndex = currentLastIndex + 1
            val nextLastIndex = nextFirstIndex + threadsPerPage - 1
            loadPageByIndex(nextFirstIndex, nextLastIndex)
        }
    }

    fun loadPreviousPage() {
        if (uiState.value.hasPreviousPage) {
            val prevFirstIndex = (currentFirstIndex - threadsPerPage).coerceAtLeast(1)
            val prevLastIndex = currentFirstIndex - 1
            loadPageByIndex(prevFirstIndex, prevLastIndex)
        }
    }

    // This replicates the complex onResume logic
    fun handleOnResume(lastReadIndex: Int) {
        if (lastReadIndex == -1) { // lastReadNothing
            loadLastPage()
            return
        }

        // Check if the last read thread is on the current page
        val isOnCurrentPage = lastReadIndex >= currentFirstIndex && lastReadIndex <= currentLastIndex
        if (!isOnCurrentPage) {
            // It's not on the current page, so we need to find the correct page to load
            val pageNumber = (lastReadIndex / threadsPerPage)
            val firstIndex = (pageNumber * threadsPerPage) + 1
            val lastIndex = firstIndex + threadsPerPage - 1
            loadPageByIndex(firstIndex, lastIndex)
        }
    }


    // ADDED: Function to handle writing a new thread
    fun writeNewThread(title: String, content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = repository.writeToBoard(boardName, title, content)
                if (success) {
                    // After a successful post, reload the last page to see the new thread
                    loadLastPage()
                    _uiState.update { it.copy(singleEventMessage = "Write success") }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to post.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Connection error.") }
            }
        }
    }

    fun modifyThread(threadKey: Int, action: ThreadAction) {
        viewModelScope.launch {
            try {
                val success = repository.modifyThread(boardName, threadKey, action)
                if (success) {
                    loadPageByIndex(currentFirstIndex, currentLastIndex) // Reload current page
                } else {
                    _uiState.update { it.copy(singleEventMessage = "Action failed.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(singleEventMessage = "Connection error.") }
            }
        }
    }

    fun viewReaders(threadKey: Int) {
        viewModelScope.launch {
            val readers = repository.getThreadReaders(boardName, threadKey)
            _uiState.update { it.copy(readersInfo = readers ?: "No permission to view readers.") }
        }
    }

    fun clearSingleEventMessage() {
        _uiState.update { it.copy(singleEventMessage = null) }
    }

    fun clearReadersInfo() {
        _uiState.update { it.copy(readersInfo = null) }
    }
}


// We need a factory to pass the boardName from the Activity to the ViewModel
class ThreadListViewModelFactory(
    private val repository: BoardRepository,
    private val boardName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThreadListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThreadListViewModel(repository, boardName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
