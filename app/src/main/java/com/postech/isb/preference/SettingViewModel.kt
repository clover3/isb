package com.postech.isb.preference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.postech.isb.boardList.BoardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

// Represents the possible states of the backup/restore operations
sealed class SettingsActionState {
    object Idle : SettingsActionState()
    object Loading : SettingsActionState()
    data class Success(val message: String) : SettingsActionState()
    data class Error(val message: String) : SettingsActionState()
}

class SettingsViewModel(
    private val repository: BoardRepository,
    private val settingsManager: SettingsManager // Added dependency
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsActionState>(SettingsActionState.Idle)
    val uiState: StateFlow<SettingsActionState> = _uiState.asStateFlow()

    private val favoriteListThreadTitle = "** Favorite board list **"
    private val favoriteListThreadFooter = "- END -"

    fun backupFavorites() {
        viewModelScope.launch {
            _uiState.value = SettingsActionState.Loading
            try {
                // Get all favorite boards from the database
                val favoriteBoards = repository.getBoards().first().filter { it.favorite }
                if (favoriteBoards.isEmpty()) {
                    _uiState.value = SettingsActionState.Error("No favorite boards to back up.")
                    return@launch
                }

                // Construct the content of the diary post
                var content = "\n# Date: ${DateFormat.getDateTimeInstance().format(Date())}\n"
                content += "# Do not change the title of this thread.\n"
                favoriteBoards.forEach { board ->
                    content += "${board.name}\n"
                }
                content += favoriteListThreadFooter

                // Write the post to the diary
                val success = repository.writeToBoard("diary", favoriteListThreadTitle, content)
                if (success) {
                    _uiState.value =
                        SettingsActionState.Success("Favorites backed up successfully!")
                } else {
                    _uiState.value = SettingsActionState.Error("Failed to write to diary.")
                }

            } catch (e: Exception) {
                _uiState.value = SettingsActionState.Error("Backup failed: ${e.message}")
            }
        }
    }

    fun restoreFavorites() {
        viewModelScope.launch {
            _uiState.value = SettingsActionState.Loading
            try {
                val backupThread = repository.findLatestDiaryPostWithTitle(favoriteListThreadTitle)
                if (backupThread == null) {
                    _uiState.value = SettingsActionState.Error("No backup found in diary.")
                    return@launch
                }

                val allBoards = repository.getBoards().first()
                val boardNames = allBoards.map { it.name }
                var unrestoredBoards = ""

                backupThread.content.toString().lines().forEach { line ->
                    val trimmedLine = line.trim()
                    if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#") && trimmedLine != favoriteListThreadFooter) {
                        if (trimmedLine.startsWith("*")) {
                            val myBoardName = trimmedLine.substring(1)
                            settingsManager.setMyBoard(myBoardName)
                        } else {
                            if (boardNames.contains(trimmedLine)) {
                                repository.setFavorite(trimmedLine, true)
                            } else {
                                unrestoredBoards += "$trimmedLine\n"
                            }
                        }
                    }
                }

                if (unrestoredBoards.isNotEmpty()) {
                    _uiState.value = SettingsActionState.Success("Restore complete. Some boards could not be found.")
                } else {
                    _uiState.value = SettingsActionState.Success("Favorites restored successfully!")
                }

            } catch (e: Exception) {
                _uiState.value = SettingsActionState.Error("Restore failed: ${e.message}")
            }
        }
    }

    // Call this to reset the state after displaying a message
    fun resetState() {
        _uiState.value = SettingsActionState.Idle
    }
}

class SettingsViewModelFactory(
    private val repository: BoardRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
