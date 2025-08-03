package com.postech.isb.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postech.isb.boardList.BoardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Enum to manage whether we are viewing the list or editing a note
enum class ComposeScreenMode {
    LIST,
    EDITING
}

data class ComposeUiState(
    val notes: List<Note> = emptyList(),
    val mode: ComposeScreenMode = ComposeScreenMode.LIST,
    val currentlyEditingNote: Note? = null, // The note being edited, if any
    val isLoading: Boolean = false,
    val error: String? = null
)

class ComposeViewModel(private val repository: BoardRepository) : ViewModel() {

    private val _mode = MutableStateFlow(ComposeScreenMode.LIST)
    private val _currentlyEditingNote = MutableStateFlow<Note?>(null)

    val uiState: StateFlow<ComposeUiState> = combine(
        repository.getAllNotes(),
        _mode,
        _currentlyEditingNote
    ) { notes, mode, editingNote ->
        ComposeUiState(
            notes = notes,
            mode = mode,
            currentlyEditingNote = editingNote
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ComposeUiState(isLoading = true)
    )

    fun enterEditMode(note: Note? = null) {
        _currentlyEditingNote.value = note // If note is null, it's a new note
        _mode.value = ComposeScreenMode.EDITING
    }

    fun exitEditMode() {
        _currentlyEditingNote.value = null
        _mode.value = ComposeScreenMode.LIST
    }

    fun saveNote(title: String, content: String) {
        viewModelScope.launch {
            val noteToSave = _currentlyEditingNote.value?.copy(
                title = title,
                content = content,
                lastModified = System.currentTimeMillis()
            ) ?: Note(
                title = title,
                content = content
            )
            repository.saveNote(noteToSave)
            exitEditMode() // Return to the list after saving
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }
}
