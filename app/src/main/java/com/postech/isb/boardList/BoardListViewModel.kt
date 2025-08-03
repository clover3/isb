package com.postech.isb.boardList

import androidx.lifecycle.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// An enum to represent the different filter states
enum class BoardFilter {
    ALL,
    FAVORITES_ONLY
    // REMOVED: NEW_POSTS_ONLY was incorrect
}

data class BoardListUiState(
    val isLoading: Boolean = false,
    val boards: List<Board> = emptyList(),
    val error: String? = null,
    val activeFilter: BoardFilter = BoardFilter.ALL
)

class BoardListViewModel(private val repository: BoardRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(BoardFilter.ALL)

    val uiState: StateFlow<BoardListUiState> = combine(
        repository.getBoards(),
        _searchQuery,
        _activeFilter
    ) { boards, query, filter ->
        val filteredBoards = boards.filter { board ->
            val matchesSearch = board.name.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                BoardFilter.ALL -> true
                BoardFilter.FAVORITES_ONLY -> board.favorite
                // REMOVED: The case for NEW_POSTS_ONLY
            }
            matchesSearch && matchesFilter
        }

        val sortedBoards = filteredBoards.sortedWith(
            compareByDescending<Board> { it.myBoard }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )

        BoardListUiState(boards = sortedBoards, activeFilter = filter)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BoardListUiState(isLoading = true)
        )

    init {
        viewModelScope.launch {
            if (repository.getBoards().first().isEmpty()) {
                onRefresh()
            }
        }
    }

    fun onRefresh() { /* ... same as before ... */ }
    fun onFavoriteClicked(board: Board) { /* ... same as before ... */ }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: BoardFilter) {

        _activeFilter.value = if (_activeFilter.value == filter) BoardFilter.ALL else filter
    }
    fun checkForNewPosts() {
        viewModelScope.launch {
            repository.checkBoardsForNewPosts(onlyFavorites = false)
        }
    }

}
