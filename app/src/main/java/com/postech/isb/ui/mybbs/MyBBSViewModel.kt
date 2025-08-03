package com.postech.isb.ui.mybbs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postech.isb.util.IsbSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyBBSUiState(
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val hasNewMail: Boolean = false
)

class MyBBSViewModel(private val isbSession: IsbSession) : ViewModel() {

    private val _uiState = MutableStateFlow(MyBBSUiState())
    val uiState: StateFlow<MyBBSUiState> = _uiState.asStateFlow()

    // Call this method to refresh the state, e.g., when the fragment resumes.
    fun refreshState() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoggedIn = isbSession.isConnected,
                    userId = if (isbSession.isConnected) IsbSession.userId else "MyBBS",
                    hasNewMail = IsbSession.new_mail
                )
            }
        }
    }
}
